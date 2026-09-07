package com.nuvio.app.features.player

import com.nuvio.app.core.logging.InAppLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val AutoSubtitleLogTag = "AutoSubtitle"

// Implements the "auto-show subtitles" enhancement requested in
// https://github.com/albertovinaroz/NuvioPro/issues/4: when subtitles are off, rewinding or
// muting/zeroing the volume temporarily turns them on so a missed line can be caught, then
// restores the original (off) state once it's no longer needed.
//
// The rewind side deliberately tracks a *watermark* — the playback position the user was at right
// before rewinding — rather than a timer. Auto-shown subtitles stay up until normal forward
// playback reaches that watermark again. This gets "rewinding further, or rewinding again before
// catching up, keeps them up longer" for free: a later rewind only ever raises the watermark
// (never lowers it), so the required catch-up distance always reflects the deepest rewind of the
// current streak.
//
// Mute-triggered auto-show is a simple level, not a watermark: active for as long as the player
// reports muted/zero volume, cleared the instant it doesn't.
//
// Both triggers share one "is anything auto-showing right now" flag, so unmuting mid-rewind (or
// vice versa) doesn't hide subtitles out from under the other still-active trigger.

private fun PlayerScreenRuntime.hasRealSubtitleSelection(): Boolean =
    (selectedSubtitleIndex != -1 || selectedAddonSubtitleId != null) && !isAutoSubtitleShowing

/** Picks which track to show when auto-enabling from scratch: the user's preferred subtitle
 * language among tracks already loaded for this item, falling back to whatever's first available
 * so auto-show always has *something* to display. Deliberately doesn't reach for addon subtitles —
 * those require a network fetch, too slow for an instant reaction to a rewind or a mute tap. */
private fun PlayerScreenRuntime.pickAutoShowSubtitleTrackIndex(): Int? {
    if (subtitleTracks.isEmpty()) return null

    val preferredLanguage = normalizeLanguageCode(playerSettingsUiState.preferredSubtitleLanguage)
    val targets = if (
        preferredLanguage == SubtitleLanguageOption.NONE || preferredLanguage == SubtitleLanguageOption.FORCED
    ) {
        emptyList()
    } else {
        resolvePreferredSubtitleLanguageTargets(
            preferredSubtitleLanguage = playerSettingsUiState.preferredSubtitleLanguage,
            secondaryPreferredSubtitleLanguage = playerSettingsUiState.secondaryPreferredSubtitleLanguage,
            deviceLanguages = DeviceLanguagePreferences.preferredLanguageCodes(),
        )
    }
    targets.forEach { target ->
        subtitleTracks.firstOrNull { SubtitleLanguageMatching.matchesLanguageCode(it.language, target) }
            ?.let { return it.index }
    }
    return (subtitleTracks.firstOrNull { it.isSelected } ?: subtitleTracks.first()).index
}

private fun PlayerScreenRuntime.activateAutoSubtitleIfNeeded() {
    if (isAutoSubtitleShowing) {
        InAppLogger.debug(AutoSubtitleLogTag, "activate: skipped, already showing")
        return
    }
    if (hasRealSubtitleSelection()) {
        InAppLogger.debug(AutoSubtitleLogTag, "activate: skipped, real selection already active")
        return
    }
    val index = pickAutoShowSubtitleTrackIndex()
    if (index == null) {
        InAppLogger.debug(AutoSubtitleLogTag, "activate: no track to show (tracks=${subtitleTracks.size})")
        return
    }
    playerController?.selectSubtitleTrack(index)
    selectedSubtitleIndex = index
    isAutoSubtitleShowing = true
    InAppLogger.debug(AutoSubtitleLogTag, "activate: turned on index=$index")
}

private fun PlayerScreenRuntime.deactivateAutoSubtitleIfNeeded() {
    if (!isAutoSubtitleShowing) return
    if (autoSubtitleRewindWatermarkMs != null || isAutoSubtitleMuteActive) {
        InAppLogger.debug(
            AutoSubtitleLogTag,
            "deactivate: skipped, still needed (watermark=$autoSubtitleRewindWatermarkMs, mute=$isAutoSubtitleMuteActive)",
        )
        return
    }
    playerController?.selectSubtitleTrack(-1)
    selectedSubtitleIndex = -1
    isAutoSubtitleShowing = false
    InAppLogger.debug(AutoSubtitleLogTag, "deactivate: turned off")
}

