package com.nuvio.app.features.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioBottomSheetActionRow
import com.nuvio.app.core.ui.NuvioBottomSheetDivider
import com.nuvio.app.core.ui.NuvioDropdownChip
import com.nuvio.app.core.ui.NuvioDropdownOption
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.home.PosterShape
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import com.nuvio.app.features.home.components.PosterGridSkeletonRow
import com.nuvio.app.features.home.components.posterGridColumnCountForWidth
import com.nuvio.app.features.ratings.AggregatedUserRating
import com.nuvio.app.features.ratings.UserRatingsRepository
import com.nuvio.app.features.tracking.TrackingProviderId
import com.nuvio.app.features.tracking.TrackingProviderRegistry
import com.nuvio.app.features.tracking.TrackingRatingScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.cloud_library_refresh
import nuvio.composeapp.generated.resources.library_filter_provider
import nuvio.composeapp.generated.resources.library_filter_rating
import nuvio.composeapp.generated.resources.library_rated_all_services
import nuvio.composeapp.generated.resources.library_rated_empty_message
import nuvio.composeapp.generated.resources.library_rated_empty_title
import nuvio.composeapp.generated.resources.library_rated_remove_rating
import nuvio.composeapp.generated.resources.library_rated_synced_section
import nuvio.composeapp.generated.resources.library_rated_title
import nuvio.composeapp.generated.resources.library_rating_any
import nuvio.composeapp.generated.resources.library_rating_min_stars
import org.jetbrains.compose.resources.stringResource

