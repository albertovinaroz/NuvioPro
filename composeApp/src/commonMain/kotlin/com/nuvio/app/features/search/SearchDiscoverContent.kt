package com.nuvio.app.features.search

import com.nuvio.app.core.ui.StaggeredEntrance
import com.nuvio.app.core.ui.NuvioEmptyState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.time.TimeMark
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.nuvio.app.core.ui.nuvio
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.core.ui.NuvioDropdownChip
import com.nuvio.app.core.ui.NuvioDropdownOption
import com.nuvio.app.core.ui.NuvioNetworkOfflineCard
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.components.PosterGridRow
import com.nuvio.app.features.home.components.PosterGridSkeletonRow
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.discoverContent(
    state: DiscoverUiState,
    isSourceLoading: Boolean,
    columns: Int,
    networkCondition: NetworkCondition,
    onTypeSelected: (String) -> Unit,
    onCatalogSelected: (String) -> Unit,
    onGenreSelected: (String?) -> Unit,
    onRetry: (() -> Unit)? = null,
    watchedKeys: Set<String> = emptySet(),
    fullyWatchedSeriesKeys: Set<String> = emptySet(),
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
    /** When the current results first arrived, for their staggered entrance. */
    entranceBatch: TimeMark? = null,
) {
    // Keys change with the filter, so switching catalog or genre swaps the rows with a crossfade
    // instead of rebinding the old ones in place.
    val filterKey = "${state.selectedCatalogKey}|${state.selectedGenre}"
    state.selectedCatalog?.let { selectedCatalog ->
        item(key = "discover_context") {
            Text(
                text = stringResource(
                    Res.string.discover_catalog_context,
                    selectedCatalog.addonName,
                    selectedCatalog.type.displayTypeLabel(),
                ),
                modifier = Modifier
                    .animateItem()
                    .padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    when {
        (state.isLoading || isSourceLoading) && state.items.isEmpty() -> {
            items(2, key = { "discover_skeleton_$it" }) {
                PosterGridSkeletonRow(
                    columns = columns,
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        state.items.isEmpty() -> {
            item(key = "discover_empty") {
                DiscoverEmptyStateCard(
                    reason = state.emptyStateReason,
                    errorMessage = state.errorMessage,
                    networkCondition = networkCondition,
                    onRetry = onRetry,
                    modifier = Modifier.animateItem(),
                )
            }
        }

        else -> {
            items(
                count = (state.items.size + columns - 1) / columns,
                key = { rowIndex -> "discover_row:$filterKey:$rowIndex" },
            ) { rowIndex ->
                val firstIndex = rowIndex * columns
                val row = @Composable {
                    PosterGridRow(
                        items = state.items.subList(firstIndex, minOf(firstIndex + columns, state.items.size)),
                        columns = columns,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        watchedKeys = watchedKeys,
                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                        onPosterClick = onPosterClick,
                        onPosterLongClick = onPosterLongClick,
                    )
                }
                // The entrance does the fading in; animateItem only fades the old rows out.
                Box(modifier = Modifier.animateItem(fadeInSpec = null)) {
                    if (entranceBatch != null) {
                        StaggeredEntrance(batch = entranceBatch, index = rowIndex, content = row)
                    } else {
                        row()
                    }
                }
            }
            if (state.isLoading) {
                item(key = "discover_loading_more") {
                    CatalogLoadingFooter(
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun DiscoverFilterRow(
    state: DiscoverUiState,
    onTypeSelected: (String) -> Unit,
    onCatalogSelected: (String) -> Unit,
    onGenreSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    /** Kept inside the scrolling row so chips scroll out to the screen edge, under the fade. */
    horizontalPadding: Dp = 16.dp,
) {
    val scrollState = rememberScrollState()
    val haptics = LocalHapticFeedback.current
    fun tick() = haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .scrollEdgeFade(scrollState)
            .horizontalScroll(scrollState)
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val selectedType = state.selectedType
        NuvioDropdownChip(
            title = stringResource(Res.string.discover_select_type),
            label = selectedType?.displayTypeLabel() ?: stringResource(Res.string.discover_type),
            selectedKey = selectedType,
            options = state.typeOptions.map { NuvioDropdownOption(key = it, label = it.displayTypeLabel()) },
            enabled = state.typeOptions.isNotEmpty(),
            onSelected = { tick(); onTypeSelected(it.key) },
        )
        NuvioDropdownChip(
            title = stringResource(Res.string.discover_select_catalog),
            label = state.selectedCatalog?.catalogName ?: stringResource(Res.string.discover_catalog),
            selectedKey = state.selectedCatalogKey,
            options = state.catalogOptions.map { option -> NuvioDropdownOption(key = option.key, label = option.catalogName) },
            enabled = state.catalogOptions.isNotEmpty(),
            onSelected = { tick(); onCatalogSelected(it.key) },
        )

        val selectedCatalog = state.selectedCatalog
        val genreRequired = selectedCatalog?.genreRequired == true
        val genreOptions = buildList {
            if (!genreRequired) {
                add(NuvioDropdownOption(key = "", label = stringResource(Res.string.discover_all_genres)))
            }
            addAll(state.genreOptions.map { genre -> NuvioDropdownOption(key = genre, label = genre) })
        }
        NuvioDropdownChip(
            title = stringResource(Res.string.discover_select_genre),
            label = state.selectedGenre ?: stringResource(Res.string.discover_all_genres),
            selectedKey = state.selectedGenre ?: "",
            options = genreOptions,
            enabled = genreOptions.size > 1 || genreRequired,
            onSelected = { option ->
                tick()
                onGenreSelected(option.key.ifBlank { null })
            },
            // A picked genre narrows the catalog; one the catalog requires is just its default.
            active = state.selectedGenre != null && !genreRequired,
            onClear = { tick(); onGenreSelected(null) }.takeIf { !genreRequired },
        )
    }
}

/** Fades the row out at whichever edge still has chips scrolled past it. */
private fun Modifier.scrollEdgeFade(scrollState: ScrollState): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val fade = 28.dp.toPx().coerceAtMost(size.width / 4f)
            if (scrollState.canScrollBackward) {
                drawRect(
                    brush = Brush.horizontalGradient(0f to Color.Transparent, 1f to Color.Black, endX = fade),
                    size = Size(fade, size.height),
                    blendMode = BlendMode.DstIn,
                )
            }
            if (scrollState.canScrollForward) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Black,
                        1f to Color.Transparent,
                        startX = size.width - fade,
                        endX = size.width,
                    ),
                    topLeft = Offset(size.width - fade, 0f),
                    size = Size(fade, size.height),
                    blendMode = BlendMode.DstIn,
                )
            }
        }

@Composable
private fun CatalogLoadingFooter(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        NuvioLoadingIndicator(
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun DiscoverEmptyStateCard(
    reason: DiscoverEmptyStateReason?,
    errorMessage: String?,
    networkCondition: NetworkCondition,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (
        reason == DiscoverEmptyStateReason.RequestFailed &&
        (networkCondition == NetworkCondition.NoInternet || networkCondition == NetworkCondition.ServersUnreachable)
    ) {
        NuvioNetworkOfflineCard(
            condition = networkCondition,
            modifier = modifier.padding(horizontal = 16.dp),
            onRetry = onRetry,
        )
        return
    }

    val icon: ImageVector
    val title: String
    val message: String

    when (reason) {
        DiscoverEmptyStateReason.NoActiveAddons -> {
            icon = Icons.Rounded.Extension
            title = stringResource(Res.string.home_empty_no_sources_title)
            message = stringResource(Res.string.search_empty_no_sources_message)
        }

        DiscoverEmptyStateReason.NoDiscoverCatalogs -> {
            icon = Icons.Rounded.Explore
            title = stringResource(Res.string.discover_empty_no_catalogs_title)
            message = stringResource(Res.string.discover_empty_no_catalogs_message)
        }

        DiscoverEmptyStateReason.RequestFailed -> {
            icon = Icons.Rounded.ErrorOutline
            title = stringResource(Res.string.discover_empty_load_failed_title)
            message = errorMessage ?: stringResource(Res.string.discover_empty_load_failed_message)
        }

        DiscoverEmptyStateReason.NoResults, null -> {
            icon = Icons.Rounded.SearchOff
            title = stringResource(Res.string.discover_empty_no_results_title)
            message = stringResource(Res.string.discover_empty_no_results_message)
        }
    }

    NuvioEmptyState(
        icon = icon,
        title = title,
        message = message,
        actionLabel = if (reason == DiscoverEmptyStateReason.RequestFailed) {
            stringResource(Res.string.action_retry)
        } else {
            null
        },
        onAction = if (reason == DiscoverEmptyStateReason.RequestFailed) onRetry else null,
        modifier = modifier,
    )
}

@Composable
private fun String.displayTypeLabel(): String =
    when (lowercase()) {
        "movie" -> stringResource(Res.string.media_movies)
        "series" -> stringResource(Res.string.media_series)
        "anime" -> stringResource(Res.string.media_anime)
        "channel" -> stringResource(Res.string.media_channels)
        "tv" -> stringResource(Res.string.media_tv)
        else -> replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
