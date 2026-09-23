package com.nuvio.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import com.nuvio.app.core.ui.NativeNavigationTab
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.sidebar_library
import nuvio.composeapp.generated.resources.sidebar_search
import org.jetbrains.compose.resources.DrawableResource

enum class AppScreenTab {
    Home,
    Search,
    Library,
    LiveTv,
    Settings,
    ;

    companion object {
        fun fromName(name: String): AppScreenTab =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Home
    }
}

internal fun AppScreenTab.toNativeNavigationTab(): NativeNavigationTab = when (this) {
    AppScreenTab.Home -> NativeNavigationTab.Home
    AppScreenTab.Search -> NativeNavigationTab.Search
    AppScreenTab.Library -> NativeNavigationTab.Library
    AppScreenTab.LiveTv -> NativeNavigationTab.LiveTv
    AppScreenTab.Settings -> NativeNavigationTab.Settings
}

internal fun NativeNavigationTab.toAppScreenTab(): AppScreenTab = when (this) {
    NativeNavigationTab.Home -> AppScreenTab.Home
    NativeNavigationTab.Search -> AppScreenTab.Search
    NativeNavigationTab.Library -> AppScreenTab.Library
    NativeNavigationTab.LiveTv -> AppScreenTab.LiveTv
    NativeNavigationTab.Settings -> AppScreenTab.Settings
}

/**
 * The Material icon for [tab] in the tab bar — filled while selected, outlined while not, so the
 * active tab reads as "filled in" rather than only changing tint (matches the same filled/outline
 * swap iOS's native tab bar does with SF Symbols). Settings is never actually rendered through
 * this — it always shows the profile avatar instead — but is covered for exhaustiveness.
 */
internal fun AppScreenTab.icon(selected: Boolean): ImageVector = when (this) {
    AppScreenTab.Home -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    AppScreenTab.Search -> if (selected) Icons.Filled.Search else Icons.Outlined.Search
    AppScreenTab.Library -> if (selected) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary
    AppScreenTab.LiveTv -> if (selected) Icons.Filled.Tv else Icons.Outlined.Tv
    AppScreenTab.Settings -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
}

/**
 * Nuvio's own hand-drawn sidebar icon for [tab], matching what every other tab bar (native iOS,
 * Android, the Settings preview) actually shows — unlike [icon] above, this isn't a generic
 * Material fallback. Only defined for Search/Library, the two tabs where a custom drawable
 * (rather than a Material icon) is the source of truth.
 *
 * Lives here instead of being referenced directly from MainTabsDestination.kt: Res.drawable.
 * sidebar_search/library fail to resolve from that file specifically despite compiling into the
 * generated commonMain resource accessors (a resource-generation edge case) — resolving them from
 * this file and exposing them through a plain property works around it.
 */
internal val AppScreenTab.sidebarDrawable: DrawableResource
    get() = when (this) {
        AppScreenTab.Search -> Res.drawable.sidebar_search
        AppScreenTab.Library -> Res.drawable.sidebar_library
        else -> error("No sidebar drawable for $this")
    }
