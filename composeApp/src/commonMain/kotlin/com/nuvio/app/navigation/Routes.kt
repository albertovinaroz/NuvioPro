package com.nuvio.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey {
    val title: String?
        get() = null

    val subtitle: String?
        get() = null

    /** Full-screen destinations such as the video player keep native navigation chrome hidden. */
    val hidesNavigationBar: Boolean
        get() = false

    /** Stable enough to apply Navigation 3 launchSingleTop semantics in SwiftUI. */
    val navigationIdentity: String
        get() = toString()

    /** Lets an explicitly cross-tab route select its native SwiftUI stack. */
    val preferredTabName: String?
        get() = null
}

@Serializable
sealed interface SettingsDestinationRoute : AppRoute {
    override val preferredTabName: String
        get() = "Settings"
}

@Serializable
data object TabsRoute : AppRoute

@Serializable
data class DetailRoute(
    val type: String,
    val id: String,
    override val title: String? = null,
    val initialSeasonNumber: Int? = null,
    val initialEpisodeNumber: Int? = null,
) : AppRoute

@Serializable
data class PersonDetailRoute(
    val personId: Int,
    val personName: String,
    val personPhoto: String? = null,
    val castAvatarTransitionKey: String? = null,
    val preferCrew: Boolean = false,
) : AppRoute {
    override val title: String
        get() = personName
}

@Serializable
data class EntityBrowseRoute(
    val entityKind: String,
    val entityId: Int,
    val entityName: String,
    val sourceType: String = "tv",
) : AppRoute {
    override val title: String
        get() = entityName
}

/** A settings leaf promoted from the former in-screen page state machine. */
@Serializable
data class SettingsPageRoute(
    val pageName: String,
    override val title: String,
) : SettingsDestinationRoute

/** Reached from Home's notifications bell (poster hero style), not nested under any tab. */
@Serializable
data class NotificationFeedRoute(
    override val title: String,
) : AppRoute

/** Reached two ways: from Settings' "What's New" row (should push onto the Settings tab, like any
 * other SettingsDestinationRoute), and from an app-update card in the notification feed (should
 * stay on whichever tab the feed itself was opened from, usually Home). forceSettingsTab picks
 * between the two per navigate() call instead of baking one in at the type level. */
@Serializable
data class WhatsNewRoute(
    override val title: String = "",
    val forceSettingsTab: Boolean = true,
) : AppRoute {
    override val preferredTabName: String?
        get() = if (forceSettingsTab) "Settings" else null
}

@Serializable
data class HomescreenSettingsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
data class MetaScreenSettingsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
data class ContinueWatchingSettingsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
data class DownloadsSettingsRoute(
    override val title: String = "",
    // Every other SettingsDestinationRoute is only ever reached from within Settings, where
    // hardcoding "Settings" is exactly right. This one is also reached from Home's downloads
    // shortcut (see MainAppContent's onDownloadsClick) — that push should stay on whichever tab
    // it came from instead, so it can opt out and fall back to the origin tab.
    val forceSettingsTab: Boolean = true,
) : AppRoute {
    override val preferredTabName: String?
        get() = if (forceSettingsTab) "Settings" else null
}

@Serializable
data class DownloadShowRoute(
    val showId: String,
    override val title: String,
) : AppRoute

@Serializable
data class AddonsSettingsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
data class PluginsSettingsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
data class AccountSettingsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
data class SupportersContributorsSettingsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
data class LicensesAttributionsSettingsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
data class ProfileEditRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
data class CollectionsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
data class CollectionEditorRoute(
    val collectionId: String? = null,
    override val title: String = "",
) : AppRoute

@Serializable
data class CollectionEditorPageRoute(
    val collectionId: String? = null,
    val pageName: String,
    override val title: String,
) : AppRoute

@Serializable
data class FolderDetailRoute(
    val collectionId: String,
    val folderId: String,
    override val title: String = "",
) : AppRoute

@Serializable
data class StreamRoute(
    val launchId: Long,
    override val title: String = "",
) : AppRoute {
    // Real native nav bar left visible (the default) meant StreamsScreen's own NuvioBackButton
    // silently rendered nothing (see NuvioBackButton's early-return on
    // LocalUseNativeNavigation.current && !LocalNativeNavigationBarHidden.current) — the "<"
    // button on screen was always the *native* one, not Compose's. On iOS 26 that real
    // UINavigationBar also participates in the system's automatic content-scroll-view detection
    // (the same contentScrollView(for:) machinery fought at length elsewhere in the tab bar), and
    // its actual native touch-intercepting bounds don't reliably match what's visually drawn —
    // root cause of luqmanfadlli/NuvioMobile-Enhanced#99 (addon filter chips unreachable in
    // landscape on first play: touches were being swallowed by this native bar before ever
    // reaching Compose, confirmed by an on-screen tap logger not seeing them at all). Hiding it
    // removes the native chrome entirely and hands the back button to Compose instead, matching
    // what this screen's own code already assumed it was doing.
    override val hidesNavigationBar: Boolean
        get() = true
}

@Serializable
data class CatalogRoute(
    val launchId: Long,
    override val title: String = "",
    override val subtitle: String? = null,
) : AppRoute

@Serializable
data class PlayerRoute(
    val launchId: Long,
    override val title: String = "",
) : AppRoute {
    override val hidesNavigationBar: Boolean
        get() = true
}
