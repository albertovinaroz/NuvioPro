package com.nuvio.app

import androidx.compose.runtime.collectAsState
import com.nuvio.app.core.ui.NativeTabBridge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.LocalNuvioBottomNavigationOverlayPadding
import com.nuvio.app.core.ui.LocalNuvioNavBarScrollState
import com.nuvio.app.core.ui.NuvioNavBarScrollState
import com.nuvio.app.core.ui.NuvioClassicNavigationBar
import com.nuvio.app.core.ui.FloatingNavigationBar
import com.nuvio.app.navigation.LocalUseNativeNavigation
import com.nuvio.app.core.ui.FloatingNavigationItem
import com.nuvio.app.core.ui.PlatformBackHandler
import com.nuvio.app.core.ui.LocalNuvioTabletNavLayout
import com.nuvio.app.core.ui.floatingNavigationBarPadding
import com.nuvio.app.features.settings.NavBarPosition
import com.nuvio.app.core.ui.rememberNuvioNavBarScrollState
import com.nuvio.app.features.profiles.NuvioProfile
import com.nuvio.app.features.profiles.ProfileSwitcherTab
import com.nuvio.app.features.settings.NavBarStyle
import com.nuvio.app.features.settings.ThemeSettingsRepository
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_nav_home
import nuvio.composeapp.generated.resources.compose_nav_library
import nuvio.composeapp.generated.resources.compose_nav_live_tv
import nuvio.composeapp.generated.resources.compose_settings_page_root
import nuvio.composeapp.generated.resources.compose_nav_search
import nuvio.composeapp.generated.resources.sidebar_home_outline
import nuvio.composeapp.generated.resources.sidebar_library_outline
import nuvio.composeapp.generated.resources.sidebar_search_filled
import nuvio.composeapp.generated.resources.sidebar_search_outline
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import dev.chrisbanes.haze.HazeState

