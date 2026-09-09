package com.nuvio.app.features.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.ui.NuvioPosterCard
import com.nuvio.app.core.ui.NuvioPosterShape
import com.nuvio.app.core.ui.NuvioTop10PosterCard
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.PosterShape

@Composable
fun HomePosterCard(
    item: MetaPreview,
    modifier: Modifier = Modifier,
    useLandscapeBackdropMode: Boolean = false,
    isWatched: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val isLandscapeMode = useLandscapeBackdropMode || posterCardStyle.catalogLandscapeModeEnabled

    NuvioPosterCard(
        title = item.name,
        imageUrl = if (isLandscapeMode) (item.banner ?: item.poster) else item.poster,
        modifier = modifier,
        shape = if (isLandscapeMode) NuvioPosterShape.Landscape else item.posterShape.toNuvioPosterShape(),
        detailLine = when {
            posterCardStyle.hideLabelsEnabled -> null
            isLandscapeMode -> genreAndRatingLabel(item)
            else -> item.releaseInfo?.let { formatReleaseDateForDisplay(it) }
        },
        showTitleBelow = !posterCardStyle.hideLabelsEnabled,
        bottomLeftLogoUrl = if (isLandscapeMode) item.logo else null,
        bottomLeftText = if (isLandscapeMode && item.logo.isNullOrBlank() && !posterCardStyle.hideLabelsEnabled) item.name else null,
        isWatched = isWatched,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@Composable
fun HomeTop10PosterCard(
    rank: Int,
    item: MetaPreview,
    modifier: Modifier = Modifier,
    useLandscapePoster: Boolean = false,
    outlinedNumber: Boolean = false,
    isWatched: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()

    NuvioTop10PosterCard(
        rank = rank,
        title = item.name,
        imageUrl = if (useLandscapePoster) (item.banner ?: item.poster) else item.poster,
        modifier = modifier,
        shape = if (useLandscapePoster) NuvioPosterShape.Landscape else NuvioPosterShape.Poster,
        outlinedNumber = outlinedNumber,
        bottomLeftLogoUrl = if (useLandscapePoster) item.logo else null,
        bottomLeftText = if (useLandscapePoster && item.logo.isNullOrBlank() && !posterCardStyle.hideLabelsEnabled) item.name else null,
        detailLine = when {
            posterCardStyle.hideLabelsEnabled -> null
            useLandscapePoster -> genreAndRatingLabel(item)
            else -> item.releaseInfo?.let { formatReleaseDateForDisplay(it) }
        },
        showTitleBelow = !posterCardStyle.hideLabelsEnabled,
        isWatched = isWatched,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

// Landscape thumbnails lose the release date shown under portrait covers (it's replaced there by
// the logo overlay baked into the image itself), so this fills that same line with the genre and
// rating instead — data MetaPreview already carries, just never surfaced on the card before.
private fun genreAndRatingLabel(item: MetaPreview): String? {
    val genre = item.genres.firstOrNull()?.takeIf(String::isNotBlank)
    val rating = item.imdbRating?.takeIf(String::isNotBlank)?.let { "★ $it" }
    return listOfNotNull(genre, rating).joinToString(" • ").takeIf(String::isNotBlank)
}

private fun PosterShape.toNuvioPosterShape(): NuvioPosterShape =
    when (this) {
        PosterShape.Poster -> NuvioPosterShape.Poster
        PosterShape.Square -> NuvioPosterShape.Square
        PosterShape.Landscape -> NuvioPosterShape.Landscape
    }
