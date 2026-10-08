package com.nuvio.app.features.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.FolderOff
import com.nuvio.app.features.home.components.posterGridColumnCountForWidth
import androidx.compose.foundation.lazy.itemsIndexed
import com.nuvio.app.core.ui.StaggeredEntrance
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.flow.filterIsInstance
import com.nuvio.app.core.ui.NuvioEmptyState
import com.nuvio.app.features.search.SearchBar
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Constraints
import com.nuvio.app.core.ui.rememberPinnedHeaderFiller
import com.nuvio.app.core.ui.pinnedHeaderFiller
import com.nuvio.app.core.ui.NativeTabBridge
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.roundToInt
import com.nuvio.app.core.ui.nuvioNoTopOverscroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ViewAgenda
import com.nuvio.app.core.ui.DisintegratingContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.stopScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.i18n.localizedMonthName
import com.nuvio.app.core.i18n.localizedShortMonthName
import com.nuvio.app.core.ui.ScreenActivityEffect
import com.nuvio.app.core.i18n.localizedByteUnit
import com.nuvio.app.core.format.resolveReleaseInfoForDisplay
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.core.network.NetworkStatusRepository
import com.nuvio.app.core.ui.DisintegrationRequest
import com.nuvio.app.core.ui.GlassIconButtonGroup
import com.nuvio.app.navigation.LocalUseNativeNavigation
import com.nuvio.app.core.ui.NuvioDropdownChip
import com.nuvio.app.core.ui.NuvioDropdownOption
import com.nuvio.app.core.ui.NuvioNetworkOfflineCard
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.shimmer
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.NuvioShelfSection
import com.nuvio.app.core.ui.NuvioViewAllPillSize
import com.nuvio.app.core.ui.ScopedDisintegrationTracker
import com.nuvio.app.core.ui.SkeletonBlock
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioConsumePointerEvents
import com.nuvio.app.features.cloud.CloudLibraryFile
import com.nuvio.app.features.cloud.CloudLibraryItem
import com.nuvio.app.features.cloud.CloudLibraryItemType
import com.nuvio.app.features.cloud.CloudLibraryRepository
import com.nuvio.app.features.cloud.CloudLibraryUiState
import com.nuvio.app.features.debrid.DebridSettingsRepository
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import com.nuvio.app.features.home.components.HomePosterCard
import com.nuvio.app.features.home.components.HomeSkeletonRow
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.ratings.AggregatedUserRating
import com.nuvio.app.features.ratings.UserRatingsRepository
import com.nuvio.app.features.tracking.TrackingProviderRegistry
import com.nuvio.app.features.tracking.TrackingRefreshIntent
import com.nuvio.app.features.watched.WatchedClock
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watchprogress.CurrentDateProvider
import com.nuvio.app.features.watching.application.WatchingState
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.addons.AddonsUiState
import com.nuvio.app.features.addons.enabledAddons
import com.nuvio.app.features.addons.hasPendingEnabledManifests
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    scrollToTopRequests: Flow<Unit> = emptyFlow(),
    onPosterClick: ((LibraryItem) -> Unit)? = null,
    onCalendarEpisodeClick: ((LibraryItem, Int?, Int?) -> Unit)? = null,
    onPosterLongClick: ((LibraryItem, LibrarySection) -> Unit)? = null,
    onSectionViewAllClick: ((LibrarySection, LibrarySortOption) -> Unit)? = null,
    onCloudFilePlay: ((CloudLibraryItem, CloudLibraryFile) -> Unit)? = null,
    onConnectCloudClick: (() -> Unit)? = null,
    disintegrationRequest: DisintegrationRequest<String>? = null,
    onRatedClick: (() -> Unit)? = null,
    onDownloadsClick: (() -> Unit)? = null,
) {
    val uiState by remember {
        LibraryRepository.ensureLoaded()
        LibraryRepository.uiState
    }.collectAsStateWithLifecycle()
    // Opening a poster acknowledges it — hide its "recently added" dot going forward.
    val wrappedOnPosterClick = onPosterClick?.let { callback ->
        { item: LibraryItem ->
            LibraryRepository.markPosterOpened(item)
            callback(item)
        }
    }
    val cloudUiState by CloudLibraryRepository.uiState.collectAsStateWithLifecycle()
    val cloudSettings by remember {
        DebridSettingsRepository.ensureLoaded()
        DebridSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val displaySettings by remember {
        LibraryDisplaySettingsRepository.ensureLoaded()
        LibraryDisplaySettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val networkStatusUiState by NetworkStatusRepository.uiState.collectAsStateWithLifecycle()
    val unknownReleaseLabel = stringResource(Res.string.generic_unknown)
    var observedOfflineState by remember { mutableStateOf(false) }
    var hydratedReleaseInfo by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var sourceModeName by rememberSaveable { mutableStateOf(LibraryViewMode.Saved.name) }
    val sourceMode = remember(sourceModeName) {
        runCatching { LibraryViewMode.valueOf(sourceModeName) }.getOrDefault(LibraryViewMode.Saved)
    }
    var showReleaseCalendar by rememberSaveable { mutableStateOf(false) }
    val releaseCalendarCacheState by LibraryReleaseCalendarCache.state.collectAsStateWithLifecycle()
    val releaseCalendarCacheKey = remember(uiState.items) {
        LibraryReleaseCalendarCache.cacheKeyFor(uiState.items)
    }
    val fallbackCalendarEvents = remember(uiState.items) {
        buildLibraryReleaseCalendarFallbackEvents(uiState.items)
    }
    val releaseCalendarEvents = if (releaseCalendarCacheState.cacheKey == releaseCalendarCacheKey) {
        releaseCalendarCacheState.events
    } else {
        fallbackCalendarEvents
    }
    val releaseCalendarLoading =
        releaseCalendarCacheState.cacheKey == releaseCalendarCacheKey && releaseCalendarCacheState.isWarming
    val releaseCalendarFailedSeriesCount =
        if (releaseCalendarCacheState.cacheKey == releaseCalendarCacheKey) {
            releaseCalendarCacheState.failedSeriesKeys.size
        } else {
            0
        }
    var selectedProviderId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTypeName by rememberSaveable { mutableStateOf<String?>(null) }
    var cloudSearchQuery by rememberSaveable { mutableStateOf("") }
    val selectedType = remember(selectedTypeName) {
        selectedTypeName?.let { runCatching { CloudLibraryItemType.valueOf(it) }.getOrNull() }
    }
    var selectedCloudItemKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLibrarySectionKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLibraryType by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMinRating by rememberSaveable { mutableStateOf(0) }
    val libraryRatingsUiState by remember {
        LibraryRatingsRepository.ensureLoaded()
        LibraryRatingsRepository.uiState
    }.collectAsStateWithLifecycle()
    // The rating filter also has to see Trakt/Simkl/MDBList ratings, not just the local ones:
    // since the details screen dropped its own rating picker in favor of the synced one, most new
    // ratings never touch LibraryRatingsRepository at all.
    val connectedRatingProviders by TrackingProviderRegistry.connectedProviderIds.collectAsStateWithLifecycle()
    var syncedRatingsForFilter by remember { mutableStateOf<List<AggregatedUserRating>>(emptyList()) }
    LaunchedEffect(connectedRatingProviders) {
        syncedRatingsForFilter = if (connectedRatingProviders.isEmpty()) {
            emptyList()
        } else {
            runCatching { UserRatingsRepository.loadAllTitleRatings() }.getOrDefault(emptyList())
        }
    }
    val syncedRatingsByImdbId = remember(syncedRatingsForFilter) {
        syncedRatingsForFilter.mapNotNull { rating ->
            rating.ids.imdb?.trim()?.lowercase()?.takeIf(String::isNotEmpty)?.let { it to rating }
        }.toMap()
    }
    val ratingFor = remember(libraryRatingsUiState, syncedRatingsByImdbId) {
        { item: LibraryItem ->
            val localRating = LibraryRatingsRepository.ratingFor(item.id, item.type)
            val syncedRating = if (syncedRatingsByImdbId.isEmpty()) {
                0
            } else {
                MetaDetailsRepository.peek(item.type, item.id)
                    ?.imdbId?.trim()?.lowercase()?.takeIf(String::isNotEmpty)
                    ?.let(syncedRatingsByImdbId::get)
                    ?.let { bestFiveStarRating(it.ratingsByProvider) }
                    ?: 0
            }
            maxOf(localRating, syncedRating)
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    ScreenActivityEffect(listState) { screenActive ->
        if (!screenActive) listState.stopScroll()
    }
    val isRemoteSource = uiState.sourceMode != LibrarySourceMode.LOCAL
    val effectiveSortOption = effectiveLibrarySortOption(
        selected = displaySettings.sortOption,
        sourceMode = uiState.sourceMode,
    )
    val orderListKeys = if (sourceMode != LibraryViewMode.Saved) emptyList() else {
        if (displaySettings.layoutMode == LibraryLayoutMode.HORIZONTAL) uiState.sections.map { it.type }
        else listOfNotNull(uiState.sections.firstOrNull { it.type == selectedLibrarySectionKey }?.type
            ?: uiState.sections.firstOrNull()?.type)
    }
    val providerOrders = rememberLibraryProviderOrders(uiState.sourceMode, orderListKeys, effectiveSortOption)
    val visibleSortOption = if (providerOrders.failed) LibrarySortOption.DEFAULT else effectiveSortOption
    val sortedSections = remember(uiState.sections, displaySettings, uiState.sourceMode, sourceMode, selectedMinRating, libraryRatingsUiState, providerOrders) {
        if (sourceMode == LibraryViewMode.Saved && displaySettings.layoutMode == LibraryLayoutMode.HORIZONTAL) {
            val ratingFiltered = if (selectedMinRating <= 0) {
                uiState.sections
            } else {
                uiState.sections
                    .map { section -> section.copy(items = section.items.filter { ratingFor(it) >= selectedMinRating }) }
                    .filter { section -> section.items.isNotEmpty() }
            }
            sortLibrarySections(
                sections = ratingFiltered,
                selected = visibleSortOption,
                sourceMode = uiState.sourceMode,
                providerOrders = providerOrders.ranks,
            )
        } else {
            emptyList()
        }
    }
    val verticalProjection = remember(
        uiState.sections,
        uiState.sourceMode,
        selectedLibrarySectionKey,
        selectedLibraryType,
        selectedMinRating,
        libraryRatingsUiState,
        displaySettings,
        sourceMode,
        providerOrders,
    ) {
        if (sourceMode == LibraryViewMode.Saved && displaySettings.layoutMode == LibraryLayoutMode.VERTICAL) {
            buildLibraryVerticalProjection(
                sections = uiState.sections,
                sourceMode = uiState.sourceMode,
                selectedSectionKey = selectedLibrarySectionKey,
                selectedType = selectedLibraryType,
                sortOption = visibleSortOption,
                minRating = selectedMinRating,
                ratingFor = ratingFor,
                providerOrders = providerOrders.ranks,
            )
        } else {
            LibraryVerticalProjection(
                availableSections = emptyList(),
                selectedSectionKey = null,
                availableTypes = emptyList(),
                selectedType = null,
                entries = emptyList(),
            )
        }
    }
    val releaseInfoFor = remember(hydratedReleaseInfo, unknownReleaseLabel) {
        { item: LibraryItem ->
            resolveReleaseInfoForDisplay(
                stored = item.releaseInfo,
                hydrated = hydratedReleaseInfo[item.libraryReleaseInfoKey()],
                fallback = unknownReleaseLabel,
            )
        }
    }
    val retryLibraryLoad: () -> Unit = {
        NetworkStatusRepository.requestRefresh(force = true)
        coroutineScope.launch {
            LibraryRepository.pullFromServer(
                profileId = ProfileRepository.activeProfileId,
                refreshIntent = TrackingRefreshIntent.USER_INITIATED,
            )
        }
    }

    ScreenActivityEffect(networkStatusUiState.condition, isRemoteSource) { screenActive ->
        if (!screenActive) return@ScreenActivityEffect
        when (networkStatusUiState.condition) {
            NetworkCondition.NoInternet,
            NetworkCondition.ServersUnreachable,
            -> {
                observedOfflineState = true
            }

            NetworkCondition.Online -> {
                if (!observedOfflineState) return@ScreenActivityEffect
                observedOfflineState = false
                if (isRemoteSource) {
                    coroutineScope.launch {
                        LibraryRepository.pullFromServer(ProfileRepository.activeProfileId)
                    }
                }
            }

            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> Unit
        }
    }

    ScreenActivityEffect(scrollToTopRequests) { screenActive ->
        if (!screenActive) return@ScreenActivityEffect
        scrollToTopRequests.collect {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(sourceMode, uiState.items) {
        if (sourceMode == LibraryViewMode.Cloud) return@LaunchedEffect
        val missingItems = uiState.items
            .filter { item -> item.releaseInfo.isNullOrBlank() }
            .distinctBy(LibraryItem::libraryReleaseInfoKey)
        if (missingItems.isEmpty()) {
            hydratedReleaseInfo = emptyMap()
            return@LaunchedEffect
        }

        val resolved = mutableMapOf<String, String>()
        missingItems.chunked(4).forEach { chunk ->
            kotlinx.coroutines.coroutineScope {
                chunk.map { item ->
                    async {
                        val releaseInfo = MetaDetailsRepository.peek(item.type, item.id)
                            ?.releaseInfo
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: runCatching { MetaDetailsRepository.fetch(item.type, item.id) }
                                .getOrNull()
                                ?.releaseInfo
                                ?.trim()
                                ?.takeIf { it.isNotBlank() }
                        releaseInfo?.let { item.libraryReleaseInfoKey() to it }
                    }
                }.awaitAll().filterNotNull().forEach { (key, releaseInfo) ->
                    resolved[key] = releaseInfo
                }
            }
            hydratedReleaseInfo = resolved.toMap()
        }
    }

    ScreenActivityEffect(sourceMode, cloudSettings.cloudLibraryEnabled, cloudSettings.providerApiKeys) { screenActive ->
        if (screenActive && sourceMode == LibraryViewMode.Cloud) {
            CloudLibraryRepository.ensureLoaded()
            selectedCloudItemKey = null
        }
    }

    LaunchedEffect(uiState.items) {
        if (uiState.items.isNotEmpty()) {
            LibraryReleaseCalendarCache.warm(uiState.items)
        }
    }

    // The native capsule's calendar button (iOS) can't reach this screen's state directly.
    LaunchedEffect(Unit) {
        NativeTabBridge.libraryCalendarRequests.collect { showReleaseCalendar = true }
    }

    val disintegration = remember { LibraryDisintegrationHolder() }
    val librarySectionsDisplay = if (
        sourceMode != LibraryViewMode.Cloud &&
        displaySettings.layoutMode == LibraryLayoutMode.HORIZONTAL &&
        uiState.isLoaded &&
        sortedSections.isNotEmpty()
    ) {
        disintegration.sync(
            sourceMode = uiState.sourceMode,
            sections = sortedSections,
            previewLimit = LIBRARY_SECTION_PREVIEW_LIMIT,
            request = disintegrationRequest,
        )
    } else {
        disintegration.reset()
        emptyList()
    }

    // Like Search: the large title scrolls away with the list, while the source switch, its
    // actions and the saved-library filters ride in an overlay that follows their slot in the
    // list until it reaches the status bar, then pins there on a blurred, theme-tinted header.
    // The calendar/downloads/rated capsule stays put at the top trailing corner (natively on iOS
    // — see LibraryHeaderGlassButtons in ContentView.swift), so the switch row docks beside it as
    // it pins. The view controls (layout, list management) lead the filter row instead.
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerDensity = LocalDensity.current
    val statusBarTopPx = with(headerDensity) { statusBarTop.toPx() }
    val pinFadePx = with(headerDensity) { 16.dp.toPx() }
    val headerFadePx = with(headerDensity) { LibraryHeaderFadeHeight.toPx() }
    val headerHazeState = rememberHazeState()
    var headerHeightPx by remember { mutableIntStateOf(0) }
    // Where the header's slot in the list currently sits; -inf once it has scrolled off the top.
    val headerSlotTop by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val slot = layoutInfo.visibleItemsInfo.firstOrNull { it.key == LibraryHeaderSlotKey }
            when {
                slot != null -> (slot.offset - layoutInfo.viewportStartOffset).toFloat()
                listState.firstVisibleItemIndex > 0 -> Float.NEGATIVE_INFINITY
                else -> null
            }
        }
    }
    // Cloud's filters while browsing a connected library (not inside a title's file picker).
    val showCloudControls = sourceMode == LibraryViewMode.Cloud &&
        cloudUiState.isLoaded &&
        cloudUiState.isEnabled &&
        cloudUiState.hasConnectedProvider &&
        selectedCloudItemKey == null
    val cloudAvailableTypes = remember(cloudUiState.items, selectedProviderId) {
        cloudUiState.items
            .filter { item -> selectedProviderId == null || item.providerId == selectedProviderId }
            .map { item -> item.type }
            .distinct()
            .sortedBy { type -> type.ordinal }
    }
    val cloudEffectiveType = selectedType?.takeIf { type -> type in cloudAvailableTypes }
    // Same condition as the list's populated branch below, which is where these filters used to live.
    val showSavedControls = sourceMode == LibraryViewMode.Saved &&
        uiState.isLoaded &&
        uiState.sections.isNotEmpty()
    // Restarts whenever what the saved library shows changes, so its rows or sections stagger
    // in; ones reached later by scrolling just show.
    val libraryEntrance = remember(
        sourceMode,
        uiState.sourceMode,
        displaySettings.layoutMode,
        effectiveSortOption,
        selectedLibrarySectionKey,
        selectedLibraryType,
        selectedMinRating,
        uiState.isLoaded && uiState.sections.isNotEmpty(),
    ) { TimeSource.Monotonic.markNow() }

    // Like iOS, starting to drag the list puts the keyboard away (Cloud's search field).
    val keyboardFocusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(listState) {
        listState.interactionSource.interactions
            .filterIsInstance<DragInteraction.Start>()
            .collect {
                keyboardFocusManager.clearFocus()
                keyboard?.hide()
            }
    }

    // Short libraries (say, two rows) still scroll far enough for the header to pin.
    val headerFiller = rememberPinnedHeaderFiller(
        listState = listState,
        firstKey = LibraryTitleKey,
        slotKey = LibraryHeaderSlotKey,
        pinnedTopPx = statusBarTopPx,
    )
    val background = MaterialTheme.nuvio.colors.background

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            // Same rule as Search's grid, so both screens fit the same number of posters to a row.
            val gridColumns = remember(maxWidth) { posterGridColumnCountForWidth(maxWidth) }

            NuvioScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .nuvioNoTopOverscroll()
                    .hazeSource(state = headerHazeState),
                horizontalPadding = 0.dp,
                topPadding = 0.dp,
                listState = listState,
                autoHidesNativeTabBar = true,
            ) {
            item(key = LibraryTitleKey) {
                NuvioScreenHeader(
                    title = if (sourceMode == LibraryViewMode.Cloud) {
                        stringResource(Res.string.library_title)
                    } else {
                        when (uiState.sourceMode) {
                            LibrarySourceMode.LOCAL -> stringResource(Res.string.library_title)
                            LibrarySourceMode.TRAKT -> stringResource(Res.string.library_trakt_title)
                            LibrarySourceMode.SIMKL -> stringResource(Res.string.library_simkl_title)
                            LibrarySourceMode.MDBLIST -> stringResource(Res.string.library_mdblist_title)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    // A touch more than the bare status-bar inset — matching Search's header,
                    // which gets the same small top margin.
                    topPadding = statusBarTop + 8.dp,
                )
            }
            item(key = LibraryHeaderSlotKey) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(LocalDensity.current) { headerHeightPx.toDp() }),
                )
            }

            if (sourceMode == LibraryViewMode.Cloud) {
                cloudLibraryContent(
                    uiState = cloudUiState,
                    selectedProviderId = selectedProviderId,
                    selectedType = selectedType,
                    selectedCloudItemKey = selectedCloudItemKey,
                    searchQuery = cloudSearchQuery,
                    onSearchQueryChange = {
                        cloudSearchQuery = it
                        selectedCloudItemKey = null
                    },
                    onProviderSelected = {
                        selectedProviderId = it
                        selectedTypeName = null
                        selectedCloudItemKey = null
                    },
                    onTypeSelected = {
                        selectedTypeName = it?.name
                        selectedCloudItemKey = null
                    },
                    onItemSelected = { item ->
                        val playableFiles = item.playableFiles
                        when {
                            playableFiles.size == 1 -> onCloudFilePlay?.invoke(item, playableFiles.first())
                            playableFiles.size > 1 -> selectedCloudItemKey = item.stableKey
                        }
                    },
                    onFileSelected = { item, file -> onCloudFilePlay?.invoke(item, file) },
                    onBackToItems = { selectedCloudItemKey = null },
                    onRefresh = { CloudLibraryRepository.refresh() },
                    onConnectCloudClick = onConnectCloudClick,
                )
            } else {
                when {
                    !uiState.isLoaded || (uiState.isLoading && uiState.sections.isEmpty()) -> {
                        if (displaySettings.layoutMode == LibraryLayoutMode.VERTICAL) {
                            libraryVerticalSkeletonItems(gridColumns)
                        } else {
                            items(3) {
                                HomeSkeletonRow(
                                    horizontalPadding = 16.dp,
                                )
                            }
                        }
                    }

                    !uiState.errorMessage.isNullOrBlank() && uiState.sections.isEmpty() -> {
                        item {
                            if (networkStatusUiState.isOfflineLike) {
                                NuvioNetworkOfflineCard(
                                    condition = networkStatusUiState.condition,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    onRetry = retryLibraryLoad,
                                )
                            } else {
                                NuvioEmptyState(
                                    icon = Icons.Rounded.ErrorOutline,
                                    modifier = Modifier.animateItem(),
                                    title = when (uiState.sourceMode) {
                                        LibrarySourceMode.LOCAL -> stringResource(Res.string.library_load_failed)
                                        LibrarySourceMode.TRAKT -> stringResource(Res.string.library_trakt_load_failed)
                                        LibrarySourceMode.SIMKL -> stringResource(Res.string.library_simkl_load_failed)
                                        LibrarySourceMode.MDBLIST -> stringResource(Res.string.library_mdblist_load_failed)
                                    },
                                    message = uiState.errorMessage.orEmpty(),
                                    actionLabel = stringResource(Res.string.action_retry),
                                    onAction = retryLibraryLoad,
                                )
                            }
                        }
                    }

                    uiState.sections.isEmpty() -> {
                        item {
                            NuvioEmptyState(
                                icon = Icons.Rounded.VideoLibrary,
                                modifier = Modifier.animateItem(),
                                title = when (uiState.sourceMode) {
                                    LibrarySourceMode.LOCAL -> stringResource(Res.string.library_empty_title)
                                    LibrarySourceMode.TRAKT -> stringResource(Res.string.library_trakt_empty_title)
                                    LibrarySourceMode.SIMKL -> stringResource(Res.string.library_simkl_empty_title)
                                    LibrarySourceMode.MDBLIST -> stringResource(Res.string.library_mdblist_empty_title)
                                },
                                message = when (uiState.sourceMode) {
                                    LibrarySourceMode.LOCAL -> stringResource(Res.string.library_empty_message)
                                    LibrarySourceMode.TRAKT -> stringResource(Res.string.library_trakt_empty_message)
                                    LibrarySourceMode.SIMKL -> stringResource(Res.string.library_simkl_empty_message)
                                    LibrarySourceMode.MDBLIST -> stringResource(Res.string.library_mdblist_empty_message)
                                },
                            )
                        }
                    }

                    else -> {
                        when (displaySettings.layoutMode) {
                            LibraryLayoutMode.HORIZONTAL -> librarySections(
                                displaySections = librarySectionsDisplay,
                                releaseInfoFor = releaseInfoFor,
                                watchedKeys = watchedUiState.watchedKeys,
                                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                                sortOption = effectiveSortOption,
                                onPosterClick = wrappedOnPosterClick,
                                onSectionViewAllClick = onSectionViewAllClick,
                                onPosterLongClick = onPosterLongClick,
                                onDisintegrated = disintegration::onExited,
                                entranceBatch = libraryEntrance,
                            )
                            LibraryLayoutMode.VERTICAL -> libraryVerticalContent(
                                projection = verticalProjection,
                                columns = gridColumns,
                                releaseInfoFor = releaseInfoFor,
                                watchedKeys = watchedUiState.watchedKeys,
                                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                                onPosterClick = wrappedOnPosterClick,
                                onPosterLongClick = onPosterLongClick,
                                entranceBatch = libraryEntrance,
                            )
                        }
                    }
                }
            }
            pinnedHeaderFiller(headerFiller)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // Positioned in the placement phase, not a graphicsLayer block, which iOS
                    // stops re-running on scroll after a rotation.
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            val slotTop = headerSlotTop
                            // Parked off screen for the one frame before the list has placed the slot.
                            val y = if (slotTop == null) {
                                -placeable.height
                            } else {
                                (slotTop - statusBarTopPx).coerceAtLeast(0f).roundToInt()
                            }
                            placeable.place(0, y)
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        // Blur and tint thin out below the filters instead of ending on a hard edge.
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val fadeStart = ((size.height - headerFadePx) / size.height).coerceIn(0f, 1f)
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to Color.Black,
                                    fadeStart to Color.Black,
                                    1f to Color.Transparent,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                        .hazeEffect(state = headerHazeState) {
                            blurRadius = 24.dp
                            backgroundColor = background
                            tints = listOf(HazeTint(background.copy(alpha = 0.72f)))
                            noiseFactor = 0f
                            // Fades in over the last stretch before the header pins.
                            val distance = (headerSlotTop ?: Float.POSITIVE_INFINITY) - statusBarTopPx
                            alpha = (1f - distance / pinFadePx).coerceIn(0f, 1f)
                        },
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Room for the backdrop's fade, which stays touch-transparent.
                        .padding(bottom = LibraryHeaderFadeHeight)
                        // Drags that start on the header still scroll the list beneath it.
                        .scrollable(
                            state = listState,
                            orientation = Orientation.Vertical,
                            reverseDirection = true,
                        )
                        .padding(top = statusBarTop)
                        .onSizeChanged { headerHeightPx = it.height },
                ) {
                    LibrarySourceSwitch(
                        selectedMode = sourceMode,
                        onModeSelected = { mode ->
                            sourceModeName = mode.name
                        },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            // Centres the row on the capsule once pinned.
                            .padding(top = 6.dp),
                    )
                    if (showSavedControls) {
                        LibrarySavedControls(
                            layoutMode = displaySettings.layoutMode,
                            sourceMode = uiState.sourceMode,
                            sortOption = effectiveSortOption,
                            verticalProjection = verticalProjection,
                            minRating = selectedMinRating,
                            onSectionSelected = { sectionKey ->
                                selectedLibrarySectionKey = sectionKey
                                selectedLibraryType = null
                            },
                            onTypeSelected = { type -> selectedLibraryType = type },
                            onSortSelected = LibraryDisplaySettingsRepository::setSortOption,
                            onMinRatingSelected = { rating -> selectedMinRating = rating },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp, bottom = 14.dp),
                        ) {
                            LibraryListManagementButton()
                            val targetLayout = if (displaySettings.layoutMode == LibraryLayoutMode.HORIZONTAL) {
                                LibraryLayoutMode.VERTICAL
                            } else {
                                LibraryLayoutMode.HORIZONTAL
                            }
                            LibraryControlIconChip(
                                contentDescription = if (targetLayout == LibraryLayoutMode.VERTICAL) {
                                    stringResource(Res.string.library_layout_show_vertical)
                                } else {
                                    stringResource(Res.string.library_layout_show_horizontal)
                                },
                                onClick = { LibraryDisplaySettingsRepository.setLayoutMode(targetLayout) },
                            ) {
                                Crossfade(
                                    targetState = targetLayout,
                                    animationSpec = tween(durationMillis = 140),
                                    label = "libraryLayoutAction",
                                ) { animatedTargetLayout ->
                                    Icon(
                                        imageVector = if (animatedTargetLayout == LibraryLayoutMode.VERTICAL) {
                                            Icons.Rounded.GridView
                                        } else {
                                            Icons.Rounded.ViewAgenda
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.nuvio.colors.textPrimary,
                                    )
                                }
                            }
                        }
                    } else if (showCloudControls) {
                        // Cloud's service/type filters and search ride in the pinned header too,
                        // like Saved's filters, instead of scrolling away with the list.
                        CloudLibraryToolbar(
                            uiState = cloudUiState,
                            selectedProviderId = selectedProviderId,
                            selectedType = cloudEffectiveType,
                            availableTypes = cloudAvailableTypes,
                            onProviderSelected = {
                                selectedProviderId = it
                                selectedTypeName = null
                                selectedCloudItemKey = null
                            },
                            onTypeSelected = {
                                selectedTypeName = it?.name
                                selectedCloudItemKey = null
                            },
                            onRefresh = { CloudLibraryRepository.refresh() },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp),
                        )
                        CloudLibrarySearchField(
                            query = cloudSearchQuery,
                            onQueryChange = {
                                cloudSearchQuery = it
                                selectedCloudItemKey = null
                            },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 10.dp, bottom = 14.dp),
                        )
                    } else {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }

            // Native nav (iOS) renders these as a real Liquid Glass capsule instead.
            if (!LocalUseNativeNavigation.current) {
                val openRatedLabel = stringResource(Res.string.library_rated_open)
                GlassIconButtonGroup(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = statusBarTop + 8.dp, end = 16.dp),
                ) {
                    val openCalendarLabel = stringResource(Res.string.library_calendar_open)
                    IconButton(
                        onClick = { showReleaseCalendar = true },
                        modifier = Modifier.semantics { contentDescription = openCalendarLabel },
                    ) {
                        LibraryCalendarGlyph(
                            modifier = Modifier.size(19.dp),
                            tint = Color.White,
                            cutoutColor = MaterialTheme.colorScheme.background,
                        )
                    }
                    if (onDownloadsClick != null) {
                        LibraryDownloadsButton(onClick = onDownloadsClick)
                    }
                    IconButton(
                        onClick = { onRatedClick?.invoke() },
                        modifier = Modifier.semantics { contentDescription = openRatedLabel },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                            tint = Color.White,
                        )
                    }
                }
            }
    }

    if (showReleaseCalendar) {
        LibraryReleaseCalendarPanel(
            events = releaseCalendarEvents,
            isLoading = releaseCalendarLoading,
            onDismiss = { showReleaseCalendar = false },
            onPosterClick = wrappedOnPosterClick,
            onCalendarEpisodeClick = onCalendarEpisodeClick,
            onMonthRequested = { month ->
                coroutineScope.launch {
                    LibraryReleaseCalendarCache.ensureMonth(uiState.items, month.key)
                }
            },
            failedSeriesCount = releaseCalendarFailedSeriesCount,
            onRefresh = {
                coroutineScope.launch {
                    LibraryReleaseCalendarCache.forceRefresh(uiState.items)
                }
            },
            onRetryFailed = {
                coroutineScope.launch {
                    LibraryReleaseCalendarCache.retryFailed(uiState.items)
                }
            },
        )
    }
}

