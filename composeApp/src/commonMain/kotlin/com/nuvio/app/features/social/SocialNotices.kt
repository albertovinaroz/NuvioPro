package com.nuvio.app.features.social

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.notifications.NotificationFeedItem
import com.nuvio.app.features.notifications.NotificationFeedRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import kotlin.time.Clock
import kotlin.time.Instant

/** Feed items made from social notices all carry this id prefix. */
internal const val SocialNoticeIdPrefix = "social:"

/**
 * Social feed items that only inform (accepted requests, replies) open the Profile's Friends
 * section; they're what the Profile badge counts until seen there. Friend requests and
 * recommendations are counted by their own pending state instead.
 */
const val SocialNoticeContentType = "social"

/** Notices older than this are left out of the feed (e.g. on a first install). */
private const val SocialNoticeMaxAgeMs = 14L * 24 * 60 * 60 * 1000

/** Turns server notices into notification-feed items; already-recorded or dismissed ids are skipped by the feed. */
internal suspend fun recordSocialNotices(notices: List<SocialNotification>) {
    val now = Clock.System.now().toEpochMilliseconds()
    val items = notices
        .filter { now - it.createdAt < SocialNoticeMaxAgeMs }
        .mapNotNull { notice -> notice.toFeedItem() }
    if (items.isEmpty()) return
    NotificationFeedRepository.ensureLoaded()
    NotificationFeedRepository.recordItems(items)
}

private suspend fun SocialNotification.toFeedItem(): NotificationFeedItem? {
    val who = person.name.ifBlank { getString(Res.string.social_someone) }
    val date = Instant.fromEpochMilliseconds(createdAt)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
    val id = SocialNoticeIdPrefix + id
    return when (kind) {
        "friend_request" -> NotificationFeedItem(
            id = id,
            contentType = SocialNoticeContentType,
            contentId = kind,
            title = getString(Res.string.social_notice_request_title),
            body = getString(Res.string.social_notice_request_body, who),
            releaseDateIso = date,
            backdropUrl = person.avatar,
        )
        "friend_accepted" -> NotificationFeedItem(
            id = id,
            contentType = SocialNoticeContentType,
            contentId = kind,
            title = getString(Res.string.social_notice_accepted_title),
            body = getString(Res.string.social_notice_accepted_body, who),
            releaseDateIso = date,
            backdropUrl = person.avatar,
        )
        "recommendation" -> {
            val type = contentType ?: return null
            val contentId = contentId ?: return null
            val title = title ?: return null
            // Opens the recommended title itself.
            NotificationFeedItem(
                id = id,
                contentType = type,
                contentId = contentId,
                title = getString(Res.string.social_notice_recommendation_title, who, title),
                body = text?.takeIf { it.isNotBlank() }?.let { "“$it”" }
                    ?: getString(Res.string.social_notice_recommendation_body),
                releaseDateIso = date,
                backdropUrl = poster,
            )
        }
        "recommendation_reply" -> {
            val (reactionKey, reply) = (text ?: "").split('|', limit = 2).let { it.getOrElse(0) { "" } to it.getOrElse(1) { "" } }
            val response = listOfNotNull(SocialReaction.fromKey(reactionKey)?.emoji, reply.ifBlank { null }).joinToString("  ")
            NotificationFeedItem(
                id = id,
                contentType = SocialNoticeContentType,
                contentId = kind,
                title = getString(Res.string.social_notice_reply_title, who, title.orEmpty()),
                body = response,
                releaseDateIso = date,
                backdropUrl = person.avatar,
            )
        }
        else -> null
    }
}

private fun NotificationFeedItem.isUnreadSocialInfo(): Boolean =
    !isRead && id.startsWith(SocialNoticeIdPrefix) && contentType == SocialNoticeContentType && contentId != "friend_request"

/**
 * What the Profile badge shows: pending friend requests, unopened recommendations, and accepted
 * requests or replies not seen yet.
 */
@Composable
fun rememberSocialBadgeCount(): Int {
    val social by SocialRepository.state.collectAsStateWithLifecycle()
    val feed by remember {
        NotificationFeedRepository.ensureLoaded()
        NotificationFeedRepository.uiState
    }.collectAsStateWithLifecycle()
    if (!SocialRepository.isAvailable) return 0
    return social.incomingRequestCount +
        social.unseenRecommendationCount +
        feed.items.count { it.isUnreadSocialInfo() }
}

/** Seeing the Friends section counts as seeing its accepted-request and reply notices. */
internal fun markSocialInfoNoticesRead() {
    NotificationFeedRepository.uiState.value.items
        .filter { it.isUnreadSocialInfo() }
        .forEach { NotificationFeedRepository.markRead(it.id) }
}
