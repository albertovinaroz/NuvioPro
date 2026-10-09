package com.nuvio.app.features.social

import co.touchlab.kermit.Logger
import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.AuthState
import com.nuvio.app.core.network.ServerConfigurationRepository
import com.nuvio.app.features.player.PlayerPlaybackSnapshot
import com.nuvio.app.features.profiles.AvatarRepository
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.profiles.avatarImageUrl
import com.nuvio.app.features.profiles.normalizedAvatarUrl
import com.nuvio.app.features.watchprogress.WatchProgressPlaybackSession
import com.nuvio.app.features.watchprogress.isWatchProgressComplete
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

data class SocialState(
    val profileId: Int = 0,
    val me: SocialMe? = null,
    val friends: SocialFriends = SocialFriends(),
    val recommendations: SocialRecommendations = SocialRecommendations(),
    val feed: List<SocialFeedItem> = emptyList(),
    /** Pending friend requests to this profile, kept current by the background poll too. */
    val incomingRequestCount: Int = 0,
    /** Received recommendations not opened yet, kept current by the background poll too. */
    val unseenRecommendationCount: Int = 0,
    val isLoading: Boolean = false,
    /** The last refresh's error code, e.g. "auth_unavailable"; null once a refresh succeeds. */
    val error: String? = null,
) {
    val unseenRecommendations: Int get() = unseenRecommendationCount
}

/**
 * Friends, recommendations and friends' activity for the active profile, backed by the Nuvio Pro
 * social server. Needs a real Nuvio account (not a local-only one) on Nuvio's own backend: that
 * account's token is what the server checks.
 */
object SocialRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val log = Logger.withTag("Social")
    private val api = SocialApi()
    private val refreshMutex = Mutex()

    private val _state = MutableStateFlow(SocialState())
    val state: StateFlow<SocialState> = _state.asStateFlow()

    /** Last "watching" heartbeat per video, and videos already reported finished. */
    private val lastHeartbeatAt = mutableMapOf<String, Long>()
    private val reportedFinished = mutableSetOf<String>()

    /** Whether this build has a social server at all (`NUVIO_SOCIAL_URL` set when it was built). */
    val isConfigured: Boolean get() = SocialServerUrl.startsWith("https://")

    val isAvailable: Boolean
        get() {
            val auth = AuthRepository.state.value
            return isConfigured &&
                auth is AuthState.Authenticated &&
                !auth.isAnonymous &&
                !ServerConfigurationRepository.active.value.isCustom
        }

    private val activeProfile: Int get() = ProfileRepository.activeProfileId

    fun refresh() {
        if (!isAvailable) return
        scope.launch { refreshNow() }
    }

    private suspend fun refreshNow() = refreshMutex.withLock {
        val profile = activeProfile
        if (_state.value.profileId != profile) _state.value = SocialState(profileId = profile)
        _state.update { it.copy(isLoading = true) }
        try {
            val meDeferred = scope.async { syncIdentity(profile, api.me(profile)) }
            val friendsDeferred = scope.async { api.friends(profile) }
            val recommendationsDeferred = scope.async { api.recommendations(profile) }
            val feedDeferred = scope.async { api.feed(profile) }
            val friends = friendsDeferred.await()
            val recommendations = recommendationsDeferred.await()
            val next = SocialState(
                profileId = profile,
                me = meDeferred.await(),
                friends = friends,
                recommendations = recommendations,
                feed = feedDeferred.await().items,
                incomingRequestCount = friends.incoming.size,
                unseenRecommendationCount = recommendations.received.count { !it.seen },
            )
            if (activeProfile == profile) _state.value = next
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            log.w(error) { "Social refresh failed" }
            _state.update { it.copy(isLoading = false, error = error.socialCode()) }
        }
    }

    /** Keeps the server's name and avatar in step with the Nuvio profile's. */
    private suspend fun syncIdentity(profile: Int, me: SocialMe): SocialMe {
        val nuvioProfile = ProfileRepository.state.value.activeProfile ?: return me
        val name = nuvioProfile.name.trim()
        val avatar = normalizedAvatarUrl(nuvioProfile.avatarUrl)
            ?: nuvioProfile.avatarId
                ?.let { id -> AvatarRepository.avatars.value.firstOrNull { it.id == id } }
                ?.let(::avatarImageUrl)
            ?: ""
        val avatarToSend = avatar.takeIf { it.startsWith("https://") }.orEmpty()
        if (name == me.name && avatarToSend == me.avatar.orEmpty()) return me
        return runCatching { api.updateMe(profile, name.ifBlank { null }, avatarToSend, null) }.getOrDefault(me)
    }

    suspend fun addFriend(code: String): Result<SocialAddFriendResult> = call {
        api.addFriend(activeProfile, code).also { refreshFriends() }
    }

    fun acceptFriend(personId: String) = launchThenRefresh { api.acceptFriend(activeProfile, personId) }

    fun declineFriend(personId: String) = launchThenRefresh { api.declineFriend(activeProfile, personId) }

    fun cancelRequest(personId: String) = launchThenRefresh { api.cancelRequest(activeProfile, personId) }

    fun removeFriend(personId: String) = launchThenRefresh { api.removeFriend(activeProfile, personId) }

    suspend fun recommend(
        friendIds: List<String>,
        contentType: String,
        contentId: String,
        title: String,
        poster: String?,
        note: String?,
    ): Result<Unit> = call {
        api.recommend(activeProfile, friendIds, contentType, contentId, title, poster, note?.trim()?.ifBlank { null })
        refreshRecommendations()
    }

    suspend fun respond(id: String, reaction: SocialReaction?, reply: String?): Result<Unit> = call {
        api.respond(activeProfile, id, reaction?.key, reply?.trim()?.ifBlank { null })
        refreshRecommendations()
    }

    /** Marks received recommendations as seen, locally right away and then on the server. */
    fun markSeen(ids: Collection<String>) {
        if (ids.isEmpty()) return
        _state.update { state ->
            val received = state.recommendations.received.map { if (it.id in ids) it.copy(seen = true) else it }
            state.copy(
                recommendations = state.recommendations.copy(received = received),
                unseenRecommendationCount = received.count { !it.seen },
            )
        }
        val profile = activeProfile
        scope.launch { ids.forEach { id -> runCatching { api.markSeen(profile, id) } } }
    }

    fun setSharing(enabled: Boolean) {
        val profile = activeProfile
        _state.update { state -> state.copy(me = state.me?.copy(sharing = if (enabled) 1 else 0)) }
        scope.launch {
            runCatching { api.updateMe(profile, null, null, if (enabled) 1 else 0) }
                .onSuccess { me -> _state.update { it.copy(me = me) } }
                .onFailure { error -> log.w(error) { "Updating sharing failed" } }
        }
    }

    /**
     * Called on every playback progress save. Sends a "watching" heartbeat at most every few
     * minutes per video, and one "finished" once the video completes. Silently does nothing
     * while sharing is off or social isn't available.
     */
    fun reportPlayback(session: WatchProgressPlaybackSession, snapshot: PlayerPlaybackSnapshot) {
        if (!isAvailable || _state.value.me?.sharing == 0) return
        if (snapshot.durationMs <= 0L) return
        val finished = isWatchProgressComplete(snapshot.positionMs, snapshot.durationMs, snapshot.isEnded)
        val now = Clock.System.now().toEpochMilliseconds()
        val key = session.videoId
        if (finished) {
            if (!reportedFinished.add(key)) return
        } else {
            if (!snapshot.isPlaying) return
            val last = lastHeartbeatAt[key]
            if (last != null && now - last < HeartbeatIntervalMs) return
            lastHeartbeatAt[key] = now
            reportedFinished.remove(key)
        }
        val profile = session.profileId
        val body = buildJsonObject {
            put("kind", if (finished) "finished" else "watching")
            put("contentType", session.parentMetaType)
            put("contentId", session.parentMetaId)
            put("title", session.title)
            session.poster?.let { put("poster", it) }
            session.seasonNumber?.let { put("season", it) }
            session.episodeNumber?.let { put("episode", it) }
            session.episodeTitle?.let { put("episodeTitle", it) }
            put("progress", (snapshot.positionMs.toDouble() / snapshot.durationMs).coerceIn(0.0, 1.0))
        }
        scope.launch {
            runCatching { api.recordActivity(profile, body) }
                .onFailure { error -> log.w(error) { "Reporting activity failed" } }
        }
    }

    private var pollingStarted = false

    /** A new profile has its own friends and notices: fetch them right away rather than at the next poll. */
    fun onProfileChanged() {
        _state.value = SocialState(profileId = activeProfile)
        scope.launch { pollNotifications() }
    }

    /**
     * Starts the app-wide check for new notices (friend requests, accepted requests,
     * recommendations, replies). Safe to call repeatedly: only the first call starts it.
     */
    fun startNotificationPolling() {
        if (pollingStarted) return
        pollingStarted = true
        scope.launch {
            while (true) {
                pollNotifications()
                delay(NotificationPollIntervalMs)
            }
        }
    }

    /** Fetches notices for the active profile into the notification feed and refreshes the badge counts. */
    suspend fun pollNotifications() {
        if (!isAvailable) return
        val profile = activeProfile
        val notices = try {
            api.notifications(profile)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            log.w(error) { "Polling notices failed" }
            return
        }
        // The feed is stored per profile: drop the result if the profile changed meanwhile.
        if (activeProfile != profile) return
        _state.update { state ->
            val base = if (state.profileId == profile) state else SocialState(profileId = profile)
            base.copy(
                incomingRequestCount = notices.incomingRequests,
                unseenRecommendationCount = notices.unseenRecommendations,
            )
        }
        recordSocialNotices(notices.items)
    }

    private suspend fun refreshFriends() {
        val profile = activeProfile
        val friends = api.friends(profile)
        _state.update {
            if (it.profileId == profile) it.copy(friends = friends, incomingRequestCount = friends.incoming.size) else it
        }
    }

    private suspend fun refreshRecommendations() {
        val profile = activeProfile
        val recommendations = api.recommendations(profile)
        _state.update {
            if (it.profileId == profile) {
                it.copy(
                    recommendations = recommendations,
                    unseenRecommendationCount = recommendations.received.count { rec -> !rec.seen },
                )
            } else {
                it
            }
        }
    }

    private fun launchThenRefresh(block: suspend () -> Unit) {
        scope.launch {
            runCatching { block() }.onFailure { error -> log.w(error) { "Social action failed" } }
            runCatching { refreshFriends() }
        }
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            log.w(error) { "Social call failed" }
            Result.failure(error)
        }

    private const val HeartbeatIntervalMs = 3 * 60 * 1000L
    private const val NotificationPollIntervalMs = 2 * 60 * 1000L
}

internal fun Throwable.socialCode(): String = (this as? SocialException)?.code ?: "network"
