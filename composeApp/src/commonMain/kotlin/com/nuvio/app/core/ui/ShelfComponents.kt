package com.nuvio.app.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.nuvio.app.features.settings.ThemeSettingsRepository
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.home_view_all
import nuvio.composeapp.generated.resources.poster_logo_content_description
import org.jetbrains.compose.resources.stringResource

enum class NuvioPosterShape {
    Poster,
    Square,
    Landscape,
}

enum class NuvioViewAllPillSize {
    Default,
    Compact,
}

@Composable
fun <T> NuvioShelfSection(
    title: String,
    entries: List<T>,
    modifier: Modifier = Modifier,
    headerHorizontalPadding: Dp = 0.dp,
    rowContentPadding: PaddingValues = PaddingValues(0.dp),
    rowModifier: Modifier = Modifier,
    itemSpacing: Dp = 10.dp,
    onViewAllClick: (() -> Unit)? = null,
    viewAllPillSize: NuvioViewAllPillSize = NuvioViewAllPillSize.Default,
    key: ((T) -> Any)? = null,
    animatePlacement: Boolean = false,
    state: LazyListState = rememberLazyListState(),
    itemContent: @Composable (T) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap + NuvioTokens.Space.s2),
    ) {
        if (title.isNotBlank()) {
            NuvioShelfSectionHeader(
                title = title,
                modifier = Modifier.padding(horizontal = headerHorizontalPadding),
                onViewAllClick = onViewAllClick,
                viewAllPillSize = viewAllPillSize,
            )
        }
        LazyRow(
            modifier = rowModifier,
            state = state,
            contentPadding = rowContentPadding,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            if (key != null) {
                items(
                    items = entries.withDuplicateSafeLazyKeys(key),
                    key = { entry -> entry.lazyKey },
                ) { keyedEntry ->
                    if (animatePlacement) {
                        Box(modifier = Modifier.animateItem()) { itemContent(keyedEntry.value) }
                    } else {
                        itemContent(keyedEntry.value)
                    }
                }
            } else {
                items(entries) { entry ->
                    if (animatePlacement) {
                        Box(modifier = Modifier.animateItem()) { itemContent(entry) }
                    } else {
                        itemContent(entry)
                    }
                }
            }
        }
    }
}

@Composable
fun NuvioPosterCard(
    title: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    shape: NuvioPosterShape = NuvioPosterShape.Poster,
    detailLine: String? = null,
    showTitleBelow: Boolean = true,
    bottomLeftLogoUrl: String? = null,
    bottomLeftText: String? = null,
    isWatched: Boolean = false,
    isRecentlyAdded: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val tokens = MaterialTheme.nuvio
    val cardWidth = shape.cardWidth(basePosterWidthDp = posterCardStyle.widthDp)
    val cardShape = RoundedCornerShape(posterCardStyle.cornerRadiusDp.dp)
    val catalogLogoOverlaySize = catalogLogoOverlaySize(
        basePosterWidthDp = posterCardStyle.widthDp,
        shape = shape,
    )
    val shouldShowTitleBelow = showTitleBelow && !posterCardStyle.hideLabelsEnabled

    Column(
        modifier = modifier.width(cardWidth),
        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s6),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(shape.aspectRatio)
                .clip(cardShape)
                .background(tokens.colors.surface)
                .nuvioCardDepth(
                    shape = cardShape,
                    surface = NuvioCardDepthSurface.Posters,
                )
                .posterCardClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    zoomImageUrl = imageUrl,
                    zoomCornerRadius = posterCardStyle.cornerRadiusDp.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = NuvioTokens.Space.s14),
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.colors.textMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!bottomLeftLogoUrl.isNullOrBlank() || !bottomLeftText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = NuvioTokens.Space.s10, vertical = NuvioTokens.Space.s10),
                ) {
                    if (!bottomLeftLogoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = bottomLeftLogoUrl,
                            contentDescription = stringResource(Res.string.poster_logo_content_description, title),
                            modifier = Modifier
                                .width(catalogLogoOverlaySize.width)
                                .height(catalogLogoOverlaySize.height),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text(
                            text = bottomLeftText.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = catalogLogoOverlaySize.textMaxWidth),
                        )
                    }
                }
            }

            NuvioPosterWatchedOverlay(isWatched = isWatched)
            NuvioPosterRecentlyAddedOverlay(isRecentlyAdded = isRecentlyAdded)
        }
        if (shouldShowTitleBelow) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!detailLine.isNullOrBlank()) {
                Text(
                    text = detailLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Box(modifier = Modifier.height(NuvioTokens.Space.none))
            }
        } else {
            Box(modifier = Modifier.height(NuvioTokens.Space.none))
        }
    }
}

