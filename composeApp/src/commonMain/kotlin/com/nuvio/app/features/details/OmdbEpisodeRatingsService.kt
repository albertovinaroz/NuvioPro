package com.nuvio.app.features.details

import co.touchlab.kermit.Logger
import com.nuvio.app.core.time.EpisodeReleaseDatePlatform
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.tmdb.TmdbSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Replaces TMDB's per-episode vote average with IMDb's own rating, via OMDb's bulk
 * per-season endpoint (`?i=ttXXXX&Season=N`) — one request per season instead of one per
 * episode, matched to how [com.nuvio.app.features.tmdb.TmdbMetadataService] already fetches
 * TMDB season details in parallel.
 *
 * Ratings are cached to disk (not just in memory) since they almost never change once a
 * season has aired, and the shared per-build OMDb key is rate-limited — every rating already
 * on disk is one less request drawn from that quota on the next app launch.
 */
internal object OmdbEpisodeRatingsService {
    /** Oldest entries are evicted first once this many season-rating sets are cached. */
    private const val MaxCachedSeasons = 400

    // A season is "incomplete" when OMDb has fewer rated episodes than episodes it lists at
    // all — almost always because some just aired and haven't accumulated enough IMDb votes
    // yet, not because they'll never be rated. Retry those after a cooldown instead of caching
    // the gap forever, or an episode that airs a week before someone checks a show stays stuck
    // showing TMDB's rating indefinitely even once IMDb catches up.
    private const val IncompleteSeasonRetryAfterMs = 2 * 24 * 60 * 60 * 1000L // 2 days

    private val log = Logger.withTag("OmdbEpisodeRatings")
    private val json = Json { ignoreUnknownKeys = true }
    private val imdbIdRegex = Regex("tt\\d+")

    // LinkedHashMap preserves insertion order across all KMP targets, so evicting the
    // first entries once over the cap behaves as a simple oldest-in/first-out cache.
    private val seasonCache = LinkedHashMap<String, CachedSeasonRatings>()
    private var hasLoadedFromDisk = false

    val hasApiKey: Boolean
        get() = OmdbSettingsRepository.effectiveApiKey().isNotBlank()

    fun shouldApply(meta: MetaDetails, settings: TmdbSettings): Boolean =
        hasApiKey &&
            settings.enabled &&
            settings.useEpisodeRatings &&
            meta.videos.any { it.season != null && it.episode != null }

    suspend fun applyTo(
        meta: MetaDetails,
        fallbackItemId: String,
        settings: TmdbSettings,
    ): MetaDetails {
        if (!shouldApply(meta, settings)) return meta
        val imdbId = extractImdbId(meta.imdbId, meta.id, fallbackItemId) ?: return meta
        val ratings = fetchRatings(
            imdbId = imdbId,
            seasonNumbers = meta.videos.mapNotNull { it.season }.distinct(),
        )
        if (ratings.isEmpty()) return meta
        return meta.copy(
            videos = meta.videos.map { video ->
                val season = video.season ?: return@map video
                val episode = video.episode ?: return@map video
                ratings[season to episode]?.let { video.copy(imdbRating = it) } ?: video
            },
        )
    }

    fun extractImdbId(vararg candidates: String?): String? =
        candidates.firstNotNullOfOrNull { candidate ->
            candidate?.let(imdbIdRegex::find)?.value
        }

    /** Returns (season, episode) -> IMDb rating, only for episodes OMDb actually rated. */
    suspend fun fetchRatings(
        imdbId: String,
        seasonNumbers: List<Int>,
    ): Map<Pair<Int, Int>, Double> = withContext(Dispatchers.Default) {
        val apiKey = OmdbSettingsRepository.effectiveApiKey()
        if (apiKey.isBlank()) return@withContext emptyMap()

        val normalizedSeasons = seasonNumbers.distinct().sorted()
        if (normalizedSeasons.isEmpty()) return@withContext emptyMap()

        ensureLoadedFromDisk()

        val perSeason = coroutineScope {
            normalizedSeasons.map { season ->
                async { season to fetchSeason(imdbId = imdbId, season = season, apiKey = apiKey) }
            }.awaitAll()
        }

        perSeason.flatMap { (season, episodeRatings) ->
            episodeRatings.map { (episode, rating) -> (season to episode) to rating }
        }.toMap()
    }

