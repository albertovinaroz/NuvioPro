package com.nuvio.app.features.ratings

import co.touchlab.kermit.Logger
import com.nuvio.app.core.tracking.ensureTrackingProvidersRegistered
import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.tracking.TRACKING_RATING_MAX
import com.nuvio.app.features.tracking.TRACKING_RATING_MIN
import com.nuvio.app.features.tracking.TrackingExternalIds
import com.nuvio.app.features.tracking.TrackingProviderId
import com.nuvio.app.features.tracking.TrackingProviderRegistry
import com.nuvio.app.features.tracking.TrackingRatingProvider
import com.nuvio.app.features.tracking.TrackingRatingRecord
import com.nuvio.app.features.tracking.TrackingRatingScope
import com.nuvio.app.features.tracking.TrackingRatingTarget
import com.nuvio.app.features.tracking.sharesIdentityWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.user_rating_no_provider
import nuvio.composeapp.generated.resources.user_rating_removed
import nuvio.composeapp.generated.resources.user_rating_saved
import nuvio.composeapp.generated.resources.user_rating_sync_failed
import org.jetbrains.compose.resources.getString
import kotlin.time.Clock

/** The personal rating of one item across every connected provider that was asked. */
data class UserRatingEntry(
    /** A provider is present once its answer is known; a null value means "not rated". */
    val byProvider: Map<TrackingProviderId, Int?> = emptyMap(),
    val loadingProviders: Set<TrackingProviderId> = emptySet(),
    val savingProviders: Set<TrackingProviderId> = emptySet(),
    val failedProviders: Set<TrackingProviderId> = emptySet(),
    /** Bumped on every local write so reads started earlier cannot overwrite it. */
    val revision: Long = 0L,
) {
    val isLoading: Boolean get() = loadingProviders.isNotEmpty()
    val isSaving: Boolean get() = savingProviders.isNotEmpty()

    fun ratingFor(preferred: List<TrackingProviderId>): Int? =
        preferred.firstNotNullOfOrNull { id -> byProvider[id] }
            ?: byProvider.entries.sortedBy { it.key.ordinal }.firstNotNullOfOrNull { it.value }
}

data class UserRatingsUiState(
    val entries: Map<String, UserRatingEntry> = emptyMap(),
    val disabledProviders: Set<TrackingProviderId> = emptySet(),
    val profileId: Int = -1,
)

/** One rated movie or show, merged across every provider that has it rated. */
data class AggregatedUserRating(
    val scope: TrackingRatingScope,
    val ids: TrackingExternalIds,
    val title: String?,
    val year: Int?,
    val ratingsByProvider: Map<TrackingProviderId, Int>,
)

/**
 * Reads and writes personal 1–10 ratings through every connected tracking service that
 * implements [TrackingRatingProvider]. Ratings are fetched per provider and scope as a full
 * list (one request family per provider/scope) and cached briefly, so opening many episodes
 * does not issue one request per item and edits made on other devices show up after [CACHE_TTL_MS].
 */
object UserRatingsRepository {
    private val log = Logger.withTag("UserRatings")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(UserRatingsUiState())
    val uiState: StateFlow<UserRatingsUiState> = _uiState.asStateFlow()

    private val cacheMutex = Mutex()
    private val fetchLocks = mutableMapOf<String, Mutex>()
    private val cache = mutableMapOf<String, CachedRatings>()
    private val normalizedTargets = mutableMapOf<String, TrackingRatingTarget>()
    private var writeSequence = 0L

    private const val CACHE_TTL_MS = 5 * 60_000L
    const val SHEET_REFRESH_AGE_MS = 30_000L

    private data class CachedRatings(
        val fetchedAtMs: Long,
        val records: List<TrackingRatingRecord>,
    )

    // ── providers ───────────────────────────────────────────────────────

    /** Connected providers able to rate [target], in provider order. */
    fun providersFor(target: TrackingRatingTarget): List<TrackingProviderId> {
        ensureRegistered()
        syncProfile()
        if (!target.isValid) return emptyList()
        return TrackingProviderRegistry.connectedRatingProviders()
            .filter { provider -> target.scope in provider.supportedScopes && provider.canRate(target) }
            .map(TrackingRatingProvider::providerId)
    }

    /** Connected providers supporting [scope] at all (used to decide whether to show an entry). */
    fun providersFor(scope: TrackingRatingScope): List<TrackingProviderId> {
        ensureRegistered()
        return TrackingProviderRegistry.connectedRatingProviders()
            .filter { provider -> scope in provider.supportedScopes }
            .map(TrackingRatingProvider::providerId)
    }

    fun enabledProvidersFor(target: TrackingRatingTarget): List<TrackingProviderId> {
        syncProfile()
        val disabled = _uiState.value.disabledProviders
        return providersFor(target).filterNot { it in disabled }
    }

    fun setProviderEnabled(providerId: TrackingProviderId, enabled: Boolean) {
        syncProfile()
        val profileId = _uiState.value.profileId
        val next = _uiState.value.disabledProviders.let { if (enabled) it - providerId else it + providerId }
        _uiState.update { it.copy(disabledProviders = next) }
        UserRatingsStorage.saveDisabledProviders(profileId, next.joinToString(",") { it.storageId })
    }

