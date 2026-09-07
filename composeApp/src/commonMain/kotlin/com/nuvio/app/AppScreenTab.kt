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