/** Call right after issuing a seek, whenever [fromPositionMs] (the position just before it) is
 * greater than the seek's target — i.e. it was a rewind. */
internal fun PlayerScreenRuntime.notifyRewindOccurred(fromPositionMs: Long) {
    InAppLogger.debug(AutoSubtitleLogTag, "notifyRewindOccurred: fromPositionMs=$fromPositionMs")
    if (!playerSettingsUiState.autoShowSubtitlesOnRewindEnabled) {
        InAppLogger.debug(AutoSubtitleLogTag, "notifyRewindOccurred: skipped, setting disabled")
        return
    }
    if (hasRealSubtitleSelection()) {
        InAppLogger.debug(AutoSubtitleLogTag, "notifyRewindOccurred: skipped, real selection active")
        return
    }
    val safeFrom = fromPositionMs.coerceAtLeast(0L)
    autoSubtitleRewindWatermarkMs = maxOf(autoSubtitleRewindWatermarkMs ?: safeFrom, safeFrom)
    InAppLogger.debug(AutoSubtitleLogTag, "notifyRewindOccurred: watermark now $autoSubtitleRewindWatermarkMs")
    activateAutoSubtitleIfNeeded()
}

/** Call on every playback position update (the same tick that updates [PlayerScreenRuntime.playbackSnapshot]). */
internal fun PlayerScreenRuntime.checkAutoSubtitleRewindWatermark(currentPositionMs: Long) {
    val watermark = autoSubtitleRewindWatermarkMs ?: return
    if (currentPositionMs >= watermark) {
        InAppLogger.debug(
            AutoSubtitleLogTag,
            "checkAutoSubtitleRewindWatermark: reached (current=$currentPositionMs, watermark=$watermark)",
        )
        autoSubtitleRewindWatermarkMs = null
        deactivateAutoSubtitleIfNeeded()
    }
}

/** Call with the current combined volume/mute level whenever it changes.
 *
 * A volume drag calls this synchronously on every pointer-move frame while the gesture is still
 * active. Switching the subtitle track (and the state mutation/recomposition that causes) inline
 * from there visibly stalls the still-active drag — so the actual switch is pushed one frame out
 * via [PlayerScreenRuntime.scope] instead of applied immediately. A real `delay`, not just
 * `launch`, is required: [PlayerScreenRuntime.scope] runs on an immediate main dispatcher, so a
 * suspension-free `launch` body would still execute inline on the calling frame. */
internal fun PlayerScreenRuntime.notifyVolumeLevelForAutoSubtitle(level: PlayerAudioLevel) {
    if (!playerSettingsUiState.autoShowSubtitlesOnMuteEnabled) {
        wasAutoSubtitleVolumeMuted = level.isMuted
        return
    }
    if (level.isMuted == wasAutoSubtitleVolumeMuted) return
    wasAutoSubtitleVolumeMuted = level.isMuted
    autoSubtitleMuteActivationJob?.cancel()
    if (level.isMuted && hasRealSubtitleSelection()) return
    isAutoSubtitleMuteActive = level.isMuted
    autoSubtitleMuteActivationJob = scope.launch {
        delay(16)
        if (isAutoSubtitleMuteActive) activateAutoSubtitleIfNeeded() else deactivateAutoSubtitleIfNeeded()
    }
}

/** Call whenever the user picks a subtitle (or "off") themselves, so their explicit choice sticks
 * instead of being reverted the next time a watermark or mute-state check runs. */
internal fun PlayerScreenRuntime.clearAutoSubtitleState() {
    autoSubtitleMuteActivationJob?.cancel()
    autoSubtitleMuteActivationJob = null
    isAutoSubtitleShowing = false
    autoSubtitleRewindWatermarkMs = null
    isAutoSubtitleMuteActive = false
}