// How much of the poster's own width the number column adds, and how much of that column the
// poster is then pulled back over — together giving the classic Netflix "Top 10" look of a big
// numeral partly tucked behind the poster's left edge, rather than sitting fully beside it. A
// two-digit rank needs a noticeably wider number column and a lighter overlap, or the poster ends
// up covering its trailing digit entirely.
//
// Landscape posters are already wide (see landscapePosterWidth), so reusing the portrait
// fractions here would make the combined card absurdly wide relative to the poster itself. Instead
// the number column is kept narrow and pulled in with a much heavier overlap — and the width that
// approach *doesn't* spend gets handed back to the poster itself via posterWidthBoost, so the card
// stays a similar overall footprint while the poster reads bigger than its normal landscape size.
private const val Top10NumberAreaWidthFraction = 0.62f
private const val Top10NumberAreaWidthFractionTwoDigits = 0.86f
private const val Top10NumberPosterOverlapFraction = 0.35f
private const val Top10NumberPosterOverlapFractionTwoDigits = 0.16f
private const val Top10LandscapeNumberAreaWidthFraction = 0.30f
private const val Top10LandscapeNumberAreaWidthFractionTwoDigits = 0.40f
private const val Top10LandscapeNumberPosterOverlapFraction = 0.55f
private const val Top10LandscapeNumberPosterOverlapFractionTwoDigits = 0.40f
private const val Top10LandscapePosterWidthBoost = 1.18f

/**
 * The large-rank-number card used for "Top 10"-style rows (see
 * HomeCatalogSettingsRepository.setTop10StyleEnabled) — a poster with a big numeral to its left,
 * the two overlapping slightly to read as one wide unit rather than two separate elements.
 */