@Composable
internal fun MainTabsDestination(
    selectedTab: AppScreenTab,
    initialHomeReady: Boolean,
    rootRouteActive: Boolean,
    useTabletFloatingTabBar: Boolean,
    useNativeNavigation: Boolean,
    useNativeTabBar: Boolean,
    liquidGlassNativeTabBarSupported: Boolean,
    liquidGlassNativeTabBarEnabled: Boolean,
    showLiveTvInNavigation: Boolean,
    requests: AppTabRequests,
    state: AppTabState,
    actions: (isTabletLayout: Boolean) -> AppTabActions,
    onBack: () -> Unit,
    onTabSelected: (AppScreenTab) -> Unit,
    onProfileSelected: (NuvioProfile) -> Unit,
    onAddProfileRequested: () -> Unit,
) {
    PlatformBackHandler(enabled = rootRouteActive, onBack = onBack)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTabletLayout = useTabletFloatingTabBar || maxWidth >= 768.dp
        val nativeActiveTab by NativeTabBridge.activeTab.collectAsState()
        val highlightedTab = if (useNativeNavigation) nativeActiveTab.toAppScreenTab() else selectedTab
        val tabActions = remember(actions, isTabletLayout) { actions(isTabletLayout) }
        val useNativeBottomTabs = if (useNativeNavigation) {
            useNativeTabBar
        } else {
            liquidGlassNativeTabBarSupported && liquidGlassNativeTabBarEnabled && initialHomeReady
        }
        val tabsRouteActive = rootRouteActive
        val navBarScrollState = rememberNuvioNavBarScrollState()
        val navBarHazeState = rememberHazeState()
        val navBarStyleSetting by remember { ThemeSettingsRepository.navBarStyle }.collectAsStateWithLifecycle()
        val navBarGlowEnabled by ThemeSettingsRepository.navBarGlowEnabled.collectAsStateWithLifecycle()
        val navBarPosition by ThemeSettingsRepository.navBarPosition.collectAsStateWithLifecycle()
        val floatingBarOnTop = navBarStyleSetting != NavBarStyle.CLASSIC && navBarPosition == NavBarPosition.TOP
        // With native navigation each root tab is its own Compose scene with its own copy of the
        // bar, all kept alive. One pill is shared across them so a tab switch hands the animation
        // over seamlessly, and only the copy on screen does the follow-on expand.
        val isOnScreenTab = !useNativeNavigation || highlightedTab == selectedTab
        val floatingNavigationItems = mainFloatingNavigationItems(
            highlightedTab = highlightedTab,
            showLiveTv = showLiveTvInNavigation,
            onTabSelected = onTabSelected,
            onProfileSelected = onProfileSelected,
            onAddProfileRequested = onAddProfileRequested,
            hazeState = navBarHazeState,
        )

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (initialHomeReady) 1f else 0f),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (tabsRouteActive && !isTabletLayout && !useNativeBottomTabs && navBarStyleSetting == NavBarStyle.CLASSIC) {
                    MainClassicNavigationBar(
                        highlightedTab = highlightedTab,
                        showLiveTv = showLiveTvInNavigation,
                        onTabSelected = onTabSelected,
                        onProfileSelected = onProfileSelected,
                        onAddProfileRequested = onAddProfileRequested,
                    )
                }
            },
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(
                    LocalNuvioBottomNavigationOverlayPadding provides when {
                        tabsRouteActive && useNativeBottomTabs -> 49.dp
                        // A top pill floats over the content like the tablet CLASSIC bar, so
                        // nothing needs to be kept clear at the bottom.
                        tabsRouteActive && navBarStyleSetting != NavBarStyle.CLASSIC && !floatingBarOnTop -> 72.dp
                        else -> 0.dp
                    },
                    LocalNuvioNavBarScrollState provides navBarScrollState,
                    LocalNuvioTabletNavLayout provides isTabletLayout,
                ) {
                    AppTabHost(
                        selectedTab = selectedTab,
                        requests = requests,
                        state = state,
                        actions = tabActions,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (tabsRouteActive && navBarStyleSetting != NavBarStyle.CLASSIC) Modifier.hazeSource(state = navBarHazeState) else Modifier)
                            .then(if (tabsRouteActive && navBarStyleSetting == NavBarStyle.ADAPTIVE) Modifier.nestedScroll(navBarScrollState.nestedScrollConnection) else Modifier)
                            .padding(innerPadding),
                    )
                }

                // CLASSIC keeps the previous per-form-factor chrome: a top rail on
                // tablet/landscape and the solid bottom bar on phones. Every other
                // style now uses the floating pill at the bottom on all sizes.
                if (isTabletLayout && !useNativeBottomTabs && navBarStyleSetting == NavBarStyle.CLASSIC) {
                    val tabletNavBarScrollState = remember { NuvioNavBarScrollState().apply { collapse() } }
                    FloatingNavigationBar(
                        modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 416.dp),
                        scrollState = tabletNavBarScrollState,
                        hazeState = navBarHazeState,
                        contentPadding = PaddingValues(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 10.dp,
                            bottom = 8.dp,
                        ),
                        compactSize = true,
                        items = floatingNavigationItems,
                        glowEnabled = navBarGlowEnabled,
                        sharedMotionKey = if (useNativeNavigation) "main_tabs_top" else null,
                    )
                }

                if (tabsRouteActive && !useNativeBottomTabs && navBarStyleSetting != NavBarStyle.CLASSIC) {
                    when (navBarStyleSetting) {
                        NavBarStyle.EXPANDED -> navBarScrollState.expand()
                        NavBarStyle.COMPACT -> navBarScrollState.collapse()
                        else -> {}
                    }
                    FloatingNavigationBar(
                        modifier = Modifier.align(if (floatingBarOnTop) Alignment.TopCenter else Alignment.BottomCenter),
                        scrollState = navBarScrollState,
                        hazeState = navBarHazeState,
                        contentPadding = if (floatingBarOnTop) {
                            PaddingValues(
                                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 10.dp,
                                bottom = 8.dp,
                            )
                        } else {
                            phoneFloatingNavigationBarPadding()
                        },
                        items = floatingNavigationItems,
                        glowEnabled = navBarGlowEnabled,
                        inlineLabels = isTabletLayout,
                        showLabels = !isIos,
                        // A tab tap always leaves the bar expanded, rather than in whatever state
                        // the previous tab's scroll left it.
                        expandOnSelect = navBarStyleSetting == NavBarStyle.ADAPTIVE && isOnScreenTab,
                        sharedMotionKey = if (useNativeNavigation) "main_tabs" else null,
                    )
                }
            }
        }
    }
}

