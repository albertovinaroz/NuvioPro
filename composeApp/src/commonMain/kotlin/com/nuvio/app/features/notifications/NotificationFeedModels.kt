package com.nuvio.app.features.notifications

import kotlinx.serialization.Serializable

data class NotificationFeedUiState(
    val items: List<NotificationFeedItem> = emptyList(),
    val unreadCount: Int = 0,
)

/**
 * One row in the in-app notification feed — a persisted, user-facing counterpart to whatever local
 * OS notification [EpisodeReleaseNotificationPlatform] schedules for the same episode, so releases
 * are still visible after the system notification itself has been dismissed or missed. Recorded
 * from [EpisodeReleaseNotificationsRepository] the moment a request is built, not when the OS
 * actually delivers it — see the comment there for why.
 */
@Serializable
data class NotificationFeedItem(
    val id: String,
    val contentType: String,
    val contentId: String,
    val title: String,
    val body: String,
    val releaseDateIso: String,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val backdropUrl: String? = null,
    val isRead: Boolean = false,
    /** Set only for items that don't point at real app content (e.g. an app-update alert) — tapping
     * opens this externally instead of navigating to [contentType]/[contentId] as a details route. */
    val linkUrl: String? = null,
)

@Serializable
internal data class StoredNotificationFeedPayload(
    val items: List<NotificationFeedItem> = emptyList(),
    /**
     * Ids the user has explicitly removed or cleared — [NotificationFeedRepository.recordItems]
     * excludes these from what it re-adds, since it rebuilds its input from scratch on every
     * refresh (whatever should currently be in the feed based on followed shows) with no memory of
     * its own for "the user already dismissed this one." Without this list, removing or clearing an
     * item is undone the next time that rebuild runs — often the very next app launch.
     */
    val dismissedIds: List<String> = emptyList(),
)

internal const val MaxNotificationFeedItems = 200
internal const val MaxDismissedNotificationFeedIds = 500
