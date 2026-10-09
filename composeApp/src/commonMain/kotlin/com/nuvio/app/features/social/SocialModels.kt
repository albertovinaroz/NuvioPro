package com.nuvio.app.features.social

import kotlinx.serialization.Serializable

/** Wire models for the Nuvio Pro social server (servers/social). */

@Serializable
data class SocialMe(
    val id: String,
    val name: String = "",
    val avatar: String? = null,
    val friendCode: String,
    /** 0: activity not shared, 1: friends see it. */
    val sharing: Int = 1,
    val incomingRequests: Int = 0,
    val unseenRecommendations: Int = 0,
)

@Serializable
data class SocialPerson(
    val id: String,
    val name: String = "",
    val avatar: String? = null,
)

@Serializable
data class SocialActivity(
    val id: Long,
    val kind: String,
    val contentType: String,
    val contentId: String,
    val title: String,
    val poster: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val episodeTitle: String? = null,
    val progress: Double? = null,
    val startedAt: Long,
    val updatedAt: Long,
) {
    val isFinished: Boolean get() = kind == "finished"
}

@Serializable
data class SocialFriend(
    val id: String,
    val name: String = "",
    val avatar: String? = null,
    val since: Long = 0,
    val watchingNow: SocialActivity? = null,
) {
    val person: SocialPerson get() = SocialPerson(id, name, avatar)
}

@Serializable
data class SocialFriendRequest(
    val id: String,
    val name: String = "",
    val avatar: String? = null,
    val since: Long = 0,
) {
    val person: SocialPerson get() = SocialPerson(id, name, avatar)
}

@Serializable
data class SocialFriends(
    val friends: List<SocialFriend> = emptyList(),
    val incoming: List<SocialFriendRequest> = emptyList(),
    val outgoing: List<SocialFriendRequest> = emptyList(),
)

@Serializable
data class SocialAddFriendResult(
    /** "friends" or "requested". */
    val status: String,
    val person: SocialPerson? = null,
)

@Serializable
data class SocialRecommendation(
    val id: String,
    /** The sender on a received recommendation, the recipient on a sent one. */
    val person: SocialPerson,
    val contentType: String,
    val contentId: String,
    val title: String,
    val poster: String? = null,
    val note: String? = null,
    val reaction: String? = null,
    val reply: String? = null,
    val createdAt: Long,
    val repliedAt: Long? = null,
    val seen: Boolean = false,
)

@Serializable
data class SocialRecommendations(
    val received: List<SocialRecommendation> = emptyList(),
    val sent: List<SocialRecommendation> = emptyList(),
)

@Serializable
data class SocialFeedItem(
    val id: Long,
    val kind: String,
    val contentType: String,
    val contentId: String,
    val title: String,
    val poster: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val episodeTitle: String? = null,
    val progress: Double? = null,
    val startedAt: Long,
    val updatedAt: Long,
    val person: SocialPerson,
    val live: Boolean = false,
)

@Serializable
data class SocialFeed(val items: List<SocialFeedItem> = emptyList())

/** A notice for the in-app notification feed. */
@Serializable
data class SocialNotification(
    val id: Long,
    /** friend_request, friend_accepted, recommendation or recommendation_reply. */
    val kind: String,
    val person: SocialPerson,
    val contentType: String? = null,
    val contentId: String? = null,
    val title: String? = null,
    val poster: String? = null,
    /** The note on a recommendation; "reaction|reply" on a reply. */
    val text: String? = null,
    val createdAt: Long,
)

@Serializable
data class SocialNotifications(
    val items: List<SocialNotification> = emptyList(),
    val incomingRequests: Int = 0,
    val unseenRecommendations: Int = 0,
)

@Serializable
internal data class SocialError(val error: String = "")

/** The reactions the server accepts, in display order, with the emoji each one shows as. */
enum class SocialReaction(val key: String, val emoji: String) {
    Love("love", "❤️"),
    Laugh("laugh", "😂"),
    Fire("fire", "🔥"),
    Like("like", "👍"),
    Dislike("dislike", "👎"),
    Watched("watched", "✅");

    companion object {
        fun fromKey(key: String?): SocialReaction? = entries.firstOrNull { it.key == key }
    }
}
