package com.nuvio.app.features.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persisted, user-facing history of episode-release alerts — a companion to the fire-and-forget OS
 * notifications [EpisodeReleaseNotificationPlatform] schedules, which leave no in-app trace once
 * dismissed or missed. [EpisodeReleaseNotificationsRepository] feeds this via [recordItems] every
 * time it rebuilds the set of upcoming/aired releases for followed shows.
 */
object NotificationFeedRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _uiState = MutableStateFlow(NotificationFeedUiState())
    val uiState: StateFlow<NotificationFeedUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var items: List<NotificationFeedItem> = emptyList()

    // Most-recently-dismissed first. recordItems rebuilds its input from scratch every refresh
    // (whatever should currently be in the feed for followed shows), so without this it has no way
    // to know "the user already removed this one" and just re-adds it the next time that runs —
    // often the very next app launch. upsertUnread deliberately ignores this list: it's used for
    // explicit re-triggers (the test-notification button, the app-update check) that should always
    // show regardless of a past dismissal.
    private var dismissedIds: List<String> = emptyList()

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    /**
     * Merges newly-built items in, newest release first, keeping each existing item's read state
     * and never re-surfacing one already recorded (a show's requests are rebuilt in full on every
     * refresh, so most calls are almost entirely re-seen ids) or one the user has since dismissed.
     */
    fun recordItems(newItems: List<NotificationFeedItem>) {
        if (newItems.isEmpty()) return
        ensureLoaded()

        val existingById = items.associateBy(NotificationFeedItem::id)
        val merged = (
            newItems
                .filterNot { incoming -> incoming.id in dismissedIds }
                .map { incoming -> existingById[incoming.id] ?: incoming } + items
            )
            .distinctBy(NotificationFeedItem::id)
            .sortedByDescending(NotificationFeedItem::releaseDateIso)
            .take(MaxNotificationFeedItems)

        if (merged == items) return
        items = merged
        publish()
        persist()
    }

    /**
     * Always overwrites (unlike [recordItems], which preserves an existing item's read state) and
     * resets it to unread — used for the Settings test-notification button, where re-testing should
     * visibly re-trigger the unread badge rather than silently no-op against an id it already knows.
     */
    fun upsertUnread(item: NotificationFeedItem) {
        ensureLoaded()
        items = (listOf(item.copy(isRead = false)) + items.filterNot { it.id == item.id })
            .sortedByDescending(NotificationFeedItem::releaseDateIso)
            .take(MaxNotificationFeedItems)
        publish()
        persist()
    }

    fun markRead(id: String) {
        ensureLoaded()
        val updated = items.map { item -> if (item.id == id && !item.isRead) item.copy(isRead = true) else item }
        if (updated == items) return
        items = updated
        publish()
        persist()
    }

    fun markAllRead() {
        ensureLoaded()
        val updated = items.map { item -> if (item.isRead) item else item.copy(isRead = true) }
        if (updated == items) return
        items = updated
        publish()
        persist()
    }

    fun remove(id: String) {
        ensureLoaded()
        val updated = items.filterNot { it.id == id }
        if (updated.size == items.size) return
        items = updated
        rememberDismissed(listOf(id))
        publish()
        persist()
    }

    fun clearAll() {
        ensureLoaded()
        if (items.isEmpty()) return
        rememberDismissed(items.map(NotificationFeedItem::id))
        items = emptyList()
        publish()
        persist()
    }

    private fun rememberDismissed(ids: List<String>) {
        dismissedIds = (ids + dismissedIds)
            .distinct()
            .take(MaxDismissedNotificationFeedIds)
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val payload = NotificationFeedStorage.loadPayload().orEmpty().trim()
        val stored = if (payload.isEmpty()) {
            StoredNotificationFeedPayload()
        } else {
            runCatching {
                json.decodeFromString<StoredNotificationFeedPayload>(payload)
            }.getOrDefault(StoredNotificationFeedPayload())
        }
        items = stored.items.take(MaxNotificationFeedItems)
        dismissedIds = stored.dismissedIds.take(MaxDismissedNotificationFeedIds)
        publish()
    }

    private fun publish() {
        _uiState.value = NotificationFeedUiState(
            items = items,
            unreadCount = items.count { !it.isRead },
        )
    }

    private fun persist() {
        NotificationFeedStorage.savePayload(
            json.encodeToString(StoredNotificationFeedPayload(items = items, dismissedIds = dismissedIds)),
        )
    }
}