@Composable
fun NuvioTop10PosterCard(
    rank: Int,
    title: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    shape: NuvioPosterShape = NuvioPosterShape.Poster,
    outlinedNumber: Boolean = false,
    bottomLeftLogoUrl: String? = null,
    bottomLeftText: String? = null,
    detailLine: String? = null,
    showTitleBelow: Boolean = true,
    isWatched: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val tokens = MaterialTheme.nuvio
    val density = LocalDensity.current
    val isTwoDigitRank = rank >= 10
    val isLandscape = shape == NuvioPosterShape.Landscape
    val posterWidth = shape.cardWidth(posterCardStyle.widthDp) *
        if (isLandscape) Top10LandscapePosterWidthBoost else 1f
    val posterHeight = posterWidth / shape.aspectRatio
    val cardShape = RoundedCornerShape(posterCardStyle.cornerRadiusDp.dp)
    val numberAreaWidthFraction = if (isLandscape) {
        if (isTwoDigitRank) Top10LandscapeNumberAreaWidthFractionTwoDigits else Top10LandscapeNumberAreaWidthFraction
    } else {
        if (isTwoDigitRank) Top10NumberAreaWidthFractionTwoDigits else Top10NumberAreaWidthFraction
    }
    val overlapFraction = if (isLandscape) {
        if (isTwoDigitRank) Top10LandscapeNumberPosterOverlapFractionTwoDigits else Top10LandscapeNumberPosterOverlapFraction
    } else {
        if (isTwoDigitRank) Top10NumberPosterOverlapFractionTwoDigits else Top10NumberPosterOverlapFraction
    }
    val numberAreaWidth = posterWidth * numberAreaWidthFraction
    val overlap = numberAreaWidth * overlapFraction
    val totalWidth = numberAreaWidth + posterWidth - overlap
    val shouldShowTitleBelow = showTitleBelow && !posterCardStyle.hideLabelsEnabled

    Column(
        modifier = modifier.width(totalWidth),
        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s6),
    ) {
        Box(
            modifier = Modifier
                .width(totalWidth)
                .height(posterHeight),
        ) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = with(density) {
                        (posterHeight * if (isTwoDigitRank) 0.78f else 0.88f).toSp()
                    },
                    fontWeight = FontWeight.Black,
                    letterSpacing = if (isTwoDigitRank) (-3).sp else (-1.5).sp,
                    drawStyle = if (outlinedNumber) {
                        Stroke(width = with(density) { (posterHeight.value * 0.007f).dp.toPx() })
                    } else {
                        null
                    },
                ),
                color = tokens.colors.textPrimary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(y = posterHeight * 0.05f),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(posterWidth)
                    .height(posterHeight)
                    .clip(cardShape)
                    .background(tokens.colors.surface)
                    .nuvioCardDepth(
                        shape = cardShape,
                        surface = NuvioCardDepthSurface.Posters,
                    )
                    .posterCardClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                        zoomImageUrl = imageUrl,
                        zoomCornerRadius = posterCardStyle.cornerRadiusDp.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = title,
                        modifier = Modifier.padding(horizontal = NuvioTokens.Space.s14),
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.colors.textMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (!bottomLeftLogoUrl.isNullOrBlank() || !bottomLeftText.isNullOrBlank()) {
                    val catalogLogoOverlaySize = catalogLogoOverlaySize(
                        basePosterWidthDp = posterCardStyle.widthDp,
                        shape = shape,
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = NuvioTokens.Space.s10, vertical = NuvioTokens.Space.s10),
                    ) {
                        if (!bottomLeftLogoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = bottomLeftLogoUrl,
                                contentDescription = stringResource(Res.string.poster_logo_content_description, title),
                                modifier = Modifier
                                    .width(catalogLogoOverlaySize.width)
                                    .height(catalogLogoOverlaySize.height),
                                contentScale = ContentScale.Fit,
                            )
                        } else {
                            Text(
                                text = bottomLeftText.orEmpty(),
                                style = MaterialTheme.typography.labelMedium,
                                color = tokens.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = catalogLogoOverlaySize.textMaxWidth),
                            )
                        }
                    }
                }
                NuvioPosterWatchedOverlay(isWatched = isWatched)
            }
        }
        if (shouldShowTitleBelow) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!detailLine.isNullOrBlank()) {
                Text(
                    text = detailLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Box(modifier = Modifier.height(NuvioTokens.Space.none))
            }
        } else {
            Box(modifier = Modifier.height(NuvioTokens.Space.none))
        }
    }
}

@Composable
private fun NuvioShelfSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onViewAllClick: (() -> Unit)? = null,
    viewAllPillSize: NuvioViewAllPillSize = NuvioViewAllPillSize.Default,
) {
    val tokens = MaterialTheme.nuvio
    val showAccent by ThemeSettingsRepository.showCatalogAccentEnabled.collectAsState()
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                color = tokens.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val viewAllPlaceholderModifier = if (onViewAllClick == null) {
                Modifier
                    .alpha(0f)
                    .clearAndSetSemantics { }
            } else {
                Modifier
            }
            NuvioViewAllPill(
                onClick = onViewAllClick,
                size = viewAllPillSize,
                modifier = viewAllPlaceholderModifier,
            )
        }
        if (showAccent) {
            Box(
                modifier = Modifier
                    .padding(top = NuvioTokens.Space.s6)
                    .width(NuvioTokens.Space.s64 - NuvioTokens.Space.s4)
                    .height(NuvioTokens.Space.s4)
                    .background(color = tokens.colors.accent, shape = tokens.shapes.chip),
            )
        }
    }
}

