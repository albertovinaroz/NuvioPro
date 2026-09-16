package com.nuvio.app.features.library

import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.PosterShape
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val LibraryRatingMax = 5

/**
 * A rating plus just enough display data to render it in the "Rated" view without depending on
 * the item being saved to the library, or on some other cache (e.g. [MetaDetailsRepository])
 * still holding it.
 */
@Serializable
data class RatedLibraryEntry(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val posterShape: PosterShape = PosterShape.Poster,
    val releaseInfo: String? = null,
    val rating: Int,
    val ratedAtEpochMs: Long,
)

fun RatedLibraryEntry.toMetaPreview(): MetaPreview = MetaPreview(
    id = id,
    type = type,
    name = name,
    poster = poster,
    posterShape = posterShape,
    releaseInfo = releaseInfo,
)

/** For navigating to the details screen — this rated entry may not be a saved [LibraryItem]. */
internal fun RatedLibraryEntry.toLibraryItem(): LibraryItem = LibraryItem(
    id = id,
    type = type,
    name = name,
    poster = poster,
    posterShape = posterShape,
    releaseInfo = releaseInfo,
    savedAtEpochMs = ratedAtEpochMs,
)

data class LibraryRatingsUiState(
    val entries: Map<String, RatedLibraryEntry> = emptyMap(),
)

/**
 * A user's own 0-5 star rating for a piece of content, kept entirely on-device per profile —
 * unlike [LibraryRepository], it never syncs to Trakt/Simkl/Nuvio Cloud, and it isn't tied to
 * library membership: a title can be rated without ever being saved to the library, the same way
 * rating a film on Letterboxd or Trakt doesn't add it to a list.
 */
object LibraryRatingsRepository {
    private val _uiState = MutableStateFlow(LibraryRatingsUiState())
    val uiState: StateFlow<LibraryRatingsUiState> = _uiState.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        _uiState.value = LibraryRatingsUiState()
    }

    fun ratingFor(id: String, type: String): Int {
        ensureLoaded()
        return _uiState.value.entries[libraryItemKey(id, type)]?.rating ?: 0
    }

    /** Pass 0 to clear the rating. */
    fun setRating(preview: MetaPreview, rating: Int) {
        ensureLoaded()
        val clamped = rating.coerceIn(0, LibraryRatingMax)
        val key = libraryItemKey(preview.id, preview.type)
        val current = _uiState.value.entries
        val updated = if (clamped == 0) {
            if (key !in current) return
            current - key
        } else {
            val existing = current[key]
            if (existing?.rating == clamped) return
            val entry = RatedLibraryEntry(
                id = preview.id,
                type = preview.type,
                name = preview.name,
                poster = preview.poster,
                posterShape = preview.posterShape,
                releaseInfo = preview.releaseInfo,
                rating = clamped,
                ratedAtEpochMs = existing?.ratedAtEpochMs ?: LibraryClock.nowEpochMs(),
            )
            current + (key to entry)
        }
        _uiState.value = _uiState.value.copy(entries = updated)
        persist()
    }

    private fun loadFromDisk() {
        hasLoaded = true
        _uiState.value = decodeLibraryRatings(LibraryRatingsStorage.loadPayload())
    }

    private fun persist() {
        LibraryRatingsStorage.savePayload(encodeLibraryRatings(_uiState.value))
    }
}

internal fun encodeLibraryRatings(state: LibraryRatingsUiState): String =
    LibraryRatingsJson.encodeToString(StoredLibraryRatings(entries = state.entries.values.toList()))

internal fun decodeLibraryRatings(payload: String?): LibraryRatingsUiState {
    val stored = payload
        ?.takeIf { it.isNotBlank() }
        ?.let { value ->
            runCatching { LibraryRatingsJson.decodeFromString<StoredLibraryRatings>(value) }.getOrNull()
        }
    val entries = stored?.entries.orEmpty()
        .filter { it.rating in 1..LibraryRatingMax }
        .associateBy { libraryItemKey(it.id, it.type) }
    return LibraryRatingsUiState(entries = entries)
}

private val LibraryRatingsJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
private data class StoredLibraryRatings(
    @SerialName("entries") val entries: List<RatedLibraryEntry> = emptyList(),
)