    fun entryKey(target: TrackingRatingTarget): String = "${ProfileRepository.activeProfileId}|${target.key}"

    // ── reads ───────────────────────────────────────────────────────────

    fun load(target: TrackingRatingTarget, maxAgeMs: Long = CACHE_TTL_MS) {
        val providers = providersFor(target)
        if (providers.isEmpty()) return
        syncProfile()
        val profileId = _uiState.value.profileId
        val key = entryKey(target)
        val startRevision = _uiState.value.entries[key]?.revision ?: 0L
        updateEntry(key) { entry -> entry.copy(loadingProviders = entry.loadingProviders + providers) }
        providers.forEach { providerId ->
            scope.launch {
                val provider = TrackingProviderRegistry.ratingProvider(providerId) ?: return@launch
                val rating = runCatching {
                    val normalized = normalized(provider, target)
                    ratingsFor(provider, profileId, normalized.scope, maxAgeMs)
                        .firstOrNull { record -> record.matches(normalized) }
                        ?.rating
                }
                rating.exceptionOrNull()?.let { error ->
                    if (error is CancellationException) throw error
                    log.w { "Failed to read ${providerId.storageId} ratings: ${error.message}" }
                }
                updateEntry(key) { entry ->
                    entry.copy(
                        byProvider = if (rating.isSuccess && entry.revision == startRevision) {
                            entry.byProvider + (providerId to rating.getOrNull())
                        } else {
                            entry.byProvider
                        },
                        loadingProviders = entry.loadingProviders - providerId,
                    )
                }
            }
        }
    }

    /**
     * Every movie and show rated on any connected provider, one entry per title even when it is
     * rated on several. Used by the Library "Rated" view to show synced ratings alongside local
     * ones; not cached beyond [ratingsFor]'s own TTL, so callers should hold the result themselves.
     */
    suspend fun loadAllTitleRatings(): List<AggregatedUserRating> {
        ensureRegistered()
        syncProfile()
        val profileId = _uiState.value.profileId
        val titleScopes = setOf(TrackingRatingScope.MOVIE, TrackingRatingScope.SHOW)
        val providers = TrackingProviderRegistry.connectedRatingProviders()
            .filter { provider -> provider.supportedScopes.any { it in titleScopes } }
        if (providers.isEmpty()) return emptyList()

        val perProviderRecords = coroutineScope {
            providers.flatMap { provider ->
                titleScopes.filter { it in provider.supportedScopes }.map { ratingScope ->
                    async {
                        provider.providerId to runCatching { ratingsFor(provider, profileId, ratingScope, CACHE_TTL_MS) }
                            .getOrElse { error ->
                                if (error is CancellationException) throw error
                                log.w { "Failed to read ${provider.providerId.storageId} ratings: ${error.message}" }
                                emptyList()
                            }
                    }
                }
            }.awaitAll()
        }

        val aggregated = mutableListOf<AggregatedUserRating>()
        for ((providerId, records) in perProviderRecords) {
            for (record in records) {
                val index = aggregated.indexOfFirst { it.scope == record.scope && it.ids.sharesIdentityWith(record.ids) }
                if (index >= 0) {
                    val existing = aggregated[index]
                    aggregated[index] = existing.copy(
                        ids = existing.ids.mergeMissing(record.ids),
                        title = existing.title ?: record.title,
                        year = existing.year ?: record.year,
                        ratingsByProvider = existing.ratingsByProvider + (providerId to record.rating),
                    )
                } else {
                    aggregated += AggregatedUserRating(
                        scope = record.scope,
                        ids = record.ids,
                        title = record.title,
                        year = record.year,
                        ratingsByProvider = mapOf(providerId to record.rating),
                    )
                }
            }
        }
        return aggregated
    }

    private suspend fun ratingsFor(
        provider: TrackingRatingProvider,
        profileId: Int,
        ratingScope: TrackingRatingScope,
        maxAgeMs: Long,
    ): List<TrackingRatingRecord> {
        val cacheKey = cacheKey(profileId, provider.providerId, ratingScope)
        val lock = cacheMutex.withLock { fetchLocks.getOrPut(cacheKey) { Mutex() } }
        return lock.withLock {
            val now = nowMs()
            val (cached, sequenceBefore) = cacheMutex.withLock { cache[cacheKey] to writeSequence }
            if (cached != null && now - cached.fetchedAtMs <= maxAgeMs) return@withLock cached.records
            val records = provider.fetchRatings(profileId, ratingScope)
                .filter { it.rating in TRACKING_RATING_MIN..TRACKING_RATING_MAX }
            cacheMutex.withLock {
                // A write that landed mid-fetch makes this snapshot stale; keep it out of the cache.
                if (writeSequence == sequenceBefore) cache[cacheKey] = CachedRatings(nowMs(), records)
            }
            records
        }
    }

    // ── writes ──────────────────────────────────────────────────────────

