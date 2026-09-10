package com.nuvio.app.features.updater

import com.nuvio.app.core.build.AppVersionConfig
import com.nuvio.app.features.notifications.EpisodeReleaseNotificationPlatform
import com.nuvio.app.features.notifications.EpisodeReleaseNotificationRequest
import com.nuvio.app.features.notifications.NotificationFeedItem
import com.nuvio.app.features.notifications.NotificationFeedRepository
import com.nuvio.app.features.watchprogress.CurrentDateProvider
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.app_update_feed_body
import nuvio.composeapp.generated.resources.app_update_feed_title
import org.jetbrains.compose.resources.getString

/**
 * Passive counterpart to [AppUpdaterController] for channels that can't download and install their
 * own update in-app (iOS sideloading via AltStore/SideStore has nothing like Android's package
 * installer) — instead of a banner offering a direct install, a newer release drops a card into the
 * notification feed and, where already permitted, fires a real OS notification too, linking out to
 * the GitHub release instead of offering a download this app has no way to actually perform.
 */
internal object AppUpdateFeedNotifier {
    private var checkStarted = false

    suspend fun ensureChecked() {
        if (checkStarted) return
        checkStarted = true

        AppUpdaterRepository.getLatestChannelRelease().onSuccess { release ->
            if (!VersionUtils.isRemoteNewer(release.tag, AppVersionConfig.VERSION_NAME)) return@onSuccess

            NotificationFeedRepository.ensureLoaded()
            val feedId = "app_update:${release.tag}"
            // Deliberately checks the live feed, not some separate "already notified" memory — if
            // the user removed or cleared this card, that's the signal to notify again next launch,
            // not to stay silent forever once a tag has been seen once.
            val alreadyPresent = NotificationFeedRepository.uiState.value.items.any { it.id == feedId }
            if (alreadyPresent) return@onSuccess

            val title = getString(Res.string.app_update_feed_title)
            val body = getString(Res.string.app_update_feed_body, release.tag)
            val releaseDate = release.publishedAt ?: CurrentDateProvider.todayIsoDate()

            NotificationFeedRepository.upsertUnread(
                NotificationFeedItem(
                    id = feedId,
                    contentType = "app_update",
                    contentId = release.tag,
                    title = title,
                    body = body,
                    releaseDateIso = releaseDate,
                    linkUrl = release.releaseUrl,
                ),
            )

            // A silent background check shouldn't be what prompts for notification permission out
            // of nowhere — only fire the real OS notification where it's already granted (e.g. via
            // the episode-release-alerts toggle); otherwise the feed card above is the only trace,
            // same as before this existed.
            val authorized = runCatching {
                EpisodeReleaseNotificationPlatform.notificationsAuthorized()
            }.getOrDefault(false)
            if (authorized) {
                runCatching {
                    EpisodeReleaseNotificationPlatform.showTestNotification(
                        EpisodeReleaseNotificationRequest(
                            requestId = feedId,
                            notificationTitle = title,
                            notificationBody = body,
                            releaseDateIso = releaseDate,
                            deepLinkUrl = "",
                        ),
                    )
                }
            }
        }
    }
}
