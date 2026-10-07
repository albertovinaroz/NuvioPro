package com.nuvio.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import com.nuvio.app.core.ui.NativeProfileSwitcherController
import com.nuvio.app.navigation.AppRoute
import platform.UIKit.UIColor
import platform.UIKit.UIViewController

private val nuvioBackgroundColor = UIColor(red = 0.051, green = 0.051, blue = 0.051, alpha = 1.0)

@Suppress("unused")
fun MainViewController(): UIViewController = nuvioComposeViewController {
    App()
}

@Suppress("unused")
fun MainViewController(
    initialTabName: String,
    useNativeTabBar: Boolean,
    useTabletFloatingTabBar: Boolean,
    onNavigate: (AppRoute, Boolean) -> Unit,
    onGoBack: () -> Unit,
    onReplace: (AppRoute) -> Unit,
    onActivate: (String) -> Unit,
    onTabTitles: (String, String, String, String, String, String, String, String, String) -> Unit,
    appGateController: AppGateController,
): UIViewController {
    val initialTab = AppScreenTab.fromName(initialTabName)
    return nuvioComposeViewController {
        App(
            initialTab = initialTab,
            useNativeNavigation = true,
            useNativeTabBar = useNativeTabBar,
            useTabletFloatingTabBar = useTabletFloatingTabBar,
            ownsAppRuntime = initialTab == AppScreenTab.Home,
            bypassAppGate = true,
            onNavigate = onNavigate,
            onGoBack = onGoBack,
            onReplace = onReplace,
            onActivate = { tab -> onActivate(tab.name) },
            onTabTitles = onTabTitles,
            appGateController = appGateController,
        )
    }
}

@Suppress("unused")
fun ScreenViewController(
    route: AppRoute,
    onNavigate: (AppRoute, Boolean) -> Unit,
    onGoBack: () -> Unit,
    onReplace: (AppRoute) -> Unit,
    onActivate: (String) -> Unit,
    appGateController: AppGateController,
    useNativeTabBar: Boolean = false,
    useTabletFloatingTabBar: Boolean = false,
): UIViewController = nuvioComposeViewController {
    App(
        initialRoute = route,
        useNativeNavigation = true,
        useNativeTabBar = useNativeTabBar,
        useTabletFloatingTabBar = useTabletFloatingTabBar,
        ownsAppRuntime = false,
        bypassAppGate = true,
        onNavigate = onNavigate,
        onGoBack = onGoBack,
        onReplace = onReplace,
        onActivate = { tab -> onActivate(tab.name) },
        appGateController = appGateController,
    )
}

@Suppress("unused")
@OptIn(ExperimentalComposeUiApi::class)
fun AppGateViewController(
    appGateController: AppGateController,
    nativeProfileSwitcherController: NativeProfileSwitcherController,
    onActivate: (String) -> Unit,
    onAppReady: (Boolean) -> Unit,
    onMainContentMountChanged: (Boolean) -> Unit,
    onMainContentVisibleChanged: (Boolean) -> Unit,
): UIViewController = ComposeUIViewController(
    configure = {
        onFocusBehavior = OnFocusBehavior.DoNothing
        opaque = false
    },
    content = {
        AppGateOverlay(
            onActivate = { tab -> onActivate(tab.name) },
            onAppReady = onAppReady,
            onMainContentMountChanged = onMainContentMountChanged,
            onMainContentVisibleChanged = onMainContentVisibleChanged,
            nativeProfileSwitcherController = nativeProfileSwitcherController,
            appGateController = appGateController,
        )
    },
).apply {
    view.backgroundColor = UIColor.clearColor
}

/**
 * The floating tab bar drawn once above every tab (see [FloatingTabBarOverlay]). Transparent: the
 * Swift host lays a native blur behind it at [onGlassBounds] and passes touches through elsewhere.
 */
@Suppress("unused")
@OptIn(ExperimentalComposeUiApi::class)
fun FloatingTabBarViewController(
    appGateController: AppGateController,
    useTabletFloatingTabBar: Boolean,
    onSelectTab: (String) -> Unit,
    onGlassBounds: (Float, Float, Float, Float) -> Unit,
    onCapturesAllTouches: (Boolean) -> Unit,
): UIViewController = ComposeUIViewController(
    configure = {
        onFocusBehavior = OnFocusBehavior.DoNothing
        opaque = false
    },
    content = {
        FloatingTabBarOverlay(
            appGateController = appGateController,
            useTabletFloatingTabBar = useTabletFloatingTabBar,
            onSelectTab = { tab -> onSelectTab(tab.name) },
            onGlassBounds = onGlassBounds,
            onCapturesAllTouches = onCapturesAllTouches,
        )
    },
).apply {
    view.backgroundColor = UIColor.clearColor
}

private fun nuvioComposeViewController(
    content: @androidx.compose.runtime.Composable () -> Unit,
): UIViewController = ComposeUIViewController(
    configure = { onFocusBehavior = OnFocusBehavior.DoNothing },
    content = content,
).apply {
    view.backgroundColor = nuvioBackgroundColor
}
