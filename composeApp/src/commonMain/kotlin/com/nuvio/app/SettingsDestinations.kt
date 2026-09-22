package com.nuvio.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nuvio.app.features.collection.CollectionEditorPage
import com.nuvio.app.features.collection.CollectionEditorScreen
import com.nuvio.app.features.collection.CollectionManagementScreen
import com.nuvio.app.features.collection.CollectionRepository
import com.nuvio.app.features.collection.FolderDetailRepository
import com.nuvio.app.features.collection.FolderDetailScreen
import com.nuvio.app.features.downloads.DownloadItem
import com.nuvio.app.features.downloads.DownloadsScreen
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.settings.SettingsPage
import com.nuvio.app.features.settings.SettingsScreen
import com.nuvio.app.navigation.AppRoute
import com.nuvio.app.navigation.CollectionEditorPageRoute
import com.nuvio.app.navigation.CollectionEditorRoute
import com.nuvio.app.navigation.CollectionsRoute
import com.nuvio.app.navigation.DetailRoute
import com.nuvio.app.navigation.DownloadShowRoute
import com.nuvio.app.navigation.DownloadsPreferencesRoute
import com.nuvio.app.navigation.DownloadsSettingsRoute
import com.nuvio.app.navigation.FolderDetailRoute
import com.nuvio.app.navigation.NuvioNavigator
import com.nuvio.app.navigation.SettingsPageRoute
import com.nuvio.app.navigation.TrailingMenuAction
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.profile_insights_edit_profile
import nuvio.composeapp.generated.resources.profile_insights_switch_profile
import org.jetbrains.compose.resources.stringResource

/** The Profile page's native trailing "..." nav bar menu; every other settings page gets none.
 * Not composable-scoped to a pageName check since it's consumed inside a plain navigate() click
 * lambda — call this once during composition and gate its use on `pageName` at the call site. */
@Composable
internal fun profileEditSwitchMenuActions(): List<TrailingMenuAction> = listOf(
    TrailingMenuAction(
        id = "edit_profile",
        title = stringResource(Res.string.profile_insights_edit_profile),
        systemImageName = "pencil",
    ),
    TrailingMenuAction(
        id = "switch_profile",
        title = stringResource(Res.string.profile_insights_switch_profile),
        systemImageName = "person.2",
    ),
)

@Composable
internal fun SettingsDestination(
    route: AppRoute,
    navController: NuvioNavigator,
    content: @Composable (onBack: () -> Unit) -> Unit,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    content(onBack)
}

@Composable
internal fun SettingsRootDestination(
    route: SettingsPageRoute,
    navController: NuvioNavigator,
    useNativeNavigation: Boolean,
    downloadsTitle: String,
    collectionsTitle: String,
    onCheckForUpdates: (() -> Unit)?,
    onTestUpdateBanner: (() -> Unit)?,
    onSwitchProfile: (() -> Unit)? = null,
    onEditProfile: (() -> Unit)? = null,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    val profileMenuActions = profileEditSwitchMenuActions()
    SettingsScreen(
        modifier = Modifier.fillMaxSize(),
        initialPageName = route.pageName,
        rootActionsEnabled = false,
        onNavigatePage = { pageName, title ->
            navController.navigate(
                SettingsPageRoute(
                    pageName = pageName,
                    title = title,
                    trailingMenuActions = if (pageName == SettingsPage.Profile.name) {
                        profileMenuActions
                    } else {
                        emptyList()
                    },
                ),
            )
        },
        onExternalBack = onBack,
        showInternalHeader = !useNativeNavigation,
        onSwitchProfile = onSwitchProfile,
        onEditProfile = onEditProfile,
        onPosterClick = { meta ->
            navController.navigate(DetailRoute(type = meta.type, id = meta.id, title = meta.name))
        },
        onDownloadsClick = {
            navController.navigate(DownloadsSettingsRoute(downloadsTitle))
        },
        onCollectionsClick = {
            navController.navigate(CollectionsRoute(collectionsTitle))
        },
        onCheckForUpdatesClick = onCheckForUpdates,
        onTestUpdateBannerClick = onTestUpdateBanner,
    )
}