@Composable
internal fun mainFloatingNavigationItems(
    highlightedTab: AppScreenTab,
    showLiveTv: Boolean,
    onTabSelected: (AppScreenTab) -> Unit,
    onProfileSelected: (NuvioProfile) -> Unit,
    onAddProfileRequested: () -> Unit,
    hazeState: HazeState?,
): List<FloatingNavigationItem> = buildList {
    add(
        FloatingNavigationItem(
            selected = highlightedTab == AppScreenTab.Home,
            onClick = { onTabSelected(AppScreenTab.Home) },
            icon = Icons.Filled.Home,
            label = stringResource(Res.string.compose_nav_home),
            outlineDrawable = Res.drawable.sidebar_home_outline,
        ),
    )
    add(
        FloatingNavigationItem(
            selected = highlightedTab == AppScreenTab.Search,
            onClick = { onTabSelected(AppScreenTab.Search) },
            // The bar's own filled lens (solid glass, like the iOS tab icon); the sidebar's
            // search drawable is a ring, which reads as unfilled next to its outline.
            drawable = Res.drawable.sidebar_search_filled,
            label = stringResource(Res.string.compose_nav_search),
            outlineDrawable = Res.drawable.sidebar_search_outline,
        ),
    )
    add(
        FloatingNavigationItem(
            selected = highlightedTab == AppScreenTab.Library,
            onClick = { onTabSelected(AppScreenTab.Library) },
            drawable = AppScreenTab.Library.sidebarDrawable,
            label = stringResource(Res.string.compose_nav_library),
            outlineDrawable = Res.drawable.sidebar_library_outline,
        ),
    )
    if (showLiveTv) {
        add(
            FloatingNavigationItem(
                selected = highlightedTab == AppScreenTab.LiveTv,
                onClick = { onTabSelected(AppScreenTab.LiveTv) },
                icon = Icons.Filled.Tv,
                label = stringResource(Res.string.compose_nav_live_tv),
                outlineIcon = Icons.Outlined.Tv,
            ),
        )
    }
    add(
        FloatingNavigationItem(
            selected = highlightedTab == AppScreenTab.Settings,
            onClick = { onTabSelected(AppScreenTab.Settings) },
            label = stringResource(Res.string.compose_settings_page_root),
            content = { onClick ->
                ProfileSwitcherTab(
                    selected = highlightedTab == AppScreenTab.Settings,
                    onClick = onClick,
                    onProfileSelected = onProfileSelected,
                    onAddProfileRequested = onAddProfileRequested,
                    hazeState = hazeState,
                    popupBelowAnchor = false,
                )
            },
        ),
    )
}

@Composable
internal fun MainClassicNavigationBar(
    highlightedTab: AppScreenTab,
    showLiveTv: Boolean,
    onTabSelected: (AppScreenTab) -> Unit,
    onProfileSelected: (NuvioProfile) -> Unit,
    onAddProfileRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NuvioClassicNavigationBar(modifier) {
        NavItem(
            selected = highlightedTab == AppScreenTab.Home,
            onClick = { onTabSelected(AppScreenTab.Home) },
            icon = Icons.Filled.Home,
            contentDescription = stringResource(Res.string.compose_nav_home),
        )
        NavItem(
            selected = highlightedTab == AppScreenTab.Search,
            onClick = { onTabSelected(AppScreenTab.Search) },
            icon = AppScreenTab.Search.sidebarDrawable,
            contentDescription = stringResource(Res.string.compose_nav_search),
        )
        NavItem(
            selected = highlightedTab == AppScreenTab.Library,
            onClick = { onTabSelected(AppScreenTab.Library) },
            icon = AppScreenTab.Library.sidebarDrawable,
            contentDescription = stringResource(Res.string.compose_nav_library),
        )
        if (showLiveTv) {
            NavItem(
                selected = highlightedTab == AppScreenTab.LiveTv,
                onClick = { onTabSelected(AppScreenTab.LiveTv) },
                icon = Icons.Filled.Tv,
                contentDescription = stringResource(Res.string.compose_nav_live_tv),
            )
        }
        NavItem(
            selected = highlightedTab == AppScreenTab.Settings,
            onClick = { onTabSelected(AppScreenTab.Settings) },
        ) {
            ProfileSwitcherTab(
                selected = highlightedTab == AppScreenTab.Settings,
                onClick = { onTabSelected(AppScreenTab.Settings) },
                onProfileSelected = onProfileSelected,
                onAddProfileRequested = onAddProfileRequested,
            )
        }
    }
}