@Composable
private fun NuvioViewAllPill(
    onClick: (() -> Unit)?,
    size: NuvioViewAllPillSize,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val actionSize = if (size == NuvioViewAllPillSize.Compact) NuvioTokens.Space.s32 else NuvioTokens.Space.s40
    val iconSize = if (size == NuvioViewAllPillSize.Compact) NuvioTokens.Icon.sm else tokens.icons.md
    val viewAllText = stringResource(Res.string.home_view_all)

    Box(
        modifier = modifier
            .size(actionSize)
            .background(
                color = tokens.colors.surface,
                shape = RoundedCornerShape(NuvioTokens.Radius.xl),
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = viewAllText,
            tint = tokens.colors.textMuted,
            modifier = Modifier.size(iconSize),
        )
    }
}

private val NuvioPosterShape.aspectRatio: Float
    get() = when (this) {
        NuvioPosterShape.Poster -> 0.675f
        NuvioPosterShape.Square -> 1f
        NuvioPosterShape.Landscape -> PosterLandscapeAspectRatio
    }

private data class CatalogLogoOverlaySize(
    val width: Dp,
    val height: Dp,
    val textMaxWidth: Dp,
)

private fun catalogLogoOverlaySize(
    basePosterWidthDp: Int,
    shape: NuvioPosterShape,
): CatalogLogoOverlaySize =
    if (shape == NuvioPosterShape.Landscape) {
        when {
            basePosterWidthDp <= 108 -> CatalogLogoOverlaySize(width = 92.dp, height = 24.dp, textMaxWidth = 120.dp)
            basePosterWidthDp <= 120 -> CatalogLogoOverlaySize(width = 104.dp, height = 28.dp, textMaxWidth = 132.dp)
            basePosterWidthDp <= 132 -> CatalogLogoOverlaySize(width = 116.dp, height = 30.dp, textMaxWidth = 144.dp)
            else -> CatalogLogoOverlaySize(width = 128.dp, height = 34.dp, textMaxWidth = 156.dp)
        }
    } else {
        when {
            basePosterWidthDp <= 108 -> CatalogLogoOverlaySize(width = 72.dp, height = 18.dp, textMaxWidth = 92.dp)
            basePosterWidthDp <= 120 -> CatalogLogoOverlaySize(width = 80.dp, height = 20.dp, textMaxWidth = 104.dp)
            basePosterWidthDp <= 132 -> CatalogLogoOverlaySize(width = 88.dp, height = 22.dp, textMaxWidth = 112.dp)
            else -> CatalogLogoOverlaySize(width = 96.dp, height = 24.dp, textMaxWidth = 124.dp)
        }
    }

private fun NuvioPosterShape.cardWidth(basePosterWidthDp: Int): Dp =
    when (this) {
        NuvioPosterShape.Poster -> basePosterWidthDp.dp
        NuvioPosterShape.Square -> basePosterWidthDp.dp
        NuvioPosterShape.Landscape -> landscapePosterWidth(basePosterWidthDp)
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.posterCardClickable(
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    zoomImageUrl: String? = null,
    zoomCornerRadius: Dp = NuvioTokens.Radius.poster,
): Modifier {
    if (onClick == null && onLongClick == null) return this
    val bounds = remember { mutableStateOf<Rect?>(null) }
    return this
        .onGloballyPositioned { coordinates -> bounds.value = coordinates.unclippedBoundsInRoot() }
        .combinedClickable(
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick?.let { longClick ->
                {
                    bounds.value?.let { cardBounds ->
                        PosterZoomAnchorHolder.stash(
                            PosterZoomAnchor(
                                boundsInRoot = cardBounds,
                                imageUrl = zoomImageUrl,
                                cornerRadius = zoomCornerRadius,
                            ),
                        )
                    }
                    longClick()
                }
            },
        )
}

private fun androidx.compose.ui.layout.LayoutCoordinates.unclippedBoundsInRoot(): Rect {
    val position = positionInRoot()
    return Rect(
        left = position.x,
        top = position.y,
        right = position.x + size.width,
        bottom = position.y + size.height,
    )
}