/** A synced-only rating (not matched to a local entry) plus its best-effort resolved metadata. */
private data class ResolvedSyncedRating(
    val rating: AggregatedUserRating,
    val item: LibraryItem?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryRatedScreen(
    onBack: () -> Unit,
    onPosterClick: (RatedLibraryEntry) -> Unit,
    onSyncedPosterClick: (LibraryItem) -> Unit,
) {
    val ratingsUiState by remember {
        LibraryRatingsRepository.ensureLoaded()
        LibraryRatingsRepository.uiState
    }.collectAsStateWithLifecycle()

    var minRating by rememberSaveable { mutableStateOf(0) }
    var providerFilterId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedProvider = remember(providerFilterId) { providerFilterId?.let(TrackingProviderId::fromStorage) }
    val anyRatingLabel = stringResource(Res.string.library_rating_any)
    val sortedEntries = remember(ratingsUiState, minRating) {
        ratingsUiState.entries.values
            .filter { minRating <= 0 || it.rating >= minRating }
            .sortedWith(
                compareByDescending<RatedLibraryEntry> { it.rating }.thenByDescending { it.ratedAtEpochMs },
            )
    }

    // Every title rated on a connected provider (Trakt/Simkl/MDBList), merged across providers.
    val connectedProviders by TrackingProviderRegistry.connectedProviderIds.collectAsStateWithLifecycle()
    var syncedRatings by remember { mutableStateOf<List<AggregatedUserRating>>(emptyList()) }
    var isLoadingSynced by remember { mutableStateOf(false) }
    LaunchedEffect(connectedProviders) {
        if (connectedProviders.isEmpty()) {
            syncedRatings = emptyList()
            return@LaunchedEffect
        }
        isLoadingSynced = true
        syncedRatings = runCatching { UserRatingsRepository.loadAllTitleRatings() }.getOrDefault(emptyList())
        isLoadingSynced = false
    }
    val coroutineScope = rememberCoroutineScope()
    val refreshSyncedRatings: () -> Unit = {
        if (connectedProviders.isNotEmpty()) {
            coroutineScope.launch {
                // maxAgeMs = 0 skips the repository's own cache so a manual refresh actually hits
                // the network instead of just replaying whatever the last fetch already saw.
                syncedRatings = runCatching { UserRatingsRepository.loadAllTitleRatings(maxAgeMs = 0L) }
                    .getOrDefault(syncedRatings)
            }
        }
    }

    // Best-effort cross-reference by IMDb id, cache-only (no network) so this stays cheap.
    val localImdbIds = remember(sortedEntries) {
        sortedEntries.mapNotNull { entry ->
            MetaDetailsRepository.peek(entry.type, entry.id)
                ?.imdbId
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.lowercase()
                ?.let { entry.id to it }
        }.toMap()
    }
    val syncedByLocalId = remember(syncedRatings, localImdbIds) {
        if (syncedRatings.isEmpty() || localImdbIds.isEmpty()) {
            emptyMap()
        } else {
            localImdbIds.mapNotNull { (localId, imdb) ->
                syncedRatings.firstOrNull { it.ids.imdb?.trim()?.lowercase() == imdb }?.let { localId to it }
            }.toMap()
        }
    }
    val syncedOnly = remember(syncedRatings, syncedByLocalId) {
        val matched = syncedByLocalId.values.toSet()
        syncedRatings.filterNot { it in matched }
    }
    // A provider filter only makes sense against titles actually rated on that provider, so it
    // drops local-only entries entirely rather than just dimming them.
    val displayedLocalEntries = remember(sortedEntries, syncedByLocalId, selectedProvider) {
        if (selectedProvider == null) {
            sortedEntries
        } else {
            sortedEntries.filter { entry ->
                syncedByLocalId[entry.id]?.ratingsByProvider?.containsKey(selectedProvider) == true
            }
        }
    }

    // Resolve poster/name for synced-only titles via the same catalog lookup details screens use.
    var allResolvedSyncedOnly by remember { mutableStateOf<List<ResolvedSyncedRating>>(emptyList()) }
    var isResolvingSyncedMetadata by remember { mutableStateOf(false) }
    LaunchedEffect(syncedOnly) {
        if (syncedOnly.isEmpty()) {
            allResolvedSyncedOnly = emptyList()
            return@LaunchedEffect
        }
        isResolvingSyncedMetadata = true
        allResolvedSyncedOnly = coroutineScope {
            syncedOnly.map { rating -> async { ResolvedSyncedRating(rating, rating.resolveLibraryItem()) } }.awaitAll()
        }
        isResolvingSyncedMetadata = false
    }
    // The star filter is on the same 1-5 scale everywhere: a synced title's best provider rating
    // (1-10) is converted (7 -> 4★, 8 -> 4★, ...) before it's compared against it.
    val resolvedSyncedOnly = remember(allResolvedSyncedOnly, minRating, selectedProvider) {
        allResolvedSyncedOnly.filter { resolved ->
            (minRating <= 0 || resolved.rating.bestFiveStarRating() >= minRating) &&
                (selectedProvider == null || resolved.rating.ratingsByProvider.containsKey(selectedProvider))
        }
    }
    val hasAnyRatings = ratingsUiState.entries.isNotEmpty() || syncedRatings.isNotEmpty() || isLoadingSynced
    var removeRatingTarget by remember { mutableStateOf<RatedLibraryEntry?>(null) }

    val tokens = MaterialTheme.nuvio
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = remember(maxWidth) {
            posterGridColumnCountForWidth(maxWidth - tokens.spacing.screenHorizontal * 2)
        }

        NuvioScreen(
            modifier = Modifier.fillMaxSize(),
        ) {
            stickyHeader {
                NuvioScreenHeader(
                    title = stringResource(Res.string.library_rated_title),
                    onBack = onBack,
                )
            }

            item(key = "rated-filter") {
                val ratingOptions = remember {
                    buildList {
                        add(NuvioDropdownOption(key = "0", label = anyRatingLabel))
                        for (stars in LibraryRatingMax downTo 1) {
                            add(NuvioDropdownOption(key = stars.toString(), label = "$stars+ ★"))
                        }
                    }
                }
                val allServicesLabel = stringResource(Res.string.library_rated_all_services)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NuvioDropdownChip(
                            title = stringResource(Res.string.library_filter_rating),
                            label = if (minRating <= 0) anyRatingLabel else stringResource(Res.string.library_rating_min_stars, minRating),
                            selectedKey = minRating.toString(),
                            options = ratingOptions,
                            onSelected = { option -> minRating = option.key.toIntOrNull() ?: 0 },
                        )
                        if (connectedProviders.isNotEmpty()) {
                            val providerOptions = remember(connectedProviders) {
                                buildList {
                                    add(NuvioDropdownOption(key = "all", label = allServicesLabel))
                                    connectedProviders.sortedBy { it.ordinal }.forEach { provider ->
                                        add(NuvioDropdownOption(key = provider.storageId, label = provider.displayName))
                                    }
                                }
                            }
                            NuvioDropdownChip(
                                title = stringResource(Res.string.library_filter_provider),
                                label = selectedProvider?.displayName ?: allServicesLabel,
                                selectedKey = providerFilterId ?: "all",
                                options = providerOptions,
                                onSelected = { option -> providerFilterId = option.key.takeIf { it != "all" } },
                            )
                        }
                    }
                    if (connectedProviders.isNotEmpty()) {
                        IconButton(onClick = refreshSyncedRatings) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(Res.string.cloud_library_refresh),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (sortedEntries.isEmpty() && !hasAnyRatings) {
                item(key = "rated-empty") {
                    HomeEmptyStateCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        title = stringResource(Res.string.library_rated_empty_title),
                        message = stringResource(Res.string.library_rated_empty_message),
                    )
                }
            } else if (displayedLocalEntries.isNotEmpty()) {
                items(
                    items = displayedLocalEntries.chunked(columns),
                    key = { rowEntries -> "rated:${rowEntries.first().type}:${rowEntries.first().id}" },
                ) { rowEntries ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowEntries.forEach { entry ->
                            RatedPosterTile(
                                entry = entry,
                                syncedProviders = syncedByLocalId[entry.id]?.ratingsByProvider.orEmpty(),
                                modifier = Modifier.weight(1f),
                                onClick = { onPosterClick(entry) },
                                onLongClick = { removeRatingTarget = entry },
                            )
                        }
                        repeat(columns - rowEntries.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            val isSyncedSectionSettling = isLoadingSynced || isResolvingSyncedMetadata
            if (isSyncedSectionSettling || resolvedSyncedOnly.isNotEmpty()) {
                item(key = "synced-header") {
                    Text(
                        text = stringResource(Res.string.library_rated_synced_section),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                if (isSyncedSectionSettling) {
                    items(
                        count = 2,
                        key = { index -> "synced-skeleton:$index" },
                    ) {
                        PosterGridSkeletonRow(columns = columns)
                    }
                } else {
                    items(
                        items = resolvedSyncedOnly.chunked(columns),
                        key = { rowEntries -> "synced:${rowEntries.first().rating.ids}:${rowEntries.first().rating.scope}" },
                    ) { rowEntries ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            rowEntries.forEach { resolved ->
                                SyncedRatedPosterTile(
                                    resolved = resolved,
                                    modifier = Modifier.weight(1f),
                                    onClick = resolved.item?.let { item -> { onSyncedPosterClick(item) } },
                                )
                            }
                            repeat(columns - rowEntries.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item(key = "rated-bottom-spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        LocalRatingRemoveSheet(
            target = removeRatingTarget,
            onDismiss = { removeRatingTarget = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalRatingRemoveSheet(
    target: RatedLibraryEntry?,
    onDismiss: () -> Unit,
) {
    if (target == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    NuvioModalBottomSheet(
        onDismissRequest = {
            coroutineScope.launch {
                dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
            }
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = nuvioSafeBottomPadding(16.dp)),
        ) {
            Text(
                text = target.name,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            NuvioBottomSheetDivider()
            NuvioBottomSheetActionRow(
                icon = Icons.Rounded.StarBorder,
                title = stringResource(Res.string.library_rated_remove_rating),
                onClick = {
                    LibraryRatingsRepository.setRating(target.toMetaPreview(), 0)
                    coroutineScope.launch {
                        dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                    }
                },
            )
        }
    }
}

private fun AggregatedUserRating.bestFiveStarRating(): Int = bestFiveStarRating(ratingsByProvider)

private suspend fun AggregatedUserRating.resolveLibraryItem(): LibraryItem? {
    val imdb = ids.imdb?.trim()?.takeIf { it.startsWith("tt", ignoreCase = true) } ?: return null
    val type = if (scope == TrackingRatingScope.MOVIE) "movie" else "series"
    val details = MetaDetailsRepository.fetch(type = type, id = imdb) ?: return null
    return LibraryItem(
        id = details.id,
        type = details.type,
        name = details.name,
        poster = details.poster,
        posterShape = PosterShape.Poster,
        releaseInfo = details.releaseInfo,
        savedAtEpochMs = 0L,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RatedPosterTile(
    entry: RatedLibraryEntry,
    syncedProviders: Map<TrackingProviderId, Int>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (entry.posterShape == PosterShape.Landscape) 1.78f else 0.68f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                ),
        ) {
            if (entry.poster != null) {
                AsyncImage(
                    model = entry.poster,
                    contentDescription = entry.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Text(
            text = entry.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = entry.rating.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (syncedProviders.isNotEmpty()) {
            ProviderBadgeRow(syncedProviders)
        }
    }
}

@Composable
private fun SyncedRatedPosterTile(
    resolved: ResolvedSyncedRating,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?,
) {
    val item = resolved.item
    val title = item?.name ?: resolved.rating.title.orEmpty()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .let { base -> onClick?.let { base.clickable(onClick = it) } ?: base },
            contentAlignment = Alignment.Center,
        ) {
            if (item?.poster != null) {
                AsyncImage(
                    model = item.poster,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else if (title.isNotBlank()) {
                Text(
                    text = title.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        ProviderBadgeRow(resolved.rating.ratingsByProvider)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderBadgeRow(ratingsByProvider: Map<TrackingProviderId, Int>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ratingsByProvider.entries.sortedBy { it.key.ordinal }.forEach { (providerId, rating) ->
            CompactProviderBadge(providerId = providerId, rating = rating)
        }
    }
}

@Composable
private fun CompactProviderBadge(providerId: TrackingProviderId, rating: Int) {
    Text(
        text = "${providerId.displayName} · $rating",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}