private const val LibraryTitleKey = "library_title"
private const val LibraryHeaderSlotKey = "library_header"
private val LibraryHeaderFadeHeight = 28.dp

private fun LazyListScope.cloudLibraryContent(
    uiState: CloudLibraryUiState,
    selectedProviderId: String?,
    selectedType: CloudLibraryItemType?,
    selectedCloudItemKey: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onProviderSelected: (String?) -> Unit,
    onTypeSelected: (CloudLibraryItemType?) -> Unit,
    onItemSelected: (CloudLibraryItem) -> Unit,
    onFileSelected: (CloudLibraryItem, CloudLibraryFile) -> Unit,
    onBackToItems: () -> Unit,
    onRefresh: () -> Unit,
    onConnectCloudClick: (() -> Unit)?,
) {
    when {
        !uiState.isLoaded -> {
            cloudLibrarySkeletonItems()
        }

        !uiState.isEnabled -> {
            item {
                NuvioEmptyState(
                    icon = Icons.Rounded.CloudOff,
                    modifier = Modifier.animateItem(),
                    title = stringResource(Res.string.cloud_library_disabled_title),
                    message = stringResource(Res.string.cloud_library_disabled_message),
                    actionLabel = stringResource(Res.string.cloud_library_disabled_action),
                    onAction = onConnectCloudClick,
                )
            }
        }

        !uiState.hasConnectedProvider -> {
            item {
                NuvioEmptyState(
                    icon = Icons.Rounded.Link,
                    modifier = Modifier.animateItem(),
                    title = stringResource(Res.string.cloud_library_connect_title),
                    message = stringResource(Res.string.cloud_library_connect_message),
                    actionLabel = stringResource(Res.string.cloud_library_connect_action),
                    onAction = onConnectCloudClick,
                )
            }
        }

        else -> {
            val providerItems = uiState.items
                .filter { item -> selectedProviderId == null || item.providerId == selectedProviderId }
            val availableTypes = providerItems
                .map { item -> item.type }
                .distinct()
                .sortedBy { type -> type.ordinal }
            val effectiveSelectedType = selectedType?.takeIf { type -> type in availableTypes }
            val typeFilteredItems = providerItems
                .filter { item -> effectiveSelectedType == null || item.type == effectiveSelectedType }
            val trimmedQuery = searchQuery.trim()
            val hasActiveFilter = selectedProviderId != null || effectiveSelectedType != null || trimmedQuery.isNotEmpty()
            val filteredItems = if (trimmedQuery.isEmpty()) {
                typeFilteredItems
            } else {
                typeFilteredItems.filter { item ->
                    item.name.contains(trimmedQuery, ignoreCase = true) ||
                        item.files.any { file -> file.name.contains(trimmedQuery, ignoreCase = true) }
                }
            }
            val selectedItem = filteredItems.firstOrNull { it.stableKey == selectedCloudItemKey }

            if (selectedItem != null) {
                item {
                    CloudLibraryFilePicker(
                        item = selectedItem,
                        onBack = onBackToItems,
                        onFileSelected = { file -> onFileSelected(selectedItem, file) },
                    )
                }
            } else {
                val visibleProviderStates = uiState.providers.filter { providerState ->
                    selectedProviderId == null || providerState.providerId == selectedProviderId
                }
                val failedProviderStates = visibleProviderStates.filter { providerState ->
                    !providerState.errorMessage.isNullOrBlank() && providerState.items.isEmpty()
                }
                failedProviderStates.forEach { providerState ->
                    item(key = "cloud-error-${providerState.providerId}") {
                        NuvioEmptyState(
                            icon = Icons.Rounded.ErrorOutline,
                            modifier = Modifier.animateItem(),
                            title = stringResource(Res.string.cloud_library_load_failed, providerState.providerName),
                            message = providerState.errorMessage.orEmpty(),
                            actionLabel = stringResource(Res.string.action_retry),
                            onAction = onRefresh,
                        )
                    }
                }

                if (uiState.isRefreshing && filteredItems.isEmpty()) {
                    cloudLibrarySkeletonItems()
                } else if (filteredItems.isEmpty() && failedProviderStates.isEmpty()) {
                    item {
                        NuvioEmptyState(
                            icon = if (hasActiveFilter) Icons.Rounded.SearchOff else Icons.Rounded.CloudQueue,
                            modifier = Modifier.animateItem(),
                            title = stringResource(
                                if (hasActiveFilter) {
                                    Res.string.cloud_library_no_matches_title
                                } else {
                                    Res.string.cloud_library_empty_title
                                },
                            ),
                            message = stringResource(
                                if (hasActiveFilter) {
                                    Res.string.cloud_library_no_matches_message
                                } else {
                                    Res.string.cloud_library_empty_message
                                },
                            ),
                            actionLabel = if (hasActiveFilter) null else stringResource(Res.string.action_retry),
                            onAction = if (hasActiveFilter) null else onRefresh,
                        )
                    }
                } else {
                    items(
                        items = filteredItems,
                        key = { item -> item.stableKey },
                    ) { item ->
                        CloudLibraryRow(
                            item = item,
                            onClick = { onItemSelected(item) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudLibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Search's capsule bar, so both search fields in the app look and behave alike.
    val focusRequester = remember { FocusRequester() }
    SearchBar(
        query = query,
        onQueryChange = onQueryChange,
        placeholder = stringResource(Res.string.cloud_library_search_label),
        focusRequester = focusRequester,
        onFocusChanged = {},
        modifier = modifier,
    )
}

private fun LazyListScope.cloudLibrarySkeletonItems() {
    item(key = "cloud-library-skeleton-toolbar") {
        CloudLibrarySkeletonToolbar(
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
    items(3) {
        CloudLibrarySkeletonRow()
    }
}

@Composable
private fun LibrarySourceSwitch(
    selectedMode: LibraryViewMode,
    onModeSelected: (LibraryViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        // An IconButton's height, which also centres it on the Liquid Glass capsule once pinned.
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LibrarySegmentedControl(
            labels = listOf(
                stringResource(Res.string.library_source_saved),
                stringResource(Res.string.library_source_cloud),
            ),
            selectedIndex = if (selectedMode == LibraryViewMode.Cloud) 1 else 0,
            onSelected = { index ->
                onModeSelected(if (index == 1) LibraryViewMode.Cloud else LibraryViewMode.Saved)
            },
        )
    }
}

/**
 * iOS' segmented control: equal segments in one capsule, with the selection a pill that slides
 * across to the tapped segment.
 */
@Composable
private fun LibrarySegmentedControl(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val haptics = LocalHapticFeedback.current
    val position by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
        label = "librarySegmentPosition",
    )
    val pillColor = tokens.colors.accent.copy(alpha = 0.24f).compositeOver(tokens.colors.surface)
    val pillBorder = tokens.colors.accent.copy(alpha = 0.5f)
    Box(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .width(IntrinsicSize.Max)
            .clip(CircleShape)
            .background(tokens.colors.surface)
            .padding(3.dp),
    ) {
        // The sliding pill, placed in the layout phase from the animated position.
        Box(
            modifier = Modifier
                .matchParentSize()
                .layout { measurable, constraints ->
                    // The capsule sizes itself by intrinsics, which measure with open-ended
                    // constraints; fixed infinite constraints would throw, so just pass through.
                    if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
                        val placeable = measurable.measure(constraints)
                        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
                    val segmentWidth = constraints.maxWidth / labels.size
                    val placeable = measurable.measure(Constraints.fixed(segmentWidth, constraints.maxHeight))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place((position * segmentWidth).roundToInt(), 0)
                    }
                }
                .clip(CircleShape)
                .background(pillColor)
                .border(1.dp, pillBorder, CircleShape),
        )
        Row {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (!selected) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onSelected(index)
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) tokens.colors.textPrimary else tokens.colors.textMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudLibraryToolbar(
    uiState: CloudLibraryUiState,
    selectedProviderId: String?,
    selectedType: CloudLibraryItemType?,
    availableTypes: List<CloudLibraryItemType>,
    onProviderSelected: (String?) -> Unit,
    onTypeSelected: (CloudLibraryItemType?) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val providerOptions = buildList {
        add(NuvioDropdownOption(key = "", label = stringResource(Res.string.cloud_library_provider_all)))
        addAll(
            uiState.providers.map { provider ->
                NuvioDropdownOption(
                    key = provider.providerId,
                    label = provider.providerName,
                )
            },
        )
    }
    val typeOptions = buildList {
        add(NuvioDropdownOption(key = "", label = stringResource(Res.string.cloud_library_type_all)))
        addAll(
            availableTypes.map { type ->
                NuvioDropdownOption(
                    key = type.name,
                    label = cloudLibraryTypeLabel(type),
                )
            },
        )
    }
    val selectedProviderName = uiState.providers
        .firstOrNull { provider -> provider.providerId == selectedProviderId }
        ?.providerName
        ?: stringResource(Res.string.cloud_library_provider_all)
    val selectedTypeLabel = selectedType?.let { type -> cloudLibraryTypeLabel(type) }
        ?: stringResource(Res.string.cloud_library_type_all)
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NuvioDropdownChip(
                    title = stringResource(Res.string.cloud_library_select_provider),
                    label = selectedProviderName,
                    selectedKey = selectedProviderId.orEmpty(),
                    options = providerOptions,
                    enabled = providerOptions.size > 1,
                    onSelected = { option ->
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onProviderSelected(option.key.ifBlank { null })
                    },
                    active = selectedProviderId != null,
                    onClear = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onProviderSelected(null)
                    },
                )
                NuvioDropdownChip(
                    title = stringResource(Res.string.cloud_library_select_type),
                    label = selectedTypeLabel,
                    selectedKey = selectedType?.name.orEmpty(),
                    options = typeOptions,
                    enabled = typeOptions.size > 1,
                    onSelected = { option ->
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        val type = option.key
                            .takeIf { it.isNotBlank() }
                            ?.let(CloudLibraryItemType::valueOf)
                        onTypeSelected(type)
                    },
                    active = selectedType != null,
                    onClear = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onTypeSelected(null)
                    },
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(Res.string.cloud_library_refresh),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CloudLibraryRow(
    item: CloudLibraryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val playableCount = item.playableFiles.size
    val title = remember(item.name) { cloudReleaseTitle(item.name) }
    val tags = remember(item.name) { cloudFileTags(item.name) }
    val downloading = item.progressFraction?.takeIf { it in 0f..0.999f }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(enabled = playableCount > 0, onClick = onClick),
        shape = tokens.shapes.compactCard,
        color = tokens.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = tokens.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // Quality tags, then service, size, file count and — only while it's
                    // still downloading — the status and progress.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        tags.forEach { tag -> CloudFileTag(tag) }
                        Text(
                            text = cloudLibraryMetaLine(item, downloading),
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (playableCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(tokens.colors.accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(Res.string.action_play),
                            tint = tokens.colors.accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            downloading?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun cloudLibraryMetaLine(item: CloudLibraryItem, downloading: Float?): String {
    val playableCount = item.playableFiles.size
    return listOfNotNull(
        item.providerName,
        item.sizeBytes?.let(::formatCloudBytes),
        when (playableCount) {
            0 -> stringResource(Res.string.cloud_library_no_playable_files)
            1 -> null
            else -> stringResource(Res.string.cloud_library_playable_file_count, playableCount)
        },
        downloading?.let { item.status?.toDisplayStatus() },
        downloading?.let { "${(it * 100f).toInt()}%" },
    ).joinToString(" · ")
}

/**
 * A readable title from a release name: separators become spaces and everything from the year
 * (or, without one, the first quality marker) on is dropped, the year kept in brackets —
 * `Doing.Life.2026.1080p.NF.WEB-DL` reads as `Doing Life (2026)`. Falls back to the raw name.
 */
internal fun cloudReleaseTitle(rawName: String): String {
    // Leading junk that's never the title: bracketed site or group tags ("【…www.site.com】",
    // "[YTS.MX]") and bare site names ("www.site.com - ").
    var name = rawName.trim()
    while (true) {
        val stripped = name
            .replace(LeadingReleaseTag, "")
            .replace(LeadingReleaseSite, "")
            .trim()
        if (stripped == name || stripped.isEmpty()) break
        name = stripped
    }
    // Drop a file extension ("mkv"), but not a trailing year that only looks like one.
    val extension = name.substringAfterLast('.', "")
    val withoutExtension = if (extension.length in 2..4 && extension.any(Char::isLetter) && extension.all(Char::isLetterOrDigit)) {
        name.substringBeforeLast('.')
    } else {
        name
    }
    val normalized = withoutExtension.replace(Regex("""[._]+"""), " ").replace(Regex("""\s+"""), " ").trim()
    Regex("""\b(19\d{2}|20\d{2})\b""").find(normalized)?.let { year ->
        val before = normalized.substring(0, year.range.first).trim().trimEnd('(', '[', '-').trim()
        if (before.isNotEmpty()) return "$before (${year.value})"
    }
    Regex("""\b(s\d{1,2}e\d{1,3}|2160p|1080p|720p|480p|4k|uhd|web[ -]?dl|webrip|bluray|hdtv)\b""", RegexOption.IGNORE_CASE)
        .find(normalized)?.let { marker ->
            val before = normalized.substring(0, marker.range.first).trim().trimEnd('-').trim()
            if (before.isNotEmpty()) return before
        }
    return normalized.ifEmpty { rawName }
}

private val LeadingReleaseTag = Regex("""^\s*(【[^】]*】|\[[^\]]*]|\([^)]*\)|\{[^}]*\})\s*[-–—:|]*\s*""")
private val LeadingReleaseSite = Regex(
    """^\s*(www\.)?[a-z0-9-]+\.(com|org|net|mx|me|to|io|cc|ws|xyz|tv|in|uk|co|ru|lol|fun)\b\s*[-–—:|]+\s*""",
    RegexOption.IGNORE_CASE,
)

@Composable
private fun CloudLibraryFilePicker(
    item: CloudLibraryItem,
    onBack: () -> Unit,
    onFileSelected: (CloudLibraryFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = MaterialTheme.nuvio.shapes.compactCard,
        color = MaterialTheme.nuvio.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(Res.string.action_back),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(Res.string.cloud_library_file_picker_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val files = item.playableFiles
            if (files.isEmpty()) {
                NuvioEmptyState(
                    icon = Icons.Rounded.FolderOff,
                    title = stringResource(Res.string.cloud_library_no_files_title),
                    message = stringResource(Res.string.cloud_library_no_files_message),
                )
            } else {
                // One list with hairlines between files, rather than a card per file.
                Column {
                    files.forEachIndexed { index, file ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp)
                                    .height(1.dp)
                                    .background(MaterialTheme.nuvio.colors.borderSubtle.copy(alpha = 0.5f)),
                            )
                        }
                        CloudLibraryFileRow(
                            file = file,
                            onClick = { onFileSelected(file) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudLibraryFileRow(
    file: CloudLibraryFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val tags = remember(file.name) { cloudFileTags(file.name) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val highlight by animateColorAsState(
        targetValue = if (pressed) tokens.colors.textPrimary.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(durationMillis = if (pressed) 0 else 260),
        label = "cloudFileRowHighlight",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(highlight)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // What tells two files of the same title apart: episode, resolution, HDR, codec, size.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                tags.forEach { tag -> CloudFileTag(tag) }
                file.sizeBytes?.let { size ->
                    Text(
                        text = formatCloudBytes(size),
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.colors.textMuted,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tokens.colors.accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = stringResource(Res.string.cloud_library_play_file),
                tint = tokens.colors.accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CloudFileTag(text: String) {
    val tokens = MaterialTheme.nuvio
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = tokens.colors.textPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(tokens.colors.textPrimary.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Episode, resolution, HDR flavour and codec, read from a release-style file name. */
internal fun cloudFileTags(fileName: String): List<String> {
    val name = fileName.lowercase()
    return buildList {
        Regex("""s(\d{1,2})[ ._-]?e(\d{1,3})""").find(name)?.let { match ->
            val season = match.groupValues[1].padStart(2, '0')
            val episode = match.groupValues[2].padStart(2, '0')
            add("S${season}E$episode")
        }
        when {
            Regex("""(2160p|\b4k\b|\buhd\b)""").containsMatchIn(name) -> add("4K")
            "1080p" in name -> add("1080p")
            "720p" in name -> add("720p")
            "480p" in name -> add("480p")
        }
        when {
            Regex("""(\bdv\b|dovi|dolby[ ._-]?vision)""").containsMatchIn(name) -> add("DV")
            Regex("""hdr10\+|hdr10plus""").containsMatchIn(name) -> add("HDR10+")
            "hdr" in name -> add("HDR")
        }
        when {
            Regex("""(x265|h[ ._]?265|hevc)""").containsMatchIn(name) -> add("HEVC")
            Regex("""(x264|h[ ._]?264|\bavc\b)""").containsMatchIn(name) -> add("H.264")
            "av1" in name -> add("AV1")
        }
    }
}

@Composable
private fun cloudLibraryTypeLabel(type: CloudLibraryItemType): String =
    when (type) {
        CloudLibraryItemType.Torrent -> stringResource(Res.string.cloud_library_type_torrents)
        CloudLibraryItemType.Usenet -> stringResource(Res.string.cloud_library_type_usenet)
        CloudLibraryItemType.WebDownload -> stringResource(Res.string.cloud_library_type_web)
        CloudLibraryItemType.File -> stringResource(Res.string.cloud_library_type_files)
    }

private fun formatCloudBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 ${localizedByteUnit("B")}"
    val kib = 1024.0
    val mib = kib * 1024.0
    val gib = mib * 1024.0
    val value = bytes.toDouble()
    return when {
        value >= gib -> "${((value / gib) * 10.0).toInt() / 10.0} ${localizedByteUnit("GB")}"
        value >= mib -> "${((value / mib) * 10.0).toInt() / 10.0} ${localizedByteUnit("MB")}"
        value >= kib -> "${((value / kib) * 10.0).toInt() / 10.0} ${localizedByteUnit("KB")}"
        else -> "$bytes ${localizedByteUnit("B")}"
    }
}

private fun String.toDisplayStatus(): String =
    replace('_', ' ')
        .lowercase()
        .replaceFirstChar { it.titlecase() }

@Composable
private fun CloudLibrarySkeletonToolbar(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkeletonBlock(width = 112.dp, height = 36.dp, cornerRadius = 12.dp)
            SkeletonBlock(width = 92.dp, height = 36.dp, cornerRadius = 12.dp)
        }
    }
}

@Composable
private fun CloudLibrarySkeletonRow(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = MaterialTheme.nuvio.shapes.compactCard,
        color = MaterialTheme.nuvio.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SkeletonBlock(
                        modifier = Modifier.fillMaxWidth(0.74f),
                        height = 18.dp,
                        cornerRadius = 6.dp,
                    )
                    SkeletonBlock(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        height = 14.dp,
                        cornerRadius = 6.dp,
                    )
                    SkeletonBlock(
                        modifier = Modifier.fillMaxWidth(0.52f),
                        height = 12.dp,
                        cornerRadius = 6.dp,
                    )
                }
                SkeletonBlock(width = 48.dp, height = 48.dp, cornerRadius = 24.dp)
            }
        }
    }
}

@Composable
private fun LibraryReleaseCalendarPanel(
    events: List<LibraryCalendarEvent>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onPosterClick: ((LibraryItem) -> Unit)?,
    onCalendarEpisodeClick: ((LibraryItem, Int?, Int?) -> Unit)?,
    onMonthRequested: (LibraryCalendarMonth) -> Unit,
    failedSeriesCount: Int = 0,
    onRefresh: (() -> Unit)? = null,
    onRetryFailed: (() -> Unit)? = null,
) {
    val today = remember { parseLibraryCalendarDate(CurrentDateProvider.todayIsoDate()) ?: LibraryCalendarDate(1970, 1, 1) }
    val todayIso = today.iso
    val initialMonth = remember { initialLibraryCalendarMonth() }
    var calendarSelection by remember(initialMonth, todayIso) {
        mutableStateOf(defaultLibraryCalendarSelection(events, initialMonth, todayIso))
    }
    var monthNavigationDirection by remember { mutableStateOf(1) }
    val visibleMonth = calendarSelection.month
    val selectedDateIso = calendarSelection.dateIso
    val monthEvents = remember(events, visibleMonth) {
        events
            .filter { event -> event.date.year == visibleMonth.year && event.date.month == visibleMonth.month }
            .sortedWith(compareBy<LibraryCalendarEvent> { it.date.iso }.thenBy { it.sortTitle.lowercase() })
    }
    val eventsByDate = remember(events) { events.groupBy { event -> event.date.iso } }

    val selectedEvents = eventsByDate[selectedDateIso].orEmpty()
    val handleEventClick: ((LibraryCalendarEvent) -> Unit)? = when {
        onCalendarEpisodeClick != null -> { event ->
            onDismiss()
            onCalendarEpisodeClick(event.item, event.seasonNumber, event.episodeNumber)
        }
        onPosterClick != null -> { event ->
            onDismiss()
            onPosterClick(event.item)
        }
        else -> null
    }
    val selectedDate = parseLibraryCalendarDate(selectedDateIso)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                )
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
        ) {
            val useHorizontalLayout = maxWidth >= 600.dp || maxWidth > maxHeight
            val panelHeight = if (useHorizontalLayout) {
                minOf(maxHeight, 500.dp)
            } else {
                minOf(maxHeight, 700.dp)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss),
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .height(panelHeight)
                    .clickable(onClick = {}),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 18.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                ) {
                    LibraryCalendarTopBar(
                        title = stringResource(Res.string.library_calendar_title),
                        subtitle = stringResource(Res.string.library_calendar_exact_dates_only),
                        onDismiss = onDismiss,
                        isRefreshing = isLoading,
                        onRefresh = onRefresh,
                    )
                    val retryFailed = onRetryFailed ?: onRefresh
                    if (failedSeriesCount > 0 && !isLoading && retryFailed != null) {
                        LibraryCalendarFailureBanner(
                            failedSeriesCount = failedSeriesCount,
                            onRetry = retryFailed,
                        )
                    }

                    when {
                        events.isEmpty() && isLoading -> LibraryCalendarLoadingState()
                        events.isEmpty() -> LibraryCalendarEmptyState()
                        useHorizontalLayout -> {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    item {
                                        AnimatedContent(
                                            targetState = visibleMonth,
                                            transitionSpec = {
                                                val direction = monthNavigationDirection
                                                (slideInHorizontally { width -> direction * width } togetherWith
                                                    slideOutHorizontally { width -> -direction * width })
                                            },
                                            label = "library-calendar-month",
                                        ) { animatedMonth ->
                                        LibraryCalendarCard(
                                            month = animatedMonth,
                                            monthEventCount = monthEvents.size,
                                            eventsByDate = eventsByDate,
                                            selectedDateIso = selectedDateIso,
                                            todayIso = todayIso,
                                            onPrevious = {
                                                monthNavigationDirection = -1
                                                onMonthRequested(visibleMonth.previous())
                                                calendarSelection = defaultLibraryCalendarSelection(
                                                    events = events,
                                                    month = visibleMonth.previous(),
                                                    todayIso = todayIso,
                                                )
                                            },
                                            onNext = {
                                                monthNavigationDirection = 1
                                                onMonthRequested(visibleMonth.next())
                                                calendarSelection = defaultLibraryCalendarSelection(
                                                    events = events,
                                                    month = visibleMonth.next(),
                                                    todayIso = todayIso,
                                                )
                                            },
                                            onToday = {
                                                calendarSelection = LibraryCalendarSelection(
                                                    month = LibraryCalendarMonth(today.year, today.month),
                                                    dateIso = todayIso,
                                                )
                                            },
                                            onDateSelected = { date ->
                                                calendarSelection = calendarSelection.copy(dateIso = date.iso)
                                            },
                                        )
                                        }
                                    }
                                }
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    libraryCalendarAgendaContent(
                                        selectedDate = selectedDate,
                                        selectedEvents = selectedEvents,
                                        todayIso = todayIso,
                                        isLoading = isLoading,
                                        onEventClick = handleEventClick,
                                    )
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            ) {
                                item {
                                    AnimatedContent(
                                        targetState = visibleMonth,
                                        transitionSpec = {
                                            val direction = monthNavigationDirection
                                            (slideInHorizontally { width -> direction * width } togetherWith
                                                slideOutHorizontally { width -> -direction * width })
                                        },
                                        label = "library-calendar-month",
                                    ) { animatedMonth ->
                                    LibraryCalendarCard(
                                        month = animatedMonth,
                                        monthEventCount = monthEvents.size,
                                        eventsByDate = eventsByDate,
                                        selectedDateIso = selectedDateIso,
                                        todayIso = todayIso,
                                        onPrevious = {
                                            monthNavigationDirection = -1
                                            onMonthRequested(visibleMonth.previous())
                                            calendarSelection = defaultLibraryCalendarSelection(
                                                events = events,
                                                month = visibleMonth.previous(),
                                                todayIso = todayIso,
                                            )
                                        },
                                        onNext = {
                                            monthNavigationDirection = 1
                                            onMonthRequested(visibleMonth.next())
                                            calendarSelection = defaultLibraryCalendarSelection(
                                                events = events,
                                                month = visibleMonth.next(),
                                                todayIso = todayIso,
                                            )
                                        },
                                        onToday = {
                                            calendarSelection = LibraryCalendarSelection(
                                                month = LibraryCalendarMonth(today.year, today.month),
                                                dateIso = todayIso,
                                            )
                                        },
                                        onDateSelected = { date ->
                                            calendarSelection = calendarSelection.copy(dateIso = date.iso)
                                        },
                                    )
                                    }
                                }
                                libraryCalendarAgendaContent(
                                    selectedDate = selectedDate,
                                    selectedEvents = selectedEvents,
                                    todayIso = todayIso,
                                    isLoading = isLoading,
                                    onEventClick = handleEventClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.libraryCalendarAgendaContent(
    selectedDate: LibraryCalendarDate?,
    selectedEvents: List<LibraryCalendarEvent>,
    todayIso: String,
    isLoading: Boolean,
    onEventClick: ((LibraryCalendarEvent) -> Unit)?,
) {
    item {
        Spacer(modifier = Modifier.height(4.dp))
        LibraryCalendarAgendaHeader(
            selectedDate = selectedDate,
            eventCount = selectedEvents.size,
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
    if (selectedEvents.isEmpty()) {
        item { LibraryCalendarNoDayEvents() }
    } else {
        items(items = selectedEvents, key = { event -> event.key }) { event ->
            LibraryCalendarEventRow(
                event = event,
                todayIso = todayIso,
                onClick = onEventClick?.let { eventClick -> { eventClick(event) } },
            )
        }
    }
    if (isLoading) {
        item { LibraryCalendarInlineLoading() }
    }
    item { Spacer(modifier = Modifier.height(12.dp)) }
}

@Composable
private fun LibraryCalendarTopBar(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 22.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onRefresh != null) {
            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier.size(40.dp),
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(Res.string.library_calendar_refresh),
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(Res.string.action_close),
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun LibraryCalendarFailureBanner(
    failedSeriesCount: Int,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (failedSeriesCount == 1) {
                    stringResource(Res.string.library_calendar_failed_series_single)
                } else {
                    stringResource(Res.string.library_calendar_failed_series, failedSeriesCount)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(Res.string.library_calendar_retry),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun LibraryCalendarGlyph(
    modifier: Modifier = Modifier,
    tint: Color,
    cutoutColor: Color,
) {
    Canvas(modifier = modifier) {
        val scale = size.minDimension / 14f
        fun x(value: Float) = value * scale
        fun y(value: Float) = value * scale

        drawRoundRect(
            color = tint,
            topLeft = Offset(x(1.5f), y(2.5f)),
            size = Size(x(11f), y(10f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(1.1f), y(1.1f)),
        )
        drawRect(
            color = cutoutColor,
            topLeft = Offset(x(2.6f), y(5.1f)),
            size = Size(x(8.8f), y(0.9f)),
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(x(3.5f), y(1.3f)),
            size = Size(x(1.5f), y(3.1f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(0.7f), y(0.7f)),
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(x(9f), y(1.3f)),
            size = Size(x(1.5f), y(3.1f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(0.7f), y(0.7f)),
        )

        val cell = x(1.15f)
        val gap = x(1.05f)
        val startX = x(4.1f)
        val startY = y(7.25f)
        repeat(3) { column ->
            repeat(2) { row ->
                drawRoundRect(
                    color = cutoutColor,
                    topLeft = Offset(startX + column * (cell + gap), startY + row * (cell + gap)),
                    size = Size(cell, cell),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(0.22f), y(0.22f)),
                )
            }
        }
    }
}


private fun LazyListScope.librarySections(
    displaySections: List<LibraryDisplaySection>,
    releaseInfoFor: (LibraryItem) -> String,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String>,
    sortOption: LibrarySortOption,
    onPosterClick: ((LibraryItem) -> Unit)?,
    onSectionViewAllClick: ((LibrarySection, LibrarySortOption) -> Unit)?,
    onPosterLongClick: ((LibraryItem, LibrarySection) -> Unit)?,
    onDisintegrated: (String) -> Unit,
    /** When the current filter's sections first appeared, for their staggered entrance. */
    entranceBatch: TimeMark? = null,
) {
    itemsIndexed(
        items = displaySections,
        key = { _, section -> "library-horizontal:${section.type}" },
    ) { sectionIndex, section ->
        Box(modifier = libraryContentTransitionModifier()) {
            StaggeredEntranceIfAny(batch = entranceBatch, index = sectionIndex) {
                NuvioShelfSection(
                    title = section.displayTitle,
                    entries = section.previewEntries,
                    headerHorizontalPadding = 16.dp,
                    rowContentPadding = PaddingValues(horizontal = 16.dp),
                    onViewAllClick = section.source
                        ?.takeIf { it.items.size > LIBRARY_SECTION_PREVIEW_LIMIT }
                        ?.let { source -> onSectionViewAllClick?.let { { it(source, sortOption) } } },
                    viewAllPillSize = NuvioViewAllPillSize.Compact,
                    key = { entry -> entry.globalKey },
                    animatePlacement = true,
                ) { entry ->
                    val item = entry.item
                    val posterItem = item.toMetaPreview().copy(releaseInfo = releaseInfoFor(item))
                    val entrySource = entry.section
                    DisintegratingContainer(
                        disintegrating = entry.exiting,
                        onDisintegrated = { onDisintegrated(entry.globalKey) },
                    ) {
                        HomePosterCard(
                            item = posterItem,
                            isWatched = WatchingState.isPosterWatched(
                                watchedKeys = watchedKeys,
                                item = posterItem,
                                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                            ),
                            isRecentlyAdded = item.isRecentlyAdded(),
                            onClick = if (entry.exiting) null else onPosterClick?.let { { it(item) } },
                            onLongClick = if (entry.exiting || entrySource == null) {
                                null
                            } else {
                                onPosterLongClick?.let { { it(item, entrySource) } }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** [StaggeredEntrance] when there's a batch to stagger from, otherwise just [content]. */
@Composable
private fun StaggeredEntranceIfAny(batch: TimeMark?, index: Int, content: @Composable () -> Unit) {
    if (batch != null) StaggeredEntrance(batch = batch, index = index, content = content) else content()
}

private fun LibraryItem.libraryReleaseInfoKey(): String = "${type.lowercase()}:$id"

@Composable
private fun LibraryCalendarLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
        )
        Text(
            text = stringResource(Res.string.library_calendar_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LibraryCalendarInlineLoading() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun LibraryCalendarEmptyState() {
    NuvioEmptyState(
        icon = Icons.Rounded.EventBusy,
        title = stringResource(Res.string.library_calendar_empty_title),
        message = stringResource(Res.string.library_calendar_empty_message),
    )
}

@Composable
private fun LibraryCalendarCard(
    month: LibraryCalendarMonth,
    monthEventCount: Int,
    eventsByDate: Map<String, List<LibraryCalendarEvent>>,
    selectedDateIso: String?,
    todayIso: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onDateSelected: (LibraryCalendarDate) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val previousMonth = {
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        onPrevious()
    }
    val nextMonth = {
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        onNext()
    }
    val swipeModifier = Modifier.pointerInput(month) {
        var totalDrag = 0f
        detectHorizontalDragGestures(
            onDragStart = { totalDrag = 0f },
            onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
            onDragEnd = {
                when {
                    totalDrag <= -48f -> nextMonth()
                    totalDrag >= 48f -> previousMonth()
                }
            },
            onDragCancel = { totalDrag = 0f },
        )
    }
    Surface(
        modifier = swipeModifier.fillMaxWidth(),
        color = MaterialTheme.nuvio.colors.surface,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.nuvio.colors.borderSubtle.copy(alpha = 0.48f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = previousMonth,
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = stringResource(Res.string.library_calendar_previous_month),
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                ) {
                    Text(
                        text = month.displayTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = libraryCalendarReleaseCountText(monthEventCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    modifier = Modifier.clickable(onClick = onToday),
                    color = MaterialTheme.nuvio.colors.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = stringResource(Res.string.library_calendar_today),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                IconButton(
                    onClick = nextMonth,
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = stringResource(Res.string.library_calendar_next_month),
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            LibraryCalendarWeekdayHeader()
            // Slides the new month in from the side it came from, like flipping a page.
            AnimatedContent(
                targetState = month,
                transitionSpec = {
                    val forward = (targetState.year * 12 + targetState.month) >
                        (initialState.year * 12 + initialState.month)
                    val direction = if (forward) 1 else -1
                    (slideInHorizontally(tween(260)) { it / 3 * direction } + fadeIn(tween(220)))
                        .togetherWith(slideOutHorizontally(tween(260)) { -it / 3 * direction } + fadeOut(tween(160)))
                },
                label = "libraryCalendarMonth",
            ) { shownMonth ->
                LibraryCalendarMonthGrid(
                    month = shownMonth,
                    eventsByDate = eventsByDate,
                    selectedDateIso = selectedDateIso,
                    todayIso = todayIso,
                    onDateSelected = onDateSelected,
                )
            }
        }
    }
}

@Composable
private fun LibraryCalendarWeekdayHeader() {
    val labels = listOf(
        stringResource(Res.string.library_calendar_weekday_sun),
        stringResource(Res.string.library_calendar_weekday_mon),
        stringResource(Res.string.library_calendar_weekday_tue),
        stringResource(Res.string.library_calendar_weekday_wed),
        stringResource(Res.string.library_calendar_weekday_thu),
        stringResource(Res.string.library_calendar_weekday_fri),
        stringResource(Res.string.library_calendar_weekday_sat),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LibraryCalendarMonthGrid(
    month: LibraryCalendarMonth,
    eventsByDate: Map<String, List<LibraryCalendarEvent>>,
    selectedDateIso: String?,
    todayIso: String,
    onDateSelected: (LibraryCalendarDate) -> Unit,
) {
    val cells = remember(month) { libraryCalendarCells(month) }
    val haptics = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                        )
                    } else {
                        val dayEvents = eventsByDate[date.iso].orEmpty()
                        val hasEvents = dayEvents.isNotEmpty()
                        val isSelected = selectedDateIso == date.iso
                        val isToday = todayIso == date.iso
                        // iOS calendar days: circles, today's number in the accent, the
                        // selected day filled with it, and a dot under any day with releases.
                        val accent = MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    if (!isSelected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    onDateSelected(date)
                                },
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) accent else Color.Transparent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = date.day.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> accent
                                        hasEvents -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = when {
                                        isSelected || isToday -> FontWeight.Bold
                                        hasEvents -> FontWeight.SemiBold
                                        else -> FontWeight.Normal
                                    },
                                )
                            }
                            if (hasEvents) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 2.dp)
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(accent),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryCalendarAgendaHeader(
    selectedDate: LibraryCalendarDate?,
    eventCount: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = stringResource(Res.string.library_calendar_agenda),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = selectedDate?.let(::displayLibraryCalendarEventDate).orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            color = MaterialTheme.nuvio.colors.surface,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(50),
        ) {
            Text(
                text = libraryCalendarReleaseCountText(eventCount),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun libraryCalendarReleaseCountText(count: Int): String =
    if (count == 1) {
        stringResource(Res.string.library_calendar_release_count_single)
    } else {
        stringResource(Res.string.library_calendar_release_count, count)
    }

@Composable
private fun LibraryCalendarNoDayEvents() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.nuvio.colors.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.nuvio.colors.borderSubtle.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(14.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    LibraryCalendarGlyph(
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        cutoutColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(Res.string.library_calendar_no_day_events_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.library_calendar_no_day_events_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LibraryCalendarEventRow(
    event: LibraryCalendarEvent,
    todayIso: String,
    onClick: (() -> Unit)?,
) {
    val isUpcoming = event.date.iso > todayIso
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }
    Surface(
        modifier = modifier.padding(bottom = 10.dp),
        color = MaterialTheme.nuvio.colors.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.nuvio.colors.borderSubtle.copy(alpha = 0.38f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LibraryCalendarEventArtwork(event = event)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                event.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(
                        if (isUpcoming) {
                            Res.string.library_calendar_upcoming
                        } else {
                            Res.string.library_calendar_available
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LibraryCalendarEventArtwork(event: LibraryCalendarEvent) {
    Box(
        modifier = Modifier
            .size(width = 104.dp, height = 62.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.nuvio.colors.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (!event.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = event.imageUrl,
                contentDescription = event.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(14.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${event.date.day} ${localizedShortMonthName(event.date.month)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private data class LibraryCalendarEvent(
    val key: String,
    val date: LibraryCalendarDate,
    val rawReleaseInfo: String,
    val item: LibraryItem,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val sortTitle: String = title,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
)

private data class LibraryCalendarSelection(
    val month: LibraryCalendarMonth,
    val dateIso: String,
)

private data class LibraryCalendarDate(
    val year: Int,
    val month: Int,
    val day: Int,
) {
    val iso: String = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

private data class LibraryCalendarMonth(
    val year: Int,
    val month: Int,
) {
    val key: String = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}"
    val displayTitle: String = "${localizedMonthName(month)} $year"

    fun previous(): LibraryCalendarMonth =
        if (month == 1) LibraryCalendarMonth(year - 1, 12) else copy(month = month - 1)

    fun next(): LibraryCalendarMonth =
        if (month == 12) LibraryCalendarMonth(year + 1, 1) else copy(month = month + 1)
}

private data class LibraryReleaseCalendarCacheState(
    val cacheKey: String? = null,
    val events: List<LibraryCalendarEvent> = emptyList(),
    val isWarming: Boolean = false,
    val isReady: Boolean = false,
    val loadedMonthKeys: Set<String> = emptySet(),
    val builtOnIsoDate: String? = null,
    val addonSignature: String? = null,
    val failedSeriesKeys: Set<String> = emptySet(),
)

private object LibraryReleaseCalendarCache {
    private val retryDelaysMs = longArrayOf(60_000L, 5L * 60_000L, 15L * 60_000L)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(LibraryReleaseCalendarCacheState())
    val state: StateFlow<LibraryReleaseCalendarCacheState> = _state.asStateFlow()

    private var lastItems: List<LibraryItem> = emptyList()
    private var warmJob: Job? = null
    private var warmJobKey: String? = null
    private var monthJob: Job? = null
    private var lastForcedRefreshAtEpochMs = 0L
    private var lastRetryAtEpochMs = 0L
    private val seriesRefetchedAtEpochMs = mutableMapOf<String, Long>()
    private var retryJob: Job? = null
    private var retryAttempt = 0
    private var addonWatcher: Job? = null

    fun cacheKeyFor(items: List<LibraryItem>): String {
        val nearbyMonths = libraryCalendarWarmMonthKeys().joinToString(separator = ",")
        val itemFingerprint = items.joinToString(separator = "|") { item ->
            "${item.type}:${item.id}:${item.releaseInfo.orEmpty()}"
        }
        return "${ProfileRepository.activeProfileId}:$nearbyMonths:$itemFingerprint"
    }

    suspend fun warm(items: List<LibraryItem>) {
        if (items.isEmpty()) return
        lastItems = items
        ensureAddonWatcher()
        requestWarm(items)?.join()
    }

    suspend fun forceRefresh(items: List<LibraryItem>) {
        if (items.isEmpty()) return
        lastItems = items
        ensureAddonWatcher()
        warmJob?.takeIf { it.isActive }?.let { running ->
            running.join()
            return
        }
        val now = WatchedClock.nowEpochMs()
        if (now - lastForcedRefreshAtEpochMs < LIBRARY_CALENDAR_FORCE_REFRESH_COOLDOWN_MS) return
        lastForcedRefreshAtEpochMs = now
        resetRetry()
        val cacheKey = cacheKeyFor(items)
        warmJobKey = cacheKey
        val job = scope.launch { runWarm(items, cacheKey, forceFull = true) }
        warmJob = job
        job.join()
    }

    suspend fun clearAndRebuild() {
        warmJob?.cancel()
        monthJob?.cancel()
        resetRetry()
        seriesRefetchedAtEpochMs.clear()
        lastForcedRefreshAtEpochMs = 0L
        lastRetryAtEpochMs = 0L
        _state.value = LibraryReleaseCalendarCacheState()
        withContext(Dispatchers.Default) { LibraryReleaseScheduleStorage.savePayload("") }
        lastItems.takeIf { it.isNotEmpty() }?.let { items -> requestWarm(items) }
    }

    suspend fun retryFailed(items: List<LibraryItem>) {
        if (items.isEmpty()) return
        lastItems = items
        val now = WatchedClock.nowEpochMs()
        if (now - lastRetryAtEpochMs < LIBRARY_CALENDAR_RETRY_COOLDOWN_MS) return
        lastRetryAtEpochMs = now
        resetRetry()
        requestWarm(items)?.join()
    }

    private fun shouldBypassMetaCacheOnForce(
        item: LibraryItem,
        failedSeriesKeys: Set<String>,
        seriesWithUpcomingEpisodes: Set<String>,
        now: Long,
    ): Boolean {
        val key = item.librarySeriesKey()
        if (key in failedSeriesKeys || key in seriesWithUpcomingEpisodes) return true
        val refetchedAt = seriesRefetchedAtEpochMs[key] ?: return true
        return now - refetchedAt >= LIBRARY_CALENDAR_FORCE_REUSE_WINDOW_MS
    }

    fun refreshIfStale() {
        val items = lastItems.takeIf { it.isNotEmpty() } ?: return
        requestWarm(items)
    }

    suspend fun ensureMonth(items: List<LibraryItem>, monthKey: String) {
        val cacheKey = cacheKeyFor(items)
        val current = _state.value
        if (current.cacheKey != cacheKey || monthKey in current.loadedMonthKeys) return
        if (warmJob?.isActive == true || monthJob?.isActive == true) return
        val job = scope.launch {
            _state.value = _state.value.copy(isWarming = true)
            try {
                val result = buildLibraryReleaseCalendarEvents(items, setOf(monthKey))
                val latest = _state.value
                if (latest.cacheKey == cacheKey) {
                    _state.value = latest.copy(
                        events = (latest.events + result.events).sortedLibraryCalendarEvents(),
                        // Leave the month unloaded when a series failed, so it is requested again.
                        loadedMonthKeys = if (result.failedSeriesKeys.isEmpty()) {
                            latest.loadedMonthKeys + monthKey
                        } else {
                            latest.loadedMonthKeys
                        },
                    )
                    persist(_state.value)
                }
            } finally {
                if (_state.value.isWarming) _state.value = _state.value.copy(isWarming = false)
            }
        }
        monthJob = job
        job.join()
    }

    private fun isFresh(state: LibraryReleaseCalendarCacheState, cacheKey: String, signature: String): Boolean =
        state.cacheKey == cacheKey &&
            state.isReady &&
            state.failedSeriesKeys.isEmpty() &&
            state.builtOnIsoDate == CurrentDateProvider.todayIsoDate() &&
            state.addonSignature == signature

    private fun requestWarm(items: List<LibraryItem>): Job? {
        val cacheKey = cacheKeyFor(items)
        if (isFresh(_state.value, cacheKey, currentLibraryAddonSignature(AddonRepository.uiState.value))) return null
        warmJob?.takeIf { job -> job.isActive && warmJobKey == cacheKey }?.let { return it }
        warmJob?.cancel()
        warmJobKey = cacheKey
        return scope.launch { runWarm(items, cacheKey) }.also { warmJob = it }
    }

    private suspend fun runWarm(items: List<LibraryItem>, cacheKey: String, forceFull: Boolean = false) {
        val today = CurrentDateProvider.todayIsoDate()
        val targetMonthKeys = libraryCalendarWarmMonthKeys()
        val inMemory = _state.value.takeIf { it.cacheKey == cacheKey }
        val persisted = if (inMemory == null) {
            withContext(Dispatchers.Default) { LibraryReleaseSchedulePersistence.load(items) }
        } else {
            null
        }

        val previousEvents = inMemory?.events ?: persisted?.events ?: _state.value.events
        _state.value = LibraryReleaseCalendarCacheState(
            cacheKey = cacheKey,
            events = (previousEvents.filter { event -> event.key.startsWith("episode:") } +
                buildLibraryReleaseCalendarFallbackEvents(items)).sortedLibraryCalendarEvents(),
            isWarming = true,
            loadedMonthKeys = inMemory?.loadedMonthKeys.orEmpty(),
            builtOnIsoDate = inMemory?.builtOnIsoDate,
            addonSignature = inMemory?.addonSignature,
            failedSeriesKeys = inMemory?.failedSeriesKeys.orEmpty(),
            isReady = inMemory?.isReady == true,
        )

        try {
            awaitLibraryAddonsSettled()
            val signature = currentLibraryAddonSignature(AddonRepository.uiState.value)
            val previousBuild = inMemory?.takeIf { it.isReady }
                ?: persisted?.takeIf { it.cacheKey == cacheKey }?.let { restored ->
                    LibraryReleaseCalendarCacheState(
                        cacheKey = cacheKey,
                        events = restored.events,
                        isReady = true,
                        loadedMonthKeys = restored.loadedMonthKeys,
                        builtOnIsoDate = restored.savedOnIsoDate,
                        addonSignature = restored.addonSignature,
                        failedSeriesKeys = restored.failedSeriesKeys,
                    )
                }

            if (!forceFull && previousBuild != null && isFresh(previousBuild, cacheKey, signature)) {
                _state.value = previousBuild
                resetRetry()
                return
            }

            val sameBuildContext = !forceFull &&
                previousBuild != null &&
                previousBuild.builtOnIsoDate == today &&
                previousBuild.addonSignature == signature &&
                previousBuild.loadedMonthKeys.containsAll(targetMonthKeys)
            val next = if (sameBuildContext && previousBuild != null) {
                val retried = previousBuild.failedSeriesKeys
                val result = buildLibraryReleaseCalendarEvents(items, targetMonthKeys, onlySeriesKeys = retried)
                val recovered = retried - result.failedSeriesKeys
                previousBuild.copy(
                    events = (previousBuild.events.filterNot { event ->
                        event.key.startsWith("episode:") && event.item.librarySeriesKey() in recovered
                    } + result.events).withoutSupersededFallbacks(targetMonthKeys),
                    failedSeriesKeys = result.failedSeriesKeys,
                )
            } else {
                val addonsChanged =
                    previousBuild?.addonSignature != null && previousBuild.addonSignature != signature
                val now = WatchedClock.nowEpochMs()
                val previouslyFailed = previousBuild?.failedSeriesKeys.orEmpty()
                val seriesWithUpcomingEpisodes = previousEvents
                    .filter { event -> event.key.startsWith("episode:") && event.date.iso >= today }
                    .map { event -> event.item.librarySeriesKey() }
                    .toSet()
                val bypassMetaCacheFor: (LibraryItem) -> Boolean = when {
                    addonsChanged -> { _ -> true }
                    forceFull -> { item ->
                        shouldBypassMetaCacheOnForce(item, previouslyFailed, seriesWithUpcomingEpisodes, now)
                    }
                    else -> { _ -> false }
                }
                val result = buildLibraryReleaseCalendarEvents(
                    items = items,
                    targetMonthKeys = targetMonthKeys,
                    bypassMetaCacheFor = bypassMetaCacheFor,
                )
                if (forceFull || addonsChanged) {
                    items.filter { item ->
                        item.isLibrarySeries() &&
                            item.librarySeriesKey() !in result.failedSeriesKeys &&
                            bypassMetaCacheFor(item)
                    }.forEach { item -> seriesRefetchedAtEpochMs[item.librarySeriesKey()] = now }
                }
                val preserved = previousEvents.filter { event ->
                    event.key.startsWith("episode:") &&
                        event.item.librarySeriesKey() in result.failedSeriesKeys &&
                        event.date.iso.take(7) in targetMonthKeys
                }
                LibraryReleaseCalendarCacheState(
                    cacheKey = cacheKey,
                    events = (result.events + preserved).sortedLibraryCalendarEvents(),
                    isReady = true,
                    loadedMonthKeys = targetMonthKeys,
                    builtOnIsoDate = today,
                    addonSignature = signature,
                    failedSeriesKeys = result.failedSeriesKeys,
                )
            }

            if (_state.value.cacheKey != cacheKey) return
            _state.value = next
            persist(next)
            if (next.failedSeriesKeys.isEmpty()) resetRetry() else scheduleRetry()
        } finally {
            if (_state.value.cacheKey == cacheKey && _state.value.isWarming) {
                _state.value = _state.value.copy(isWarming = false)
            }
        }
    }

    private suspend fun persist(snapshot: LibraryReleaseCalendarCacheState) {
        withContext(Dispatchers.Default) { LibraryReleaseSchedulePersistence.save(snapshot) }
    }

    private fun scheduleRetry() {
        if (retryJob?.isActive == true) return
        val delayMs = retryDelaysMs.getOrNull(retryAttempt) ?: return
        retryAttempt += 1
        retryJob = scope.launch {
            delay(delayMs)
            refreshIfStale()
        }
    }

    private fun resetRetry() {
        retryAttempt = 0
        retryJob?.cancel()
        retryJob = null
    }

    private fun ensureAddonWatcher() {
        if (addonWatcher?.isActive == true) return
        addonWatcher = scope.launch {
            AddonRepository.uiState
                .map { addonState ->
                    if (addonState.addons.hasPendingEnabledManifests()) null else currentLibraryAddonSignature(addonState)
                }
                .distinctUntilChanged()
                .drop(1)
                .collect { signature ->
                    if (signature == null) return@collect
                    if (_state.value.addonSignature != signature) {
                        // Addons became ready or changed: failures may now succeed.
                        resetRetry()
                        refreshIfStale()
                    }
                }
        }
    }
}

private fun currentLibraryAddonSignature(state: AddonsUiState): String =
    state.addons
        .enabledAddons()
        .filter { addon -> addon.manifest?.resources?.any { resource -> resource.name == "meta" } == true }
        .map { addon -> addon.manifestUrl }
        .sorted()
        .joinToString(separator = "|")

private suspend fun awaitLibraryAddonsSettled() {
    AddonRepository.initialize()
    withTimeoutOrNull(LIBRARY_CALENDAR_ADDON_SETTLE_TIMEOUT_MS) {
        AddonRepository.uiState.first { state -> !state.addons.hasPendingEnabledManifests() }
    }
}

private const val LIBRARY_CALENDAR_ADDON_SETTLE_TIMEOUT_MS = 15_000L
private const val LIBRARY_CALENDAR_FETCH_CONCURRENCY = 4
private const val LIBRARY_CALENDAR_FORCE_REFRESH_COOLDOWN_MS = 5L * 60_000L
private const val LIBRARY_CALENDAR_RETRY_COOLDOWN_MS = 30_000L
private const val LIBRARY_CALENDAR_FORCE_REUSE_WINDOW_MS = 60L * 60_000L

private fun LibraryItem.librarySeriesKey(): String = "${type.lowercase()}:$id"

private fun List<LibraryCalendarEvent>.sortedLibraryCalendarEvents(): List<LibraryCalendarEvent> =
    distinctBy { it.key }
        .sortedWith(compareBy<LibraryCalendarEvent> { it.date.iso }.thenBy { it.sortTitle.lowercase() })

private fun List<LibraryCalendarEvent>.withoutSupersededFallbacks(
    targetMonthKeys: Set<String>,
): List<LibraryCalendarEvent> {
    val seriesWithEpisodes = filter { it.key.startsWith("episode:") }
        .map { it.item.librarySeriesKey() }
        .toSet()
    return filterNot { event ->
        !event.key.startsWith("episode:") &&
            event.date.iso.take(7) in targetMonthKeys &&
            event.item.isLibrarySeries() &&
            event.item.librarySeriesKey() in seriesWithEpisodes
    }.sortedLibraryCalendarEvents()
}

internal fun refreshLibraryReleaseScheduleIfStale() {
    LibraryReleaseCalendarCache.refreshIfStale()
}

internal suspend fun clearLibraryReleaseScheduleCache() {
    LibraryReleaseCalendarCache.clearAndRebuild()
}

internal suspend fun forceRefreshLibraryReleaseSchedule(items: List<LibraryItem>) {
    LibraryReleaseCalendarCache.forceRefresh(items)
}

private fun libraryCalendarWarmMonthKeys(): Set<String> {
    val currentMonth = initialLibraryCalendarMonth()
    return setOf(
        currentMonth.previous().key,
        currentMonth.key,
        currentMonth.next().key,
    )
}

private class LibraryCalendarBuildResult(
    val events: List<LibraryCalendarEvent>,
    val failedSeriesKeys: Set<String>,
)

private suspend fun buildLibraryReleaseCalendarEvents(
    items: List<LibraryItem>,
    targetMonthKeys: Set<String>,
    onlySeriesKeys: Set<String>? = null,
    bypassMetaCacheFor: (LibraryItem) -> Boolean = { false },
): LibraryCalendarBuildResult {
    val fallbackEvents = if (onlySeriesKeys == null) buildLibraryReleaseCalendarFallbackEvents(items) else emptyList()
    val episodes = buildLibraryEpisodeCalendarEvents(items, targetMonthKeys, onlySeriesKeys, bypassMetaCacheFor)
    return LibraryCalendarBuildResult(
        events = (episodes.events + fallbackEvents).withoutSupersededFallbacks(targetMonthKeys),
        failedSeriesKeys = episodes.failedSeriesKeys,
    )
}

private fun buildLibraryReleaseCalendarFallbackEvents(items: List<LibraryItem>): List<LibraryCalendarEvent> =
    items
        .asSequence()
        .mapNotNull { item ->
            val rawReleaseInfo = item.releaseInfo?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val date = parseLibraryCalendarDate(rawReleaseInfo) ?: return@mapNotNull null
            LibraryCalendarEvent(
                key = "item:${item.type}:${item.id}:${date.iso}",
                date = date,
                rawReleaseInfo = rawReleaseInfo,
                item = item,
                title = item.name,
                imageUrl = item.banner ?: item.poster,
                sortTitle = item.name,
            )
        }
        .sortedWith(compareBy<LibraryCalendarEvent> { it.date.iso }.thenBy { it.item.name.lowercase() })
        .toList()

private suspend fun buildLibraryEpisodeCalendarEvents(
    items: List<LibraryItem>,
    targetMonthKeys: Set<String>,
    onlySeriesKeys: Set<String>?,
    bypassMetaCacheFor: (LibraryItem) -> Boolean,
): LibraryCalendarBuildResult =
    coroutineScope {
        val permits = Semaphore(LIBRARY_CALENDAR_FETCH_CONCURRENCY)
        val results = items
            .filter(LibraryItem::isLibrarySeries)
            .filter { item -> onlySeriesKeys == null || item.librarySeriesKey() in onlySeriesKeys }
            .distinctBy { item -> item.librarySeriesKey() }
            .map { item ->
                async {
                    permits.withPermit {
                        val details = try {
                            MetaDetailsRepository.fetch(item.type, item.id, useCache = !bypassMetaCacheFor(item))
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Throwable) {
                            null
                        }
                        item to details
                    }
                }
            }
            .awaitAll()
        val events = mutableListOf<LibraryCalendarEvent>()
        val failed = mutableSetOf<String>()
        results.forEach { (item, details) ->
            if (details == null) {
                failed += item.librarySeriesKey()
                return@forEach
            }
            details.videos
                .mapNotNull { video -> video.toLibraryCalendarEvent(item) }
                .filterTo(events) { event -> event.date.iso.take(7) in targetMonthKeys }
        }
        LibraryCalendarBuildResult(events = events, failedSeriesKeys = failed)
    }

private fun MetaVideo.toLibraryCalendarEvent(item: LibraryItem): LibraryCalendarEvent? {
    val rawReleaseInfo = released?.takeIf { it.isNotBlank() } ?: return null
    val date = parseLibraryCalendarDate(rawReleaseInfo) ?: return null
    val seasonNumber = season?.takeIf { it > 0 }
    val episodeNumber = episode?.takeIf { it > 0 }
    val episodeLabel = when {
        seasonNumber != null && episodeNumber != null -> "S${seasonNumber}E${episodeNumber}"
        episodeNumber != null -> "E$episodeNumber"
        else -> null
    }
    val subtitle = listOfNotNull(episodeLabel, title.takeIf { it.isNotBlank() })
        .joinToString(" - ")
        .takeIf { it.isNotBlank() }
    return LibraryCalendarEvent(
        key = "episode:${item.type}:${item.id}:${season ?: 0}:${episode ?: id}:${date.iso}",
        date = date,
        rawReleaseInfo = rawReleaseInfo,
        item = item,
        title = item.name,
        subtitle = subtitle,
        imageUrl = thumbnail ?: item.banner ?: item.poster,
        sortTitle = "${item.name} ${season ?: 0} ${episode ?: 0} $title",
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
    )
}

private fun LibraryItem.isLibrarySeries(): Boolean =
    type.equals("series", ignoreCase = true) ||
        type.equals("tv", ignoreCase = true) ||
        type.equals("show", ignoreCase = true) ||
        type.equals("tvshow", ignoreCase = true)

private fun parseLibraryCalendarDate(raw: String?): LibraryCalendarDate? {
    val datePart = raw
        ?.trim()
        ?.substringBefore('T')
        ?.takeIf { it.length == 10 }
        ?: return null
    val parts = datePart.split('-')
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull()?.takeIf { it in 1000..9999 } ?: return null
    val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
    val day = parts[2].toIntOrNull()?.takeIf { it in 1..daysInLibraryCalendarMonth(year, month) } ?: return null
    return LibraryCalendarDate(year, month, day)
}

private fun initialLibraryCalendarMonth(): LibraryCalendarMonth {
    val today = parseLibraryCalendarDate(CurrentDateProvider.todayIsoDate())
        ?: LibraryCalendarDate(1970, 1, 1)
    return LibraryCalendarMonth(today.year, today.month)
}

private fun defaultLibraryCalendarSelectedDate(
    monthEvents: List<LibraryCalendarEvent>,
    month: LibraryCalendarMonth,
    todayIso: String,
): String? {
    val isCurrentMonth = todayIso.take(7) == month.key
    return monthEvents
        .firstOrNull { event -> !isCurrentMonth || event.date.iso >= todayIso }
        ?.date
        ?.iso
        ?: monthEvents.firstOrNull()?.date?.iso
}

private fun defaultLibraryCalendarSelection(
    events: List<LibraryCalendarEvent>,
    month: LibraryCalendarMonth,
    todayIso: String,
): LibraryCalendarSelection {
    val monthEvents = events
        .filter { event -> event.date.year == month.year && event.date.month == month.month }
        .sortedWith(compareBy<LibraryCalendarEvent> { it.date.iso }.thenBy { it.sortTitle.lowercase() })
    return LibraryCalendarSelection(
        month = month,
        dateIso = defaultLibraryCalendarSelectedDate(monthEvents, month, todayIso)
            ?: LibraryCalendarDate(month.year, month.month, 1).iso,
    )
}

private fun displayLibraryCalendarEventDate(date: LibraryCalendarDate): String =
    "${localizedMonthName(date.month)} ${date.day}, ${date.year}"

private fun libraryCalendarCells(month: LibraryCalendarMonth): List<LibraryCalendarDate?> {
    val firstDayOffset = firstLibraryCalendarWeekdayOffset(month.year, month.month)
    val days = daysInLibraryCalendarMonth(month.year, month.month)
    val cells = MutableList<LibraryCalendarDate?>(firstDayOffset) { null }
    for (day in 1..days) {
        cells += LibraryCalendarDate(month.year, month.month, day)
    }
    while (cells.size < 42) {
        cells += null
    }
    return cells
}

private fun firstLibraryCalendarWeekdayOffset(year: Int, month: Int): Int {
    val epochDay = isoEpochDay(LibraryCalendarDate(year, month, 1).iso)
    val raw = (epochDay + 4L) % 7L
    return if (raw < 0L) (raw + 7L).toInt() else raw.toInt()
}

private fun daysInLibraryCalendarMonth(year: Int, month: Int): Int =
    when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLibraryCalendarLeapYear(year)) 29 else 28
        else -> 30
    }

private fun isLibraryCalendarLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

private fun isoEpochDay(date: String): Long {
    val year = date.substring(0, 4).toLong()
    val month = date.substring(5, 7).toLong()
    val day = date.substring(8, 10).toLong()

    val adjustedYear = year - if (month <= 2L) 1L else 0L
    val era = if (adjustedYear >= 0L) adjustedYear / 400L else (adjustedYear - 399L) / 400L
    val yearOfEra = adjustedYear - era * 400L
    val adjustedMonth = month + if (month > 2L) -3L else 9L
    val dayOfYear = (153L * adjustedMonth + 2L) / 5L + day - 1L
    val dayOfEra = yearOfEra * 365L + yearOfEra / 4L - yearOfEra / 100L + dayOfYear
    return era * 146_097L + dayOfEra - 719_468L
}

private enum class LibraryViewMode {
    Saved,
    Cloud,
}

private const val LIBRARY_SECTION_PREVIEW_LIMIT = 18

private data class LibraryDisplayEntry(
    val globalKey: String,
    val item: LibraryItem,
    val section: LibrarySection?,
    val exiting: Boolean,
)

private data class LibraryDisplaySection(
    val source: LibrarySection?,
    val type: String,
    val displayTitle: String,
    val previewEntries: List<LibraryDisplayEntry>,
)

private class LibraryExitingEntry(
    val item: LibraryItem,
    val sectionType: String,
    val sectionTitle: String,
    val index: Int,
)

private class LibraryDisintegrationHolder {
    private val tracker = ScopedDisintegrationTracker<LibrarySourceMode, String, LibraryExitingEntry> { entry ->
        librarySectionItemKey(entry.sectionType, entry.item)
    }

    fun onExited(globalKey: String) {
        tracker.onDisintegrated(globalKey)
    }

    fun reset() {
        tracker.reset()
    }

    fun sync(
        sourceMode: LibrarySourceMode,
        sections: List<LibrarySection>,
        previewLimit: Int,
        request: DisintegrationRequest<String>?,
    ): List<LibraryDisplaySection> {
        val current = ArrayList<LibraryExitingEntry>()
        sections.forEach { section ->
            section.items.take(previewLimit).forEachIndexed { index, item ->
                current += LibraryExitingEntry(item, section.type, section.displayTitle, index)
            }
        }
        val exitingBySection = tracker.sync(sourceMode, current, request)
            .asSequence()
            .filter { entry -> entry.exiting }
            .map { entry -> entry.item }
            .groupBy { entry -> entry.sectionType }
        val seenTypes = HashSet<String>(sections.size)
        val result = ArrayList<LibraryDisplaySection>(sections.size + 1)

        for (section in sections) {
            seenTypes += section.type
            val entries = ArrayList<LibraryDisplayEntry>(previewLimit + 1)
            section.items.take(previewLimit).forEach { item ->
                entries += LibraryDisplayEntry(
                    globalKey = librarySectionItemKey(section.type, item),
                    item = item,
                    section = section,
                    exiting = false,
                )
            }
            exitingBySection[section.type]?.sortedBy { it.index }?.forEach { ex ->
                val key = librarySectionItemKey(section.type, ex.item)
                if (entries.none { it.globalKey == key }) {
                    entries.add(
                        ex.index.coerceIn(0, entries.size),
                        LibraryDisplayEntry(key, ex.item, section, exiting = true),
                    )
                }
            }
            result += LibraryDisplaySection(section, section.type, section.displayTitle, entries)
        }

        for ((type, list) in exitingBySection) {
            if (type in seenTypes) continue
            val sorted = list.sortedBy { it.index }
            val entries = sorted.map { ex ->
                LibraryDisplayEntry(librarySectionItemKey(type, ex.item), ex.item, section = null, exiting = true)
            }
            result += LibraryDisplaySection(null, type, sorted.first().sectionTitle, entries)
        }

        return result
    }
}

/** An episode from the Library release calendar, exposed for other screens (Profile Insight). */
internal data class LibraryUpcomingEpisode(
    val key: String,
    val item: LibraryItem,
    val dateIso: String,
    val subtitle: String?,
    val imageUrl: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
)

internal suspend fun warmLibraryReleaseSchedule(items: List<LibraryItem>) {
    if (items.isNotEmpty()) LibraryReleaseCalendarCache.warm(items)
}

/**
 * Episodes of saved series airing from today through the next [days] days (today included),
 * read from the same release-calendar cache the Library calendar shows, so both always agree.
 */
internal fun libraryUpcomingEpisodesFlow(days: Int = 7): Flow<List<LibraryUpcomingEpisode>> =
    combine(LibraryReleaseCalendarCache.state, ProfileRepository.state) { state, profiles ->
        // The in-memory cache is shared across profiles and isn't rebuilt for an empty library, so
        // it can still hold the previous profile's episodes; only trust it for the active profile.
        val activeIndex = profiles.activeProfile?.profileIndex ?: return@combine emptyList()
        if (state.cacheKey?.startsWith("$activeIndex:") != true) return@combine emptyList()
        state.events.upcomingEpisodes(days)
    }

private fun List<LibraryCalendarEvent>.upcomingEpisodes(days: Int): List<LibraryUpcomingEpisode> {
    val today = parseLibraryCalendarDate(CurrentDateProvider.todayIsoDate()) ?: return emptyList()
    val windowIsoDates = (0 until days).map { offset -> libraryCalendarDatePlusDays(today, offset).iso }.toSet()
    return asSequence()
        .filter { event -> event.key.startsWith("episode:") && event.date.iso in windowIsoDates }
        .distinctBy { it.key }
        .sortedWith(compareBy<LibraryCalendarEvent> { it.date.iso }.thenBy { it.sortTitle.lowercase() })
        .map { event ->
            LibraryUpcomingEpisode(
                key = event.key,
                item = event.item,
                dateIso = event.date.iso,
                subtitle = event.subtitle,
                imageUrl = event.imageUrl,
                seasonNumber = event.seasonNumber,
                episodeNumber = event.episodeNumber,
            )
        }
        .toList()
}

@Serializable
private data class StoredLibraryCalendarEvent(
    val key: String,
    val dateIso: String,
    val rawReleaseInfo: String,
    val itemType: String,
    val itemId: String,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val sortTitle: String,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
)

@Serializable
private data class StoredLibraryReleaseSchedule(
    val cacheKey: String,
    val savedOnIsoDate: String,
    val addonSignature: String? = null,
    val failedSeriesKeys: Set<String> = emptySet(),
    val loadedMonthKeys: Set<String> = emptySet(),
    val events: List<StoredLibraryCalendarEvent> = emptyList(),
)

private class RestoredLibraryReleaseSchedule(
    val cacheKey: String,
    val savedOnIsoDate: String,
    val addonSignature: String?,
    val failedSeriesKeys: Set<String>,
    val loadedMonthKeys: Set<String>,
    val events: List<LibraryCalendarEvent>,
)

private object LibraryReleaseSchedulePersistence {
    private val json = Json { ignoreUnknownKeys = true }

    fun save(state: LibraryReleaseCalendarCacheState) {
        val cacheKey = state.cacheKey ?: return
        runCatching {
            val payload = StoredLibraryReleaseSchedule(
                cacheKey = cacheKey,
                savedOnIsoDate = state.builtOnIsoDate ?: CurrentDateProvider.todayIsoDate(),
                addonSignature = state.addonSignature,
                failedSeriesKeys = state.failedSeriesKeys,
                loadedMonthKeys = state.loadedMonthKeys,
                events = state.events.map { event ->
                    StoredLibraryCalendarEvent(
                        key = event.key,
                        dateIso = event.date.iso,
                        rawReleaseInfo = event.rawReleaseInfo,
                        itemType = event.item.type,
                        itemId = event.item.id,
                        title = event.title,
                        subtitle = event.subtitle,
                        imageUrl = event.imageUrl,
                        sortTitle = event.sortTitle,
                        seasonNumber = event.seasonNumber,
                        episodeNumber = event.episodeNumber,
                    )
                },
            )
            LibraryReleaseScheduleStorage.savePayload(json.encodeToString(StoredLibraryReleaseSchedule.serializer(), payload))
        }
    }

    fun load(items: List<LibraryItem>): RestoredLibraryReleaseSchedule? {
        val raw = LibraryReleaseScheduleStorage.loadPayload() ?: return null
        val stored = runCatching {
            json.decodeFromString(StoredLibraryReleaseSchedule.serializer(), raw)
        }.getOrNull() ?: return null
        val itemsByKey = items.associateBy { item -> "${item.type.lowercase()}:${item.id}" }
        val events = stored.events.mapNotNull { event ->
            val item = itemsByKey["${event.itemType.lowercase()}:${event.itemId}"] ?: return@mapNotNull null
            val date = parseLibraryCalendarDate(event.dateIso) ?: return@mapNotNull null
            LibraryCalendarEvent(
                key = event.key,
                date = date,
                rawReleaseInfo = event.rawReleaseInfo,
                item = item,
                title = event.title,
                subtitle = event.subtitle,
                imageUrl = event.imageUrl,
                sortTitle = event.sortTitle,
                seasonNumber = event.seasonNumber,
                episodeNumber = event.episodeNumber,
            )
        }
        return RestoredLibraryReleaseSchedule(
            cacheKey = stored.cacheKey,
            savedOnIsoDate = stored.savedOnIsoDate,
            addonSignature = stored.addonSignature,
            failedSeriesKeys = stored.failedSeriesKeys,
            loadedMonthKeys = stored.loadedMonthKeys,
            events = events,
        )
    }
}

private fun libraryCalendarDatePlusDays(date: LibraryCalendarDate, days: Int): LibraryCalendarDate {
    var year = date.year
    var month = date.month
    var day = date.day + days
    while (day > daysInLibraryCalendarMonth(year, month)) {
        day -= daysInLibraryCalendarMonth(year, month)
        if (month == 12) {
            month = 1
            year += 1
        } else {
            month += 1
        }
    }
    return LibraryCalendarDate(year, month, day)
}