/**
 * Bottom padding for the floating bar: on iOS it sits closer to the bottom edge (it's icon-only
 * there). Shared by the tabs bar and the settings-route bar so the two never drift apart.
 */
@Composable
private fun phoneFloatingNavigationBarPadding(): PaddingValues {
    if (!isIos) return floatingNavigationBarPadding()
    val base = floatingNavigationBarPadding().calculateBottomPadding()
    return PaddingValues(bottom = (base - 18.dp).coerceAtLeast(0.dp))
}

@Composable
internal fun BoxScope.SettingsRouteNavigationBar(
    isTabletLayout: Boolean,
    showLiveTv: Boolean,
    hazeState: HazeState?,
    onTabSelected: (AppScreenTab) -> Unit,
    onProfileSelected: (NuvioProfile) -> Unit,
    onAddProfileRequested: () -> Unit,
    onBottomOverlayChanged: (Dp) -> Unit,
) {
    val style by remember { ThemeSettingsRepository.navBarStyle }.collectAsStateWithLifecycle()
    val glowEnabled by ThemeSettingsRepository.navBarGlowEnabled.collectAsStateWithLifecycle()
    val position by ThemeSettingsRepository.navBarPosition.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val highlightedTab = AppScreenTab.Settings

    when {
        style == NavBarStyle.CLASSIC && isTabletLayout -> {
            SideEffect { onBottomOverlayChanged(0.dp) }
            FloatingNavigationBar(
                modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 416.dp),
                scrollState = remember { com.nuvio.app.core.ui.NuvioNavBarScrollState().apply { collapse() } },
                hazeState = hazeState,
                contentPadding = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 10.dp,
                    bottom = 8.dp,
                ),
                compactSize = true,
                items = mainFloatingNavigationItems(
                    highlightedTab = highlightedTab,
                    showLiveTv = showLiveTv,
                    onTabSelected = onTabSelected,
                    onProfileSelected = onProfileSelected,
                    onAddProfileRequested = onAddProfileRequested,
                    hazeState = hazeState,
                ),
                glowEnabled = glowEnabled,
            )
        }
        style == NavBarStyle.CLASSIC -> {
            Box(
                Modifier.align(Alignment.BottomCenter).onSizeChanged {
                    onBottomOverlayChanged(with(density) { it.height.toDp() })
                },
            ) {
                MainClassicNavigationBar(
                    highlightedTab = highlightedTab,
                    showLiveTv = showLiveTv,
                    onTabSelected = onTabSelected,
                    onProfileSelected = onProfileSelected,
                    onAddProfileRequested = onAddProfileRequested,
                )
            }
        }
        else -> {
            val onTop = position == NavBarPosition.TOP
            SideEffect { onBottomOverlayChanged(if (onTop) 0.dp else 72.dp) }
            FloatingNavigationBar(
                items = mainFloatingNavigationItems(
                    highlightedTab = highlightedTab,
                    showLiveTv = showLiveTv,
                    onTabSelected = onTabSelected,
                    onProfileSelected = onProfileSelected,
                    onAddProfileRequested = onAddProfileRequested,
                    hazeState = hazeState,
                ),
                modifier = Modifier.align(if (onTop) Alignment.TopCenter else Alignment.BottomCenter),
                scrollState = if (style == NavBarStyle.COMPACT) {
                    remember { com.nuvio.app.core.ui.NuvioNavBarScrollState().apply { collapse() } }
                } else {
                    null
                },
                hazeState = hazeState,
                contentPadding = if (onTop) {
                    PaddingValues(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 10.dp,
                        bottom = 8.dp,
                    )
                } else {
                    phoneFloatingNavigationBarPadding()
                },
                glowEnabled = glowEnabled,
                inlineLabels = isTabletLayout,
                showLabels = !isIos,
                // Same pill as the root tabs' bar, so leaving or returning to a settings page
                // carries the selection animation over instead of restarting it.
                sharedMotionKey = if (LocalUseNativeNavigation.current) "main_tabs" else null,
            )
        }
    }
}