    fun rate(target: TrackingRatingTarget, rating: Int) {
        require(rating in TRACKING_RATING_MIN..TRACKING_RATING_MAX) { "Rating must be 1..10" }
        write(target, rating)
    }

    fun remove(target: TrackingRatingTarget) = write(target, null)

    private fun write(target: TrackingRatingTarget, rating: Int?) {
        val providers = enabledProvidersFor(target)
        if (providers.isEmpty()) {
            scope.launch { NuvioToastController.show(getString(Res.string.user_rating_no_provider)) }
            return
        }
        val profileId = _uiState.value.profileId
        val key = entryKey(target)
        val previous = _uiState.value.entries[key]?.byProvider.orEmpty()
        updateEntry(key) { entry ->
            entry.copy(
                byProvider = entry.byProvider + providers.associateWith { rating },
                savingProviders = entry.savingProviders + providers,
                failedProviders = entry.failedProviders - providers.toSet(),
                revision = entry.revision + 1,
            )
        }
        scope.launch {
            val outcomes = coroutineScope {
                providers.map { providerId ->
                    async {
                        providerId to runCatching {
                            val provider = TrackingProviderRegistry.ratingProvider(providerId)
                                ?: error("Provider not registered")
                            val normalized = normalized(provider, target)
                            if (rating == null) {
                                provider.removeRating(profileId, normalized)
                            } else {
                                provider.setRating(profileId, normalized, rating)
                            }
                            updateCachedRecord(profileId, providerId, normalized, rating)
                        }.onFailure { error ->
                            if (error is CancellationException) throw error
                            log.w { "Failed to write ${providerId.storageId} rating: ${error.message}" }
                        }.isSuccess
                    }
                }.awaitAll()
            }
            val succeeded = outcomes.filter { it.second }.map { it.first }
            val failed = outcomes.filterNot { it.second }.map { it.first }
            updateEntry(key) { entry ->
                entry.copy(
                    byProvider = entry.byProvider + failed.associateWith { previous[it] },
                    savingProviders = entry.savingProviders - providers.toSet(),
                    failedProviders = entry.failedProviders + failed,
                )
            }
            if (succeeded.isNotEmpty()) {
                val names = succeeded.joinToString(", ") { it.displayName }
                NuvioToastController.show(
                    if (rating == null) {
                        getString(Res.string.user_rating_removed, names)
                    } else {
                        getString(Res.string.user_rating_saved, rating, names)
                    },
                )
            }
            if (failed.isNotEmpty()) {
                NuvioToastController.show(
                    getString(Res.string.user_rating_sync_failed, failed.joinToString(", ") { it.displayName }),
                    durationMillis = 4000L,
                )
            }
        }
    }

    private suspend fun updateCachedRecord(
        profileId: Int,
        providerId: TrackingProviderId,
        target: TrackingRatingTarget,
        rating: Int?,
    ) = cacheMutex.withLock {
        writeSequence += 1
        val cacheKey = cacheKey(profileId, providerId, target.scope)
        val cached = cache[cacheKey] ?: return@withLock
        val remaining = cached.records.filterNot { it.matches(target) }
        val next = if (rating == null) {
            remaining
        } else {
            remaining + TrackingRatingRecord(target.scope, target.ids, rating, target.season, target.episode)
        }
        cache[cacheKey] = cached.copy(records = next)
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private var registered = false

    private fun ensureRegistered() {
        if (registered) return
        ensureTrackingProvidersRegistered()
        registered = true
    }

    private suspend fun normalized(
        provider: TrackingRatingProvider,
        target: TrackingRatingTarget,
    ): TrackingRatingTarget {
        val key = "${provider.providerId.storageId}|${target.key}"
        cacheMutex.withLock { normalizedTargets[key] }?.let { return it }
        val result = runCatching { provider.normalize(target) }
            .onFailure { if (it is CancellationException) throw it }
            .getOrDefault(target)
        cacheMutex.withLock { normalizedTargets[key] = result }
        return result
    }

    private fun updateEntry(key: String, transform: (UserRatingEntry) -> UserRatingEntry) {
        _uiState.update { state ->
            state.copy(entries = state.entries + (key to transform(state.entries[key] ?: UserRatingEntry())))
        }
    }

    private fun syncProfile() {
        val profileId = ProfileRepository.activeProfileId
        if (_uiState.value.profileId == profileId) return
        val disabled = UserRatingsStorage.loadDisabledProviders(profileId)
            .orEmpty()
            .split(',')
            .mapNotNull(TrackingProviderId::fromStorage)
            .toSet()
        _uiState.update { it.copy(profileId = profileId, disabledProviders = disabled) }
    }

    /** Includes the account generation so reconnecting a different account never reuses stale ratings. */
    private fun cacheKey(profileId: Int, providerId: TrackingProviderId, scope: TrackingRatingScope): String {
        val generation = TrackingProviderRegistry.authProvider(providerId)?.accountGeneration ?: 0L
        return "$profileId|${providerId.storageId}|$generation|${scope.name}"
    }

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()
}