    private fun ensureLoadedFromDisk() {
        if (hasLoadedFromDisk) return
        hasLoadedFromDisk = true
        val payload = OmdbEpisodeRatingsStorage.loadPayload()?.trim().orEmpty()
        if (payload.isEmpty()) return
        runCatching { json.decodeFromString<Map<String, CachedSeasonRatings>>(payload) }
            .onSuccess { seasonCache.putAll(it) }
            .onFailure { error -> log.w { "Failed to load cached IMDb ratings: ${error.message}" } }
    }

    private fun persistToDisk() {
        val payload = runCatching { json.encodeToString<Map<String, CachedSeasonRatings>>(seasonCache) }
            .getOrNull() ?: return
        OmdbEpisodeRatingsStorage.savePayload(payload)
    }

    private suspend fun fetchSeason(
        imdbId: String,
        season: Int,
        apiKey: String,
    ): Map<Int, Double> {
        val cacheKey = "$imdbId:$season"
        val cached = seasonCache[cacheKey]
        if (cached != null) {
            val dueForRetry = !cached.isComplete &&
                EpisodeReleaseDatePlatform.nowEpochMs() - cached.fetchedAtEpochMs >= IncompleteSeasonRetryAfterMs
            if (!dueForRetry) return cached.ratings
        }

        val url = "https://www.omdbapi.com/?i=$imdbId&Season=$season&apikey=$apiKey"
        val response = runCatching {
            json.decodeFromString<OmdbSeasonResponse>(httpGetText(url))
        }.onFailure { error ->
            log.w { "OMDb season request failed for $imdbId season $season: ${error.message}" }
        }.getOrNull() ?: return cached?.ratings.orEmpty()

        if (!response.response.equals("True", ignoreCase = true)) return cached?.ratings.orEmpty()

        val episodes = response.episodes.orEmpty()
        val ratings = episodes
            .mapNotNull { episode ->
                val episodeNumber = episode.episode?.toIntOrNull() ?: return@mapNotNull null
                val rating = episode.imdbRating
                    ?.takeIf { it.isNotBlank() && !it.equals("N/A", ignoreCase = true) }
                    ?.toDoubleOrNull()
                    ?.takeIf { it > 0.0 }
                    ?: return@mapNotNull null
                episodeNumber to rating
            }
            .toMap()

        if (episodes.isNotEmpty()) {
            seasonCache[cacheKey] = CachedSeasonRatings(
                ratings = ratings,
                episodeCount = episodes.size,
                fetchedAtEpochMs = EpisodeReleaseDatePlatform.nowEpochMs(),
            )
            while (seasonCache.size > MaxCachedSeasons) {
                val oldestKey = seasonCache.keys.firstOrNull() ?: break
                seasonCache.remove(oldestKey)
            }
            persistToDisk()
        }
        return ratings
    }
}

/**
 * [episodeCount] is how many episodes OMDb's season response listed at fetch time (rated or
 * not) — comparing it to `ratings.size` is what lets a season with newly-aired, not-yet-rated
 * episodes be recognized as [isComplete] == false and retried later, instead of caching the gap
 * forever.
 */
@Serializable
private data class CachedSeasonRatings(
    val ratings: Map<Int, Double>,
    val episodeCount: Int,
    val fetchedAtEpochMs: Long,
) {
    val isComplete: Boolean get() = ratings.size >= episodeCount
}

@Serializable
private data class OmdbSeasonResponse(
    @SerialName("Response") val response: String? = null,
    @SerialName("Episodes") val episodes: List<OmdbEpisodeEntry>? = null,
)

@Serializable
private data class OmdbEpisodeEntry(
    @SerialName("Episode") val episode: String? = null,
    @SerialName("imdbRating") val imdbRating: String? = null,
)
