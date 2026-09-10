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
)

@Serializable
internal data class StoredNotificationFeedPayload(
    val items: List<NotificationFeedItem> = emptyList(),
)

internal const val MaxNotificationFeedItems = 200
