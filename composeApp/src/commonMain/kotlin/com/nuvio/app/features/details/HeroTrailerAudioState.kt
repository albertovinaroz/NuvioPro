package com.nuvio.app.features.details

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class HeroTrailerSurface {
    Home,
    Details,
}

/**
 * Whichever hero (Home's carousel or a Details screen's hero) currently has a trailer
 * ready-and-playing marks itself [visible] here, and clears it again the moment its own trailer
 * stops being shown. Home and a Details screen are never on-screen together, so [visible] always
 * reflects the one hero mute control that could plausibly be visible right now, never two
 * competing ones — see [HeroTrailerMuteController] for how the native (iOS) mute button consumes
 * this instead of Compose's own. Mute state itself is tracked per [HeroTrailerSurface] rather than
 * globally, so a "start with sound" preference set for one surface doesn't leak into the other.
 */
object HeroTrailerAudioState {
    private val states = HeroTrailerSurface.entries.associateWith { MutableStateFlow(true) }

    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    private val _visibleSurface = MutableStateFlow<HeroTrailerSurface?>(null)
    val visibleSurface: StateFlow<HeroTrailerSurface?> = _visibleSurface.asStateFlow()

    fun muted(surface: HeroTrailerSurface): StateFlow<Boolean> = state(surface).asStateFlow()

    fun toggleMuted(surface: HeroTrailerSurface) {
        val state = state(surface)
        state.value = !state.value
    }

    fun applyStartMuted(surface: HeroTrailerSurface, muted: Boolean) {
        state(surface).value = muted
    }

    fun setVisible(visible: Boolean, surface: HeroTrailerSurface) {
        _visible.value = visible
        _visibleSurface.value = if (visible) surface else null
    }

    /** For [HeroTrailerMuteController]: toggles whichever surface is currently visible — a no-op if none is. */
    internal fun toggleVisibleSurfaceMuted() {
        _visibleSurface.value?.let(::toggleMuted)
    }

    private fun state(surface: HeroTrailerSurface): MutableStateFlow<Boolean> =
        states.getValue(surface)
}

/**
 * Bridges [HeroTrailerAudioState] to a native SwiftUI mute button (see HeroTrailerMuteButton in
 * ContentView.swift) — mirrors [NativeProfileSwitcherController][com.nuvio.app.core.ui.NativeProfileSwitcherController]'s
 * observe/stop lifecycle so Swift can start collecting on `.onAppear` and cancel on
 * `.onDisappear` without leaking a coroutine per navigation. A fresh instance per SwiftUI view is
 * fine — every instance just observes the same underlying [HeroTrailerAudioState] singleton.
 */
class HeroTrailerMuteController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observationJob: Job? = null

    fun observeState(callback: (visible: Boolean, muted: Boolean) -> Unit) {
        observationJob?.cancel()
        observationJob = scope.launch {
            combine(
                HeroTrailerAudioState.visible,
                HeroTrailerAudioState.visibleSurface,
                HeroTrailerAudioState.muted(HeroTrailerSurface.Home),
                HeroTrailerAudioState.muted(HeroTrailerSurface.Details),
            ) { visible, surface, homeMuted, detailsMuted ->
                val muted = when (surface) {
                    HeroTrailerSurface.Home -> homeMuted
                    HeroTrailerSurface.Details -> detailsMuted
                    null -> true
                }
                visible to muted
            }.collect { (visible, muted) -> callback(visible, muted) }
        }
    }

    fun stopObserving() {
        observationJob?.cancel()
        observationJob = null
    }

    fun toggleMuted() {
        HeroTrailerAudioState.toggleVisibleSurfaceMuted()
    }
}