@Composable
internal fun DownloadsDestination(
    route: DownloadsSettingsRoute,
    navController: NuvioNavigator,
    useNativeNavigation: Boolean,
    onOpenDownload: (DownloadItem) -> Unit,
    settingsTitle: String,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    DownloadsScreen(
        onBack = onBack,
        onOpenDownload = onOpenDownload,
        onNavigateToShow = if (useNativeNavigation) {
            { showId, title -> navController.navigate(DownloadShowRoute(showId, title)) }
        } else {
            null
        },
        onOpenSettings = { navController.navigate(DownloadsPreferencesRoute(settingsTitle)) },
    )
}

@Composable
internal fun DownloadShowDestination(
    route: DownloadShowRoute,
    navController: NuvioNavigator,
    onOpenDownload: (DownloadItem) -> Unit,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    DownloadsScreen(
        onBack = onBack,
        onOpenDownload = onOpenDownload,
        initialShowId = route.showId,
        onBackFromShow = onBack,
    )
}

@Composable
internal fun CollectionsDestination(
    route: CollectionsRoute,
    navController: NuvioNavigator,
    newCollectionTitle: String,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    CollectionManagementScreen(
        onBack = onBack,
        onNavigateToEditor = { collectionId ->
            val editorTitle = collectionId
                ?.let { id ->
                    CollectionRepository.collections.value.firstOrNull { it.id == id }?.title
                }
                .orEmpty()
            navController.navigate(
                CollectionEditorRoute(
                    collectionId = collectionId,
                    title = editorTitle.ifBlank { newCollectionTitle },
                ),
            )
        },
    )
}

@Composable
internal fun CollectionEditorDestination(
    route: CollectionEditorRoute,
    navController: NuvioNavigator,
    useNativeNavigation: Boolean,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    CollectionEditorScreen(
        collectionId = route.collectionId,
        onBack = onBack,
        initialPage = if (useNativeNavigation) CollectionEditorPage.Root else null,
        onNavigateToPage = if (useNativeNavigation) {
            { page, title ->
                navController.navigate(
                    CollectionEditorPageRoute(
                        collectionId = route.collectionId,
                        pageName = page.name,
                        title = title,
                    ),
                )
            }
        } else {
            null
        },
    )
}

@Composable
internal fun CollectionEditorPageDestination(
    route: CollectionEditorPageRoute,
    navController: NuvioNavigator,
) {
    val page = remember(route.pageName) {
        runCatching { CollectionEditorPage.valueOf(route.pageName) }.getOrNull()
    }
    val onBack = rememberGuardedPopBackStack(navController, route)
    if (page == null || page == CollectionEditorPage.Root) {
        LaunchedEffect(route) { onBack() }
        return
    }
    CollectionEditorScreen(
        collectionId = route.collectionId,
        initialPage = page,
        initializeRepository = false,
        onBack = onBack,
        onNavigateToPage = { nextPage, title ->
            navController.navigate(
                CollectionEditorPageRoute(
                    collectionId = route.collectionId,
                    pageName = nextPage.name,
                    title = title,
                ),
            )
        },
    )
}

@Composable
internal fun FolderDestination(
    route: FolderDetailRoute,
    navController: NuvioNavigator,
    onCatalogClick: (HomeCatalogSection) -> Unit,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    LaunchedEffect(route.collectionId, route.folderId) {
        FolderDetailRepository.initialize(route.collectionId, route.folderId)
    }
    FolderDetailScreen(
        onBack = onBack,
        onCatalogClick = onCatalogClick,
        onPosterClick = { meta: MetaPreview ->
            navController.navigate(DetailRoute(type = meta.type, id = meta.id, title = meta.name))
        },
    )
}
