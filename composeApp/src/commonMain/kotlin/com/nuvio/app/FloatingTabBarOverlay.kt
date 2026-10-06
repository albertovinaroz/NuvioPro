package com.nuvio.app

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.FloatingNavigationBar
import com.nuvio.app.core.ui.LocalNuvioTabletNavLayout
import com.nuvio.app.core.ui.LocalOverlayTouchCapture
import com.nuvio.app.core.ui.NativeTabBridge
import com.nuvio.app.core.ui.SharedNuvioNavBarScrollState
import com.nuvio.app.features.livetv.LiveTvRepository
import com.nuvio.app.features.settings.NavBarPosition
import com.nuvio.app.features.settings.NavBarStyle
import com.nuvio.app.features.settings.ThemeSettingsRepository

/** Whether the floating bar sits at the bottom, where the native-navigation overlay draws it. */
internal fun floatingTabBarOverlayShowsBar(style: NavBarStyle, position: NavBarPosition): Boolean =
    style != NavBarStyle.CLASSIC && position != NavBarPosition.TOP

/**
 * The floating tab bar as one transparent scene layered above every tab's navigation stack (iOS
 * native navigation without the system tab bar). Drawn once up here rather than by each screen, it
 * stays put while settings pages push and pop beneath it, instead of sliding away with the old
 * page and coming back in with a fresh copy on the new one.
 *
 * Haze can't sample other Compose scenes, so the host puts a native blur view behind the canvas,
 * kept on the glass via [onGlassBounds] (points, in this scene's coordinates; all zero = no bar).
 */
@Composable
internal fun FloatingTabBarOverlay(
    appGateController: AppGateController,
    useTabletFloatingTabBar: Boolean,
    onSelectTab: (AppScreenTab) -> Unit,
    onGlassBounds: (x: Float, y: Float, width: Float, height: Float) -> Unit,
    onCapturesAllTouches: (Boolean) -> Unit,
) {
    val currentOnGlassBounds by rememberUpdatedState(onGlassBounds)
    AppEnvironment {
        val style by ThemeSettingsRepository.navBarStyle.collectAsStateWithLifecycle()
        val position by ThemeSettingsRepository.navBarPosition.collectAsStateWithLifecycle()
        val glowEnabled by ThemeSettingsRepository.navBarGlowEnabled.collectAsStateWithLifecycle()
        val liveTvUiState by remember {
            LiveTvRepository.ensureLoaded()
            LiveTvRepository.uiState
        }.collectAsStateWithLifecycle()
        val activeTab by NativeTabBridge.activeTab.collectAsState()
        val showsBar = floatingTabBarOverlayShowsBar(style, position)
        val density = LocalDensity.current.density
        val lastBounds = remember { FloatArray(4) }
        fun publish(x: Float, y: Float, width: Float, height: Float) {
            if (lastBounds[0] == x && lastBounds[1] == y && lastBounds[2] == width && lastBounds[3] == height) return
            lastBounds[0] = x; lastBounds[1] = y; lastBounds[2] = width; lastBounds[3] = height
            currentOnGlassBounds(x, y, width, height)
        }
        LaunchedEffect(showsBar) { if (!showsBar) publish(0f, 0f, 0f, 0f) }
        if (!showsBar) return@AppEnvironment

        when (style) {
            NavBarStyle.EXPANDED -> SharedNuvioNavBarScrollState.expand()
            NavBarStyle.COMPACT -> SharedNuvioNavBarScrollState.collapse()
            else -> {}
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Same rule as MainTabsDestination, so the bar matches what the screens leave room for.
            val isTabletLayout = useTabletFloatingTabBar || maxWidth >= 768.dp
            CompositionLocalProvider(
                LocalOverlayTouchCapture provides onCapturesAllTouches,
                LocalNuvioTabletNavLayout provides isTabletLayout,
            ) {
                FloatingNavigationBar(
                    items = mainFloatingNavigationItems(
                        highlightedTab = activeTab.toAppScreenTab(),
                        showLiveTv = liveTvUiState.showInNavigation,
                        onTabSelected = onSelectTab,
                        onProfileSelected = { NativeTabBridge.requestProfileSelection(it.profileIndex) },
                        onAddProfileRequested = appGateController::requestProfileSelection,
                        hazeState = null,
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                    scrollState = SharedNuvioNavBarScrollState,
                    contentPadding = phoneFloatingNavigationBarPadding(),
                    glowEnabled = glowEnabled,
                    inlineLabels = isTabletLayout,
                    showLabels = !isIos,
                    expandOnSelect = style == NavBarStyle.ADAPTIVE,
                    nativeBackdrop = true,
                    onGlassBoundsChanged = { bounds: Rect ->
                        publish(
                            bounds.left / density,
                            bounds.top / density,
                            bounds.width / density,
                            bounds.height / density,
                        )
                    },
                )
            }
        }
    }
}
