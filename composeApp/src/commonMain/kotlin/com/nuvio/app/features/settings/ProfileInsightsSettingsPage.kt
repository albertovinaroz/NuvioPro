package com.nuvio.app.features.settings

import co.touchlab.kermit.Logger
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.format.resolveReleaseInfoForDisplay
import com.nuvio.app.core.ui.platformPhysicalTopInset
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.gradientMask
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.core.ui.NuvioAsyncImage
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.library.LibraryRepository
import com.nuvio.app.features.library.LibraryUiState
import com.nuvio.app.features.profiles.AvatarCatalogItem
import com.nuvio.app.features.profiles.AvatarRepository
import com.nuvio.app.features.profiles.NuvioProfile
import com.nuvio.app.features.profiles.ProfileBackgroundBackdrop
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.profiles.parseHexColor
import com.nuvio.app.features.profiles.profileAvatarImageUrl
import com.nuvio.app.features.watched.WatchedClock
import com.nuvio.app.features.watched.WatchedItem
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watched.WatchedUiState
import com.nuvio.app.features.watched.watchedItemKey
import com.nuvio.app.features.watchprogress.CurrentDateProvider
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.app.features.watchprogress.WatchProgressUiState
import kotlin.math.roundToInt
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private val profileInsightsLog = Logger.withTag("ProfileInsights")

internal fun LazyListScope.profileInsightsContent(
    isTablet: Boolean,
    onSwitchProfile: (() -> Unit)?,
    onEditProfile: (() -> Unit)?,
    onPosterClick: ((MetaPreview) -> Unit)?,
    // True when a native "..." nav bar menu already covers Edit/Switch Profile (native
    // navigation on phone) — the old floating hero buttons then stay hidden instead of
    // duplicating those actions.
    hasNativeTrailingMenu: Boolean = false,
) {
    item {
        ProfileInsightsBody(
            isTablet = isTablet,
            onSwitchProfile = onSwitchProfile,
            onEditProfile = onEditProfile,
            onPosterClick = onPosterClick,
            hasNativeTrailingMenu = hasNativeTrailingMenu,
        )
    }
}

@Composable
private fun ProfileInsightsBody(
    isTablet: Boolean,
    onSwitchProfile: (() -> Unit)?,
    onEditProfile: (() -> Unit)?,
    onPosterClick: ((MetaPreview) -> Unit)?,
    hasNativeTrailingMenu: Boolean = false,
) {
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val avatars by AvatarRepository.avatars.collectAsStateWithLifecycle()
    val watchProgressState by remember {
        WatchProgressRepository.ensureLoaded()
        WatchProgressRepository.uiState
    }.collectAsStateWithLifecycle()
    val watchedState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val libraryState by remember {
        LibraryRepository.ensureLoaded()
        LibraryRepository.uiState
    }.collectAsStateWithLifecycle()
    val todayIsoDate = remember { CurrentDateProvider.todayIsoDate() }

    LaunchedEffect(Unit) {
        AvatarRepository.fetchAvatars()
    }

    val activeProfile = profileState.activeProfile
    val activeProfileIndex = activeProfile?.profileIndex ?: ProfileRepository.activeProfileId
    val avatarItem = remember(activeProfile?.avatarId, avatars) {
        activeProfile
            ?.avatarId
            ?.let { avatarId -> avatars.firstOrNull { avatar -> avatar.id == avatarId } }
    }
    val profileNameFallback = stringResource(Res.string.compose_nav_profile)
    val profileName = activeProfile
        ?.name
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: profileNameFallback
    val stats = remember(activeProfileIndex, watchProgressState, watchedState, fullyWatchedSeriesKeys, libraryState, todayIsoDate) {
        runCatching {
            buildProfileInsightsStats(
                watchProgressState = watchProgressState,
                watchedState = watchedState,
                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                libraryState = libraryState,
                todayIsoDate = todayIsoDate,
            )
        }.onFailure { error ->
            profileInsightsLog.e(error) { "Failed to build profile insights stats profile=$activeProfileIndex" }
        }.getOrElse {
            emptyProfileInsightsStats()
        }
    }
    val continueTitle = stringResource(Res.string.profile_insights_stat_continue)
    val completedTitle = stringResource(Res.string.profile_insights_stat_completed)
    val ongoingTitle = stringResource(Res.string.profile_insights_stat_ongoing)
    val libraryTitle = stringResource(Res.string.profile_insights_stat_library)
    val upcomingTitle = stringResource(Res.string.profile_insights_stat_upcoming)
    val insightCollections = remember(
        activeProfileIndex,
        watchProgressState,
        watchedState,
        fullyWatchedSeriesKeys,
        libraryState,
        todayIsoDate,
        continueTitle,
        completedTitle,
        ongoingTitle,
        libraryTitle,
        upcomingTitle,
    ) {
        runCatching {
            buildProfileInsightCollections(
                watchProgressState = watchProgressState,
                watchedState = watchedState,
                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                libraryState = libraryState,
                todayIsoDate = todayIsoDate,
                continueTitle = continueTitle,
                completedTitle = completedTitle,
                ongoingTitle = ongoingTitle,
                libraryTitle = libraryTitle,
                upcomingTitle = upcomingTitle,
            )
        }.onFailure { error ->
            profileInsightsLog.e(error) { "Failed to build profile insight collections profile=$activeProfileIndex" }
        }.getOrElse {
            emptyProfileInsightCollections(
                continueTitle = continueTitle,
                completedTitle = completedTitle,
                ongoingTitle = ongoingTitle,
                libraryTitle = libraryTitle,
                upcomingTitle = upcomingTitle,
            )
        }
    }
    var selectedInsightCollection by remember { mutableStateOf<ProfileInsightCollection?>(null) }
    LaunchedEffect(activeProfileIndex) {
        selectedInsightCollection = null
    }
    val isCollectionAvailable: (ProfileInsightCollectionKind) -> Boolean = { kind ->
        insightCollections[kind]?.items?.isNotEmpty() == true
    }
    val onCollectionClick: (ProfileInsightCollectionKind) -> Unit = { kind ->
        selectedInsightCollection = insightCollections[kind]
            ?.takeIf { collection -> collection.items.isNotEmpty() }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        ProfileInsightsHero(
            profile = activeProfile,
            avatarItem = avatarItem,
            profileName = profileName,
            isTablet = isTablet,
            stats = stats,
            isCollectionAvailable = isCollectionAvailable,
            onCollectionClick = onCollectionClick,
            onEditProfile = onEditProfile.takeUnless { isTablet },
            onSwitchProfile = onSwitchProfile.takeUnless { isTablet },
            hasNativeTrailingMenu = hasNativeTrailingMenu,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (isTablet) 18.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (isTablet) 18.dp else 14.dp),
        ) {
            // On phone, both actions live as stacked circular buttons in the hero overlay
            // instead (see ProfileHeaderIconButton) — only tablet's bounded hero keeps them
            // inline here.
            val inlineEditProfile = onEditProfile.takeIf { isTablet }
            val inlineSwitchProfile = onSwitchProfile.takeIf { isTablet }
            if (inlineSwitchProfile != null || inlineEditProfile != null) {
                ProfileManagementActions(
                    isTablet = isTablet,
                    onSwitchProfile = inlineSwitchProfile,
                    onEditProfile = inlineEditProfile,
                )
            }
            ProfileWatchTimeRow(stats = stats)
            SettingsSection(
                title = null,
                isTablet = isTablet,
            ) {
                ProfileTasteCard(stats = stats)
            }
        }
    }

    selectedInsightCollection?.let { collection ->
        ProfileInsightCollectionSheet(
            collection = collection,
            isTablet = isTablet,
            onDismiss = { selectedInsightCollection = null },
            onPosterClick = onPosterClick,
        )
    }

}

@Composable
private fun ProfileManagementActions(
    isTablet: Boolean,
    onSwitchProfile: (() -> Unit)?,
    onEditProfile: (() -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = tokens.spacing.controlGap),
        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 14.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onSwitchProfile != null) {
            NuvioPrimaryButton(
                text = stringResource(Res.string.profile_insights_switch_profile),
                onClick = onSwitchProfile,
                modifier = Modifier.weight(1f),
            )
        }
        if (onEditProfile != null) {
            OutlinedButton(
                onClick = onEditProfile,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = tokens.shapes.button,
                border = BorderStroke(1.dp, tokens.colors.borderSubtle),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = tokens.colors.textPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    modifier = Modifier.gradientMask(MaterialTheme.themePalette.accentBrush()),
                    tint = tokens.colors.accent,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.profile_insights_edit_profile),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Top-right circular counterpart to the header's back button, for the phone cinematic hero —
 * glass style matches [ProfileMetricPill] since both float over the same backdrop photo.
 */
@Composable
private fun ProfileHeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ProfileInsightsHero(
    profile: NuvioProfile?,
    avatarItem: AvatarCatalogItem?,
    profileName: String,
    isTablet: Boolean,
    stats: ProfileInsightsStats,
    isCollectionAvailable: (ProfileInsightCollectionKind) -> Boolean,
    onCollectionClick: (ProfileInsightCollectionKind) -> Unit,
    onEditProfile: (() -> Unit)?,
    onSwitchProfile: (() -> Unit)?,
    hasNativeTrailingMenu: Boolean = false,
) {
    // The bled, edge-to-edge treatment below is tuned specifically for the phone/portrait path:
    // it deliberately reaches past the Settings scaffold's padding to the true screen edges and
    // past the top to sit behind iOS's native (not Compose-drawn) navigation title. Regular width
    // (iPad, or an iPhone rotated to landscape) uses a completely different presentation — a
    // Settings list + detail pane split, with a real Compose-drawn TabletPageHeader above this and
    // no bleed-worthy screen edge to reach at all — so it gets a plain, bounded card instead.
    if (isTablet) {
        ProfileInsightsHeroBounded(
            profile = profile,
            avatarItem = avatarItem,
            profileName = profileName,
            stats = stats,
            isCollectionAvailable = isCollectionAvailable,
            onCollectionClick = onCollectionClick,
        )
    } else {
        ProfileInsightsHeroCinematic(
            profile = profile,
            avatarItem = avatarItem,
            profileName = profileName,
            stats = stats,
            isCollectionAvailable = isCollectionAvailable,
            onCollectionClick = onCollectionClick,
            onEditProfile = onEditProfile,
            onSwitchProfile = onSwitchProfile,
            hasNativeTrailingMenu = hasNativeTrailingMenu,
        )
    }
}

@Composable
private fun ProfileInsightsHeroBounded(
    profile: NuvioProfile?,
    avatarItem: AvatarCatalogItem?,
    profileName: String,
    stats: ProfileInsightsStats,
    isCollectionAvailable: (ProfileInsightCollectionKind) -> Boolean,
    onCollectionClick: (ProfileInsightCollectionKind) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val accent = profile?.avatarColorHex?.let(::parseHexColor) ?: tokens.colors.accent
    val avatarImageUrl = remember(profile, avatarItem) {
        profile?.let { profileAvatarImageUrl(it, avatarItem) }
    }
    val shape = RoundedCornerShape(34.dp)

    // A fixed height instead of an aspectRatio: avatar + title + pill row need a predictable
    // amount of room regardless of how wide/narrow the detail pane is, and 21:9 was too short for
    // that content on a landscape phone specifically — it overflowed the card's own bounds and
    // visually overlapped the Switch/Edit profile buttons below it.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape),
    ) {
        if (profile != null) {
            ProfileBackgroundBackdrop(profile = profile, modifier = Modifier.matchParentSize())
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0E1727), accent.copy(alpha = 0.42f), tokens.colors.surface),
                        ),
                    ),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.20f), Color.Black.copy(alpha = 0.88f)),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProfileHeroAvatar(
                profileName = profileName,
                avatarImageUrl = avatarImageUrl,
                avatarColor = accent,
                avatarBackgroundColor = avatarItem?.bgColor?.let(::parseHexColor) ?: accent,
                isTablet = true,
            )
            Text(
                text = stringResource(Res.string.profile_insights_title, profileName),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            ProfileMetricPillRow(
                stats = stats,
                isCollectionAvailable = isCollectionAvailable,
                onCollectionClick = onCollectionClick,
            )
        }
    }
}

@Composable
private fun ProfileInsightsHeroCinematic(
    profile: NuvioProfile?,
    avatarItem: AvatarCatalogItem?,
    profileName: String,
    stats: ProfileInsightsStats,
    isCollectionAvailable: (ProfileInsightCollectionKind) -> Boolean,
    onCollectionClick: (ProfileInsightCollectionKind) -> Unit,
    onEditProfile: (() -> Unit)?,
    onSwitchProfile: (() -> Unit)?,
    hasNativeTrailingMenu: Boolean = false,
) {
    val tokens = MaterialTheme.nuvio
    val accent = profile?.avatarColorHex?.let(::parseHexColor) ?: tokens.colors.accent
    val avatarImageUrl = remember(profile, avatarItem) {
        profile?.let { profileAvatarImageUrl(it, avatarItem) }
    }

    // The outer Box's size is fixed purely by fillMaxWidth+aspectRatio, using the page's normal
    // (un-bled) width — that's what gets reported to the list, so it doesn't push everything below
    // down just because the photo inside renders bigger. The inner BoxWithConstraints reads that
    // same width/height to size a *separate*, oversized+offset photo layer that bleeds past the
    // shared Settings scaffold's horizontal padding to the true screen edges, and past its top to
    // reach behind the status bar / native title (Box doesn't clip overflowing children, so this
    // is purely a drawing-position trick — it never touches the reported layout size above).
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        BoxWithConstraints(modifier = Modifier.matchParentSize()) {
            val leftInset = tokens.spacing.screenHorizontal + 40.dp
            val rightInset = tokens.spacing.screenHorizontal + 160.dp
            val bleedWidth = maxWidth + leftInset + rightInset
            val topExtension = 56.dp
            val extendedHeight = maxHeight + topExtension

            Box(
                modifier = Modifier
                    .requiredWidth(bleedWidth)
                    .requiredHeight(extendedHeight)
                    .offset(x = -leftInset, y = -topExtension),
            ) {
                if (profile != null) {
                    ProfileBackgroundBackdrop(
                        profile = profile,
                        modifier = Modifier.matchParentSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF0E1727),
                                        accent.copy(alpha = 0.42f),
                                        tokens.colors.surface,
                                    ),
                                ),
                            ),
                    )
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.20f),
                                    Color.Black.copy(alpha = 0.88f),
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY,
                            ),
                        ),
                )
            }

            // Normal (un-bled) width, so it stays where the rest of the page's content sits —
            // reserves room below for the pill row via bottomPadding rather than sharing a Column
            // with it, since that pill row needs a completely different (bled) width.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 140.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProfileHeroAvatar(
                    profileName = profileName,
                    avatarImageUrl = avatarImageUrl,
                    avatarColor = accent,
                    avatarBackgroundColor = avatarItem?.bgColor?.let(::parseHexColor) ?: accent,
                    isTablet = false,
                )
                Text(
                    text = stringResource(Res.string.profile_insights_title, profileName),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // No side padding: flush to the hero's own true edges (the hero Box is already
            // full-width), matching the Airy Grid content below it. No end padding either — the
            // row's own scroll clipping already cuts the last pill mid-way when they don't all
            // fit, which is exactly the "there's more, scroll" affordance; reserving matching
            // space on the right would just hide that peek.
            ProfileMetricPillRow(
                stats = stats,
                isCollectionAvailable = isCollectionAvailable,
                onCollectionClick = onCollectionClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 18.dp),
            )

            if (onEditProfile != null && !hasNativeTrailingMenu) {
                // iOS's native nav bar (title/back button) paints on top of this whole strip
                // as an opaque system layer, so this can't sit level with it like the back
                // button — clearing at least the safe area, not just the header's own padding,
                // keeps this from rendering underneath, invisible.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = platformPhysicalTopInset() + 4.dp, end = 18.dp),
                ) {
                    ProfileHeaderIconButton(
                        icon = Icons.Rounded.Edit,
                        contentDescription = stringResource(Res.string.profile_insights_edit_profile),
                        onClick = onEditProfile,
                    )
                }
            }

            if (onSwitchProfile != null && !hasNativeTrailingMenu) {
                // Same bottom anchor as the avatar/name column below, so this lines up with the
                // profile name rather than the edit button above.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 140.dp, end = 18.dp),
                ) {
                    ProfileHeaderIconButton(
                        icon = Icons.Rounded.People,
                        contentDescription = stringResource(Res.string.profile_insights_switch_profile),
                        onClick = onSwitchProfile,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeroAvatar(
    profileName: String,
    avatarImageUrl: String?,
    avatarColor: Color,
    avatarBackgroundColor: Color,
    isTablet: Boolean,
) {
    val size = if (isTablet) 92.dp else 78.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (avatarImageUrl.isNullOrBlank()) {
                    avatarColor.copy(alpha = 0.18f)
                } else {
                    avatarBackgroundColor
                },
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.28f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (!avatarImageUrl.isNullOrBlank()) {
            NuvioAsyncImage(
                imageUrl = avatarImageUrl,
                contentDescription = profileName,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                animateIfPossible = true,
            )
        } else {
            Text(
                text = profileName.take(1).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ProfileMetricPillRow(
    stats: ProfileInsightsStats,
    isCollectionAvailable: (ProfileInsightCollectionKind) -> Boolean,
    onCollectionClick: (ProfileInsightCollectionKind) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val pills = listOf(
        ProfileMetricPillSpec(
            value = stats.continueCount.toString(),
            label = stringResource(Res.string.profile_insights_hero_continue),
            collectionKind = ProfileInsightCollectionKind.Continue,
        ),
        ProfileMetricPillSpec(
            value = stats.libraryCount.toString(),
            label = stringResource(Res.string.profile_insights_hero_library),
            collectionKind = ProfileInsightCollectionKind.Library,
        ),
        ProfileMetricPillSpec(
            value = stats.upcomingCount.toString(),
            label = stringResource(Res.string.profile_insights_hero_upcoming),
            collectionKind = ProfileInsightCollectionKind.Upcoming,
        ),
        ProfileMetricPillSpec(
            value = stats.completedCount.toString(),
            label = stringResource(Res.string.profile_insights_stat_completed),
            collectionKind = ProfileInsightCollectionKind.Completed,
        ),
        ProfileMetricPillSpec(
            value = stats.ongoingSeriesCount.toString(),
            label = stringResource(Res.string.profile_insights_stat_ongoing),
            collectionKind = ProfileInsightCollectionKind.Ongoing,
        ),
    )

    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxWidth()) {
        // A plain scrollable Row instead of LazyRow: with contentPadding this wide (it reaches
        // past the true screen edge to match the bled photo behind it), LazyRow was starting at a
        // nonzero initial scroll offset on iOS, clipping the first pill until the user nudged it —
        // a Row's scroll position is simply 0 at rest, with no such quirk.
        // The start/end insets are real Spacer children, not an outer .padding() — padding applied
        // after .horizontalScroll() resizes the viewport, it doesn't add space inside the
        // scrollable content, so it was leaving the first pill flush against (and clipped by) the
        // true edge.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Spacer(modifier = Modifier.width(contentPadding.calculateStartPadding(LayoutDirection.Ltr)))
            pills.forEach { pill ->
                ProfileMetricPill(
                    spec = pill,
                    onClick = pill.collectionKind
                        .takeIf(isCollectionAvailable)
                        ?.let { kind -> { onCollectionClick(kind) } },
                )
            }
            Spacer(modifier = Modifier.width(contentPadding.calculateEndPadding(LayoutDirection.Ltr)))
        }

        if (scrollState.canScrollBackward) {
            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-6).dp)
                    .size(20.dp),
            )
        }
        if (scrollState.canScrollForward) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 6.dp)
                    .size(20.dp),
            )
        }
    }
}

private data class ProfileMetricPillSpec(
    val value: String,
    val label: String,
    val collectionKind: ProfileInsightCollectionKind,
)

@Composable
private fun ProfileMetricPill(
    spec: ProfileMetricPillSpec,
    onClick: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .widthIn(min = 58.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = spec.value,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 16.sp,
                    maxFontSize = MaterialTheme.typography.titleLarge.fontSize,
                ),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = spec.label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(MaterialTheme.themePalette.accentBrush(alpha = 0.55f)),
        )
    }
}

@Composable
private fun ProfileWatchTimeRow(stats: ProfileInsightsStats) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = profileInsightDurationLabel(stats.trackedDurationMs),
            style = MaterialTheme.typography.displaySmall,
            color = tokens.colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.profile_insights_stat_time_caption),
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.colors.textMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileInsightCollectionSheet(
    collection: ProfileInsightCollection,
    isTablet: Boolean,
    onDismiss: () -> Unit,
    onPosterClick: ((MetaPreview) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    NuvioModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTablet) 24.dp else 18.dp)
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = collection.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.profile_insights_collection_count, collection.items.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(if (isTablet) 132.dp else 104.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (isTablet) 640.dp else 520.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(
                    items = collection.items,
                    key = { item -> item.id },
                ) { item ->
                    ProfileInsightPosterTile(
                        item = item,
                        onClick = onPosterClick?.let { callback ->
                            { preview ->
                                onDismiss()
                                callback(preview)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileInsightPosterTile(
    item: ProfileInsightPosterItem,
    onClick: ((MetaPreview) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val unknownLabel = stringResource(Res.string.generic_unknown)
    val initialImageUrl = remember(item.id, item.imageUrl) {
        item.imageUrl?.trim()?.takeIf { it.isNotBlank() }
    }
    val initialReleaseInfo = remember(item.id, item.releaseInfo) {
        item.releaseInfo?.trim()?.takeIf { it.isNotBlank() }
    }
    val cachedMeta = remember(item.id, item.lookupType, item.lookupId) {
        profileCachedMeta(item.lookupType, item.lookupId)
    }
    val cachedImageUrl = remember(item.id, initialImageUrl, cachedMeta) {
        initialImageUrl ?: cachedMeta.profileMetaArtworkUrl()
    }
    val cachedReleaseInfo = remember(item.id, initialReleaseInfo, cachedMeta) {
        initialReleaseInfo ?: cachedMeta?.releaseInfo?.trim()?.takeIf { it.isNotBlank() }
    }
    var resolvedImageUrl by remember(item.id, cachedImageUrl) {
        mutableStateOf(cachedImageUrl)
    }
    var resolvedReleaseInfo by remember(item.id, cachedReleaseInfo) {
        mutableStateOf(cachedReleaseInfo)
    }

    LaunchedEffect(item.id, item.lookupType, item.lookupId, cachedImageUrl, cachedReleaseInfo) {
        resolvedImageUrl = cachedImageUrl
        resolvedReleaseInfo = cachedReleaseInfo
        if (cachedImageUrl != null && cachedReleaseInfo != null) return@LaunchedEffect
        val (artwork, releaseInfo) = profileFetchPosterMetadata(item.lookupType, item.lookupId)
        resolvedImageUrl = cachedImageUrl ?: artwork
        resolvedReleaseInfo = cachedReleaseInfo ?: releaseInfo
    }

    val cleanType = item.lookupType?.trim()?.takeIf { it.isNotBlank() }
    val cleanId = item.lookupId?.trim()?.takeIf { it.isNotBlank() }
    val displayReleaseInfo = resolveReleaseInfoForDisplay(
        stored = initialReleaseInfo,
        hydrated = resolvedReleaseInfo,
        fallback = unknownLabel,
    )
    val detailLine = listOfNotNull(
        displayReleaseInfo,
        item.secondaryText?.trim()?.takeIf { it.isNotBlank() },
    ).joinToString(" • ")

    Column(
        modifier = if (onClick != null && cleanType != null && cleanId != null) {
            Modifier.clickable {
                onClick(
                    MetaPreview(
                        id = cleanId,
                        type = cleanType,
                        name = item.title,
                        poster = resolvedImageUrl,
                        releaseInfo = resolvedReleaseInfo,
                    ),
                )
            }
        } else {
            Modifier
        },
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, tokens.colors.borderSubtle, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (resolvedImageUrl != null) {
                NuvioAsyncImage(
                    imageUrl = resolvedImageUrl.orEmpty(),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    animateIfPossible = true,
                )
            } else {
                Text(
                    text = item.title.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = tokens.colors.textMuted,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelLarge,
            color = tokens.colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = detailLine,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileTasteCard(stats: ProfileInsightsStats) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        if (stats.tasteSegments.isNotEmpty()) {
            ProfileTasteGenreGrid(segments = stats.tasteSegments)
        }
        ProfileTasteBalanceNumbers(stats = stats)
        if (stats.dnaChips.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                stats.dnaChips.forEach { chip ->
                    ProfileTasteDnaChip(text = chip.localizedLabel())
                }
            }
        }
    }
}

@Composable
private fun ProfileTasteGenreGrid(segments: List<ProfileTasteSegment>) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.profile_insights_taste_genres_label),
            style = MaterialTheme.typography.labelMedium,
            color = tokens.colors.textMuted,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            segments.chunked(2).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    row.forEachIndexed { columnIndex, segment ->
                        ProfileTasteGenreTile(
                            segment = segment,
                            rank = rowIndex * 2 + columnIndex,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTasteGenreTile(
    segment: ProfileTasteSegment,
    rank: Int,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    // Ranked opacity instead of one flat tone for every tile: the top genre reads as the
    // dominant signal at a glance, and the grid still feels like a ranking, not a checklist.
    val valueAlpha = when (rank) {
        0 -> 1f
        1 -> 0.7f
        else -> 0.5f
    }
    Column(
        modifier = modifier
            .drawBehind {
                drawLine(
                    color = tokens.colors.borderSubtle,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = segment.label,
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${(segment.share * 100f).roundToInt().coerceIn(1, 100)}%",
            style = MaterialTheme.typography.headlineSmall,
            color = tokens.colors.accent.copy(alpha = valueAlpha),
            fontWeight = FontWeight.Light,
        )
    }
}

@Composable
private fun ProfileTasteBalanceNumbers(stats: ProfileInsightsStats) {
    val tokens = MaterialTheme.nuvio
    val movieLeaning = stats.movieShare >= 0.5f
    val accent = tokens.colors.accent
    val muted = tokens.colors.textMuted.copy(alpha = 0.4f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
    ) {
        ProfileTasteBalanceNumber(
            percent = stats.movieShare,
            label = stringResource(Res.string.profile_insights_type_movie),
            dotColor = if (movieLeaning) accent else muted,
            valueColor = if (movieLeaning) tokens.colors.textPrimary else tokens.colors.textMuted,
        )
        ProfileTasteBalanceNumber(
            percent = 1f - stats.movieShare,
            label = stringResource(Res.string.profile_insights_type_series),
            dotColor = if (movieLeaning) muted else accent,
            valueColor = if (movieLeaning) tokens.colors.textMuted else tokens.colors.textPrimary,
        )
    }
}

@Composable
private fun ProfileTasteBalanceNumber(
    percent: Float,
    label: String,
    dotColor: Color,
    valueColor: Color,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "${(percent * 100f).roundToInt().coerceIn(0, 100)}%",
            style = MaterialTheme.typography.headlineMedium,
            color = valueColor,
            fontWeight = FontWeight.Light,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tokens.colors.textMuted,
            )
        }
    }
}

@Composable
private fun ProfileTasteDnaChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, tokens.colors.borderSubtle, RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.colors.textPrimary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun profileInsightDurationLabel(durationMs: Long): String {
    val totalMinutes = (durationMs / ProfileInsightsMinuteMs).coerceAtLeast(0L)
    if (totalMinutes <= 0L) return stringResource(Res.string.profile_insights_minutes, 0)

    val formatted = if (totalMinutes < 60L) {
        stringResource(Res.string.profile_insights_minutes, totalMinutes.toInt())
    } else {
        val totalHours = (totalMinutes + 30L) / 60L
        val days = totalHours / 24L
        val hours = totalHours % 24L
        when {
            days > 0L && hours > 0L ->
                stringResource(Res.string.profile_insights_days_hours, days.toInt(), hours.toInt())
            days > 0L -> stringResource(Res.string.profile_insights_days, days.toInt())
            else -> stringResource(Res.string.profile_insights_hours, totalHours.toInt())
        }
    }
    return "~$formatted"
}

private fun buildProfileInsightsStats(
    watchProgressState: WatchProgressUiState,
    watchedState: WatchedUiState,
    fullyWatchedSeriesKeys: Set<String>,
    libraryState: LibraryUiState,
    todayIsoDate: String,
): ProfileInsightsStats {
    val now = WatchedClock.nowEpochMs()
    val recentCutoff = now - ProfileInsightsRecentWindowMs
    val progressEntries = watchProgressState.entries
        .filter(WatchProgressEntry::isProfileInsightProgressEntry)
        .map(WatchProgressEntry::normalizedCompletion)
        .distinctBy { entry ->
            listOf(
                entry.parentMetaType,
                entry.parentMetaId,
                entry.videoId,
                entry.seasonNumber,
                entry.episodeNumber,
            ).joinToString("|")
        }
    val continueEntries = progressEntries.profileInsightContinueWatchingEntries()
    val libraryItems = libraryState.items.filter(LibraryItem::isProfileInsightContent)
    val watchedItems = watchedState.items.filter(WatchedItem::isProfileInsightContent)
    val watchedBuckets = buildProfileWatchedContentBuckets(
        watchedItems = watchedItems,
        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
        libraryItems = libraryItems,
        progressEntries = progressEntries,
    )
    val completedContentItems = watchedBuckets.completedItems
    val ongoingSeriesItems = watchedBuckets.ongoingSeriesItems
    val normalizedTypes = libraryItems.map { item -> item.type } +
        progressEntries.map { entry -> entry.parentMetaType } +
        watchedItems.map { item -> item.type }
    val typedCounts = normalizedTypes
        .mapNotNull(String::profileNormalizedType)
        .groupingBy { type -> type }
        .eachCount()
    val movieCount = typedCounts["movie"] ?: 0
    val seriesCount = typedCounts["series"] ?: 0
    val movieSeriesTotal = (movieCount + seriesCount).coerceAtLeast(0)
    val movieShare = if (movieSeriesTotal > 0) {
        movieCount.toFloat() / movieSeriesTotal.toFloat()
    } else {
        0.5f
    }
    val recentActivityCount = profileRecentActivityCount(
        watchedItems = watchedItems,
        progressEntries = progressEntries,
        recentCutoff = recentCutoff,
    )

    return ProfileInsightsStats(
        continueCount = continueEntries.size,
        completedCount = completedContentItems.size,
        ongoingSeriesCount = ongoingSeriesItems.size,
        libraryCount = libraryItems.size,
        trackedDurationMs = profileTrackedDurationMs(
            watchedItems = watchedItems,
            progressEntries = progressEntries,
        ),
        recentActivityCount = recentActivityCount,
        upcomingCount = libraryItems.count { item ->
            item.profileReleaseIsoDate()?.let { releaseDate -> releaseDate >= todayIsoDate } == true
        },
        topGenre = libraryItems.profileTopGenre(),
        topType = normalizedTypes
            .mapNotNull(String::profileNormalizedType)
            .profileMostCommonValue(),
        tasteSegments = libraryItems.profileTopGenreSegments(limit = 3),
        movieShare = movieShare,
        typeBalanceLabel = when {
            movieSeriesTotal == 0 -> ProfileTasteBalanceLabel.Learning
            movieShare >= 0.62f -> ProfileTasteBalanceLabel.MovieLeaning
            movieShare <= 0.38f -> ProfileTasteBalanceLabel.SeriesLeaning
            else -> ProfileTasteBalanceLabel.Balanced
        },
        dnaChips = buildProfileTasteDnaChips(
            libraryCount = libraryItems.size,
            continueCount = continueEntries.size,
            completedCount = completedContentItems.size,
            recentActivityCount = recentActivityCount,
            upcomingCount = libraryItems.count { item ->
                item.profileReleaseIsoDate()?.let { releaseDate -> releaseDate >= todayIsoDate } == true
            },
            movieShare = movieShare,
            movieSeriesTotal = movieSeriesTotal,
        ),
    )
}

private fun buildProfileInsightCollections(
    watchProgressState: WatchProgressUiState,
    watchedState: WatchedUiState,
    fullyWatchedSeriesKeys: Set<String>,
    libraryState: LibraryUiState,
    todayIsoDate: String,
    continueTitle: String,
    completedTitle: String,
    ongoingTitle: String,
    libraryTitle: String,
    upcomingTitle: String,
): Map<ProfileInsightCollectionKind, ProfileInsightCollection> {
    val continueItems = watchProgressState.entries
        .profileInsightContinueWatchingEntries()
        .asSequence()
        .map { entry ->
            ProfileInsightPosterItem(
                id = "continue:${entry.parentMetaId}:${entry.videoId}",
                title = entry.title.trim().takeIf { it.isNotBlank() } ?: entry.parentMetaId,
                secondaryText = entry.profileEpisodeLine(),
                imageUrl = entry.poster ?: entry.episodeThumbnail ?: entry.background,
                lookupType = entry.parentMetaType,
                lookupId = entry.parentMetaId,
            )
        }
        .toList()

    val watchedBuckets = buildProfileWatchedContentBuckets(
        watchedItems = watchedState.items,
        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
        libraryItems = libraryState.items,
        progressEntries = watchProgressState.entries,
    )

    val completedItems = watchedBuckets.completedItems
        .asSequence()
        .map { item ->
            ProfileInsightPosterItem(
                id = "completed:${item.kind}:${item.id}",
                title = item.title,
                releaseInfo = item.releaseInfo,
                imageUrl = item.imageUrl,
                lookupType = item.kind,
                lookupId = item.id,
            )
        }
        .toList()

    val ongoingItems = watchedBuckets.ongoingSeriesItems
        .asSequence()
        .map { item ->
            ProfileInsightPosterItem(
                id = "ongoing:${item.kind}:${item.id}",
                title = item.title,
                releaseInfo = item.releaseInfo,
                imageUrl = item.imageUrl,
                lookupType = item.kind,
                lookupId = item.id,
            )
        }
        .toList()

    val libraryItems = libraryState.items
        .asSequence()
        .filter(LibraryItem::isProfileInsightContent)
        .sortedByDescending { item -> item.savedAtEpochMs }
        .map { item ->
            ProfileInsightPosterItem(
                id = "library:${item.id}:${item.type}",
                title = item.name.trim().takeIf { it.isNotBlank() } ?: item.id,
                releaseInfo = item.releaseInfo?.trim()?.takeIf { it.isNotBlank() },
                imageUrl = item.poster ?: item.banner,
                lookupType = item.type,
                lookupId = item.id,
            )
        }
        .toList()

    val upcomingItems = libraryState.items
        .asSequence()
        .filter(LibraryItem::isProfileInsightContent)
        .filter { item -> item.profileReleaseIsoDate()?.let { releaseDate -> releaseDate >= todayIsoDate } == true }
        .sortedBy { item -> item.profileReleaseIsoDate().orEmpty() }
        .map { item ->
            ProfileInsightPosterItem(
                id = "upcoming:${item.id}:${item.type}",
                title = item.name.trim().takeIf { it.isNotBlank() } ?: item.id,
                releaseInfo = item.releaseInfo?.trim()?.takeIf { it.isNotBlank() },
                imageUrl = item.poster ?: item.banner,
                lookupType = item.type,
                lookupId = item.id,
            )
        }
        .toList()

    return mapOf(
        ProfileInsightCollectionKind.Continue to ProfileInsightCollection(
            title = continueTitle,
            subtitle = "",
            items = continueItems,
        ),
        ProfileInsightCollectionKind.Completed to ProfileInsightCollection(
            title = completedTitle,
            subtitle = "",
            items = completedItems,
        ),
        ProfileInsightCollectionKind.Ongoing to ProfileInsightCollection(
            title = ongoingTitle,
            subtitle = "",
            items = ongoingItems,
        ),
        ProfileInsightCollectionKind.Library to ProfileInsightCollection(
            title = libraryTitle,
            subtitle = "",
            items = libraryItems,
        ),
        ProfileInsightCollectionKind.Upcoming to ProfileInsightCollection(
            title = upcomingTitle,
            subtitle = "",
            items = upcomingItems,
        ),
    )
}

private fun emptyProfileInsightsStats(): ProfileInsightsStats =
    ProfileInsightsStats(
        continueCount = 0,
        completedCount = 0,
        ongoingSeriesCount = 0,
        libraryCount = 0,
        trackedDurationMs = 0L,
        recentActivityCount = 0,
        upcomingCount = 0,
        topGenre = null,
        topType = null,
        tasteSegments = emptyList(),
        movieShare = 0.5f,
        typeBalanceLabel = ProfileTasteBalanceLabel.Learning,
        dnaChips = listOf(ProfileTasteDnaChip.Learning),
    )

private fun emptyProfileInsightCollections(
    continueTitle: String,
    completedTitle: String,
    ongoingTitle: String,
    libraryTitle: String,
    upcomingTitle: String,
): Map<ProfileInsightCollectionKind, ProfileInsightCollection> =
    mapOf(
        ProfileInsightCollectionKind.Continue to ProfileInsightCollection(
            title = continueTitle,
            subtitle = "",
            items = emptyList(),
        ),
        ProfileInsightCollectionKind.Completed to ProfileInsightCollection(
            title = completedTitle,
            subtitle = "",
            items = emptyList(),
        ),
        ProfileInsightCollectionKind.Ongoing to ProfileInsightCollection(
            title = ongoingTitle,
            subtitle = "",
            items = emptyList(),
        ),
        ProfileInsightCollectionKind.Library to ProfileInsightCollection(
            title = libraryTitle,
            subtitle = "",
            items = emptyList(),
        ),
        ProfileInsightCollectionKind.Upcoming to ProfileInsightCollection(
            title = upcomingTitle,
            subtitle = "",
            items = emptyList(),
        ),
    )

private data class ProfileWatchedContentBuckets(
    val completedItems: List<ProfileCompletedContentItem>,
    val ongoingSeriesItems: List<ProfileCompletedContentItem>,
)

private fun buildProfileWatchedContentBuckets(
    watchedItems: List<WatchedItem>,
    fullyWatchedSeriesKeys: Set<String>,
    libraryItems: List<LibraryItem>,
    progressEntries: List<WatchProgressEntry> = emptyList(),
): ProfileWatchedContentBuckets {
    val libraryByContentKey = libraryItems
        .mapNotNull { item ->
            val kind = item.type.profileCompletedContentKind() ?: return@mapNotNull null
            if (!item.isProfileInsightContent()) return@mapNotNull null
            "${kind}:${item.id}" to item
        }
        .toMap()
    val progressByContentKey = progressEntries
        .filter(WatchProgressEntry::isProfileInsightProgressEntry)
        .map(WatchProgressEntry::normalizedCompletion)
        .mapNotNull { entry ->
            val kind = entry.parentMetaType.profileCompletedContentKind() ?: return@mapNotNull null
            if (entry.parentMetaId.isBlank()) return@mapNotNull null
            "${kind}:${entry.parentMetaId}" to entry
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, entries) -> entries.maxByOrNull(WatchProgressEntry::lastUpdatedEpochMs) }

    val eligibleItems = watchedItems
        .asSequence()
        .filter(WatchedItem::isProfileInsightContent)
        .toList()

    val movieItems = eligibleItems
        .asSequence()
        .filter { item -> item.type.profileCompletedContentKind() == "movie" }
        .filterNot { item -> item.season != null || item.episode != null }
        .groupBy { item -> "movie:${item.id}" }
        .mapNotNull { (key, group) ->
            val item = group.maxByOrNull(WatchedItem::markedAtEpochMs) ?: return@mapNotNull null
            val libraryItem = libraryByContentKey[key]
            val progressItem = progressByContentKey[key]
            ProfileCompletedContentItem(
                id = item.id,
                kind = "movie",
                title = item.name.trim().takeIf { it.isNotBlank() }
                    ?: libraryItem?.name?.trim()?.takeIf { it.isNotBlank() }
                    ?: progressItem?.title?.trim()?.takeIf { it.isNotBlank() }
                    ?: item.id,
                releaseInfo = item.releaseInfo?.trim()?.takeIf { it.isNotBlank() }
                    ?: libraryItem?.releaseInfo?.trim()?.takeIf { it.isNotBlank() },
                imageUrl = item.poster
                    ?: libraryItem?.poster
                    ?: libraryItem?.banner
                    ?: progressItem?.profileArtworkUrl()
                    ?: profileCachedArtworkUrl("movie", item.id),
                markedAtEpochMs = item.markedAtEpochMs,
            )
        }

    val completedSeriesItems = mutableListOf<ProfileCompletedContentItem>()
    val ongoingSeriesItems = mutableListOf<ProfileCompletedContentItem>()

    eligibleItems
        .asSequence()
        .filter { item -> item.type.profileCompletedContentKind() == "series" }
        .groupBy { item -> "series:${item.id}" }
        .forEach { (key, group) ->
            val topLevelMarker = group
                .filterNot { item -> item.season != null || item.episode != null }
                .maxByOrNull(WatchedItem::markedAtEpochMs)
            val hasTopLevelSeriesMarker = topLevelMarker != null &&
                !topLevelMarker.type.equals("tv", ignoreCase = true)
            val hasFullyWatchedMarker = hasTopLevelSeriesMarker ||
                group.any { item -> watchedItemKey(item.type, item.id) in fullyWatchedSeriesKeys }
            val libraryItem = libraryByContentKey[key]
            val progressItem = progressByContentKey[key]

            if (hasFullyWatchedMarker) {
                val representative = topLevelMarker ?: group.maxByOrNull(WatchedItem::markedAtEpochMs)
                    ?: return@forEach
                completedSeriesItems += ProfileCompletedContentItem(
                    id = representative.id,
                    kind = "series",
                    title = topLevelMarker?.name?.trim()?.takeIf { it.isNotBlank() }
                        ?: libraryItem?.name?.trim()?.takeIf { it.isNotBlank() }
                        ?: progressItem?.title?.trim()?.takeIf { it.isNotBlank() }
                        ?: representative.name.trim().takeIf { it.isNotBlank() }
                        ?: representative.id,
                    releaseInfo = topLevelMarker?.releaseInfo?.trim()?.takeIf { it.isNotBlank() }
                        ?: libraryItem?.releaseInfo?.trim()?.takeIf { it.isNotBlank() }
                        ?: representative.releaseInfo?.trim()?.takeIf { it.isNotBlank() },
                    imageUrl = topLevelMarker?.poster
                        ?: libraryItem?.poster
                        ?: libraryItem?.banner
                        ?: progressItem?.profileArtworkUrl()
                        ?: representative.poster
                        ?: profileCachedArtworkUrl("series", representative.id),
                    markedAtEpochMs = group.maxOf { item -> item.markedAtEpochMs },
                )
            } else {
                val hasEpisodeActivity = group.any { item -> item.season != null && item.episode != null }
                if (!hasEpisodeActivity) return@forEach
                val representative = group.maxByOrNull(WatchedItem::markedAtEpochMs) ?: return@forEach
                ongoingSeriesItems += ProfileCompletedContentItem(
                    id = representative.id,
                    kind = "series",
                    title = libraryItem?.name?.trim()?.takeIf { it.isNotBlank() }
                        ?: progressItem?.title?.trim()?.takeIf { it.isNotBlank() }
                        ?: representative.name.trim().takeIf { it.isNotBlank() }
                        ?: representative.id,
                    releaseInfo = libraryItem?.releaseInfo?.trim()?.takeIf { it.isNotBlank() }
                        ?: representative.releaseInfo?.trim()?.takeIf { it.isNotBlank() },
                    imageUrl = libraryItem?.poster
                        ?: libraryItem?.banner
                        ?: progressItem?.profileArtworkUrl()
                        ?: representative.poster
                        ?: profileCachedArtworkUrl("series", representative.id),
                    markedAtEpochMs = group.maxOf { item -> item.markedAtEpochMs },
                )
            }
        }

    return ProfileWatchedContentBuckets(
        completedItems = (movieItems + completedSeriesItems).sortedByDescending(ProfileCompletedContentItem::markedAtEpochMs),
        ongoingSeriesItems = ongoingSeriesItems.sortedByDescending(ProfileCompletedContentItem::markedAtEpochMs),
    )
}
private fun WatchProgressEntry.isProfileInsightProgressEntry(): Boolean =
    parentMetaType.profileCompletedContentKind() != null &&
        !parentMetaId.isLikelyProfileLiveTvValue() &&
        !title.isLikelyProfileLiveTvValue()

private fun List<WatchProgressEntry>.profileInsightContinueWatchingEntries(): List<WatchProgressEntry> =
    asSequence()
        .filter(WatchProgressEntry::isProfileInsightProgressEntry)
        .map(WatchProgressEntry::normalizedCompletion)
        .filterNot(WatchProgressEntry::isEffectivelyCompleted)
        .filter { entry ->
            entry.lastPositionMs > 0L || (entry.normalizedProgressPercent ?: 0f) > 0f
        }
        .distinctBy { entry ->
            listOf(
                entry.parentMetaType,
                entry.parentMetaId,
                entry.videoId,
                entry.seasonNumber,
                entry.episodeNumber,
            ).joinToString("|")
        }
        .sortedByDescending(WatchProgressEntry::lastUpdatedEpochMs)
        .toList()

private fun WatchProgressEntry.profileEpisodeLine(): String? {
    val episodeCode = if (seasonNumber != null && episodeNumber != null) {
        "S${seasonNumber}E${episodeNumber}"
    } else {
        null
    }
    val cleanTitle = episodeTitle?.trim()?.takeIf { it.isNotBlank() }
    return when {
        episodeCode != null && cleanTitle != null -> "$episodeCode - $cleanTitle"
        episodeCode != null -> episodeCode
        cleanTitle != null -> cleanTitle
        else -> null
    }
}

private fun WatchProgressEntry.profileTrackedDurationMs(): Long {
    val effectiveDurationMs = if (durationMs > 0L) {
        durationMs
    } else {
        profileFallbackDurationMs(
            kind = parentMetaType.profileCompletedContentKind(),
            isEpisode = isEpisode,
        )
    }
    if (effectiveDurationMs <= 0L) return lastPositionMs.coerceAtLeast(0L)
    if (isEffectivelyCompleted) return effectiveDurationMs
    if (lastPositionMs > 0L) return lastPositionMs.coerceIn(0L, effectiveDurationMs)
    val explicitPercent = normalizedProgressPercent ?: return 0L
    return (effectiveDurationMs * (explicitPercent / 100f)).toLong().coerceIn(0L, effectiveDurationMs)
}

private fun profileTrackedDurationMs(
    watchedItems: List<WatchedItem>,
    progressEntries: List<WatchProgressEntry>,
): Long {
    val durationByKey = mutableMapOf<String, Long>()
    // Built once per distinct show instead of once per watched episode: profileEstimatedDurationMs
    // otherwise re-scans the show's whole episode list for every one of its watched episodes,
    // which is quadratic for a fully-watched series with hundreds of episodes.
    val episodeIndexByMetaId = mutableMapOf<String, Map<Pair<Int?, Int?>, MetaVideo>>()

    fun record(key: String?, durationMs: Long) {
        if (key == null || durationMs <= 0L) return
        if (durationMs > (durationByKey[key] ?: 0L)) {
            durationByKey[key] = durationMs
        }
    }

    progressEntries.forEach { entry ->
        record(entry.profileTrackableActivityKey(), entry.profileTrackedDurationMs())
    }
    watchedItems.forEach { item ->
        record(
            item.profileTrackableActivityKey(),
            item.profileEstimatedDurationMs { meta ->
                episodeIndexByMetaId.getOrPut(meta.id) {
                    meta.videos.associateBy { video -> video.season to video.episode }
                }
            },
        )
    }

    return durationByKey.values.sum()
}

private fun WatchProgressEntry.profileArtworkUrl(): String? =
    poster?.takeIf { it.isNotBlank() }
        ?: background?.takeIf { it.isNotBlank() }
        ?: episodeThumbnail?.takeIf { it.isNotBlank() }

private suspend fun profileFetchPosterMetadata(type: String?, id: String?): Pair<String?, String?> {
    var artwork: String? = null
    var releaseInfo: String? = null

    for ((lookupType, lookupId) in profileMetaLookupCandidates(type, id)) {
        val meta = MetaDetailsRepository.peek(type = lookupType, id = lookupId)
            ?: runCatching {
                MetaDetailsRepository.fetch(type = lookupType, id = lookupId)
            }.onFailure { error ->
                profileInsightsLog.w(error) {
                    "Failed to hydrate profile metadata for $lookupType/$lookupId"
                }
            }.getOrNull()

        if (meta != null) {
            artwork = artwork ?: meta.profileMetaArtworkUrl()
            releaseInfo = releaseInfo ?: meta.releaseInfo?.trim()?.takeIf { it.isNotBlank() }
            if (artwork != null && releaseInfo != null) {
                return artwork to releaseInfo
            }
        }
    }

    return artwork to releaseInfo
}

private fun profileCachedArtworkUrl(type: String?, id: String?): String? {
    for ((lookupType, lookupId) in profileMetaLookupCandidates(type, id)) {
        MetaDetailsRepository.peek(type = lookupType, id = lookupId)
            .profileMetaArtworkUrl()
            ?.let { return it }
    }

    return null
}

private fun MetaDetails?.profileMetaArtworkUrl(): String? =
    this?.poster?.trim()?.takeIf { it.isNotBlank() }
        ?: this?.background?.trim()?.takeIf { it.isNotBlank() }

private fun profileMetaLookupCandidates(type: String?, id: String?): List<Pair<String, String>> {
    val cleanId = id?.trim()?.takeIf { it.isNotBlank() } ?: return emptyList()
    val cleanType = type?.trim()?.takeIf { it.isNotBlank() } ?: return emptyList()
    val normalizedKind = cleanType.profileCompletedContentKind()

    val typeCandidates = buildList {
        add(cleanType)
        normalizedKind?.let(::add)
        when (normalizedKind) {
            "movie" -> add("film")
            "series" -> {
                add("tv")
                add("show")
                add("tvshow")
            }
        }
    }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }

    return typeCandidates.map { candidateType -> candidateType to cleanId }
}

private fun profileRecentActivityCount(
    watchedItems: List<WatchedItem>,
    progressEntries: List<WatchProgressEntry>,
    recentCutoff: Long,
): Int = buildSet {
    watchedItems
        .asSequence()
        .filter { item -> item.markedAtEpochMs >= recentCutoff }
        .mapNotNull(WatchedItem::profileActivityKey)
        .forEach(::add)
    progressEntries
        .asSequence()
        .filter { entry -> entry.lastUpdatedEpochMs >= recentCutoff }
        .mapNotNull(WatchProgressEntry::profileActivityKey)
        .forEach(::add)
}.size

private fun WatchedItem.profileActivityKey(): String? {
    if (!isProfileTrackableActivity()) return null
    val kind = type.profileCompletedContentKind() ?: return null
    val contentId = id.trim().takeIf { it.isNotBlank() } ?: return null
    return "$kind:$contentId:${season ?: -1}:${episode ?: -1}"
}

private fun WatchProgressEntry.profileActivityKey(): String? {
    if (!isProfileTrackableActivity()) return null
    val kind = parentMetaType.profileCompletedContentKind() ?: return null
    val contentId = parentMetaId.trim().takeIf { it.isNotBlank() } ?: return null
    return "$kind:$contentId:${seasonNumber ?: -1}:${episodeNumber ?: -1}"
}

private fun WatchedItem.profileTrackableActivityKey(): String? = profileActivityKey()

private fun WatchProgressEntry.profileTrackableActivityKey(): String? = profileActivityKey()

private fun WatchedItem.isProfileTrackableActivity(): Boolean {
    val kind = type.profileCompletedContentKind() ?: return false
    return kind == "movie" || (kind == "series" && season != null && episode != null)
}

private fun WatchProgressEntry.isProfileTrackableActivity(): Boolean {
    val kind = parentMetaType.profileCompletedContentKind() ?: return false
    return kind == "movie" || (kind == "series" && seasonNumber != null && episodeNumber != null)
}

private fun WatchedItem.profileEstimatedDurationMs(
    episodeIndexFor: (MetaDetails) -> Map<Pair<Int?, Int?>, MetaVideo>,
): Long {
    val kind = type.profileCompletedContentKind() ?: return 0L
    val meta = profileCachedMeta(type, id)
    val minutes = when {
        kind == "movie" && season == null && episode == null ->
            meta?.runtime?.let(::profileParseRuntimeMinutes)?.toLong()
                ?: ProfileInsightsFallbackMovieMinutes
        kind == "series" && season != null && episode != null ->
            meta?.let(episodeIndexFor)
                ?.get(season to episode)
                ?.runtime
                ?.takeIf { runtime -> runtime > 0 }
                ?.toLong()
                ?: ProfileInsightsFallbackEpisodeMinutes
        else -> return 0L
    }
    return minutes * ProfileInsightsMinuteMs
}

private fun profileFallbackDurationMs(kind: String?, isEpisode: Boolean): Long = when {
    kind == null -> 0L
    kind == "movie" && !isEpisode -> ProfileInsightsFallbackMovieMinutes * ProfileInsightsMinuteMs
    kind == "series" && isEpisode -> ProfileInsightsFallbackEpisodeMinutes * ProfileInsightsMinuteMs
    else -> 0L
}

private fun profileCachedMeta(type: String?, id: String?): MetaDetails? {
    for ((lookupType, lookupId) in profileMetaLookupCandidates(type, id)) {
        MetaDetailsRepository.peek(type = lookupType, id = lookupId)?.let { return it }
    }
    return null
}

private fun profileParseRuntimeMinutes(value: String?): Int? {
    val runtime = value?.trim()?.takeIf { it.isNotBlank() } ?: return null

    profileHourMinuteColonRegex.matchEntire(runtime)?.let { match ->
        val hours = match.groupValues[1].toIntOrNull() ?: return null
        val minutes = match.groupValues[2].toIntOrNull() ?: return null
        return ((hours * 60) + minutes).coerceAtLeast(0)
    }

    val hoursToken = profileHourTokenRegex.find(runtime)?.groupValues?.getOrNull(1)?.toIntOrNull()
    val minutesToken = profileMinuteTokenRegex.find(runtime)?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (hoursToken != null || minutesToken != null) {
        return (((hoursToken ?: 0).coerceAtLeast(0) * 60) + (minutesToken ?: 0).coerceAtLeast(0))
    }

    return profileDigitsOnlyRegex.matchEntire(runtime)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?.coerceAtLeast(0)
}

private fun List<LibraryItem>.profileTopGenre(): String? =
    asSequence()
        .flatMap { item -> item.genres.asSequence() }
        .map { genre -> genre.trim() }
        .filter { genre -> genre.isNotBlank() }
        .groupingBy { genre -> genre }
        .eachCount()
        .maxByOrNull { (_, count) -> count }
        ?.key

private fun List<LibraryItem>.profileTopGenreSegments(limit: Int): List<ProfileTasteSegment> {
    val counts = asSequence()
        .flatMap { item -> item.genres.asSequence() }
        .map { genre -> genre.trim() }
        .filter { genre -> genre.isNotBlank() }
        .groupingBy { genre -> genre }
        .eachCount()
        .toList()
        .sortedByDescending { (_, count) -> count }
    val total = counts.sumOf { (_, count) -> count }.coerceAtLeast(1)
    return counts
        .take(limit)
        .map { (genre, count) ->
            ProfileTasteSegment(
                label = genre,
                share = count.toFloat() / total.toFloat(),
            )
        }
}

private fun buildProfileTasteDnaChips(
    libraryCount: Int,
    continueCount: Int,
    completedCount: Int,
    recentActivityCount: Int,
    upcomingCount: Int,
    movieShare: Float,
    movieSeriesTotal: Int,
): List<ProfileTasteDnaChip> = buildList {
    when {
        movieSeriesTotal == 0 -> add(ProfileTasteDnaChip.Learning)
        movieShare >= 0.62f -> add(ProfileTasteDnaChip.MovieLeaning)
        movieShare <= 0.38f -> add(ProfileTasteDnaChip.SeriesLeaning)
        else -> add(ProfileTasteDnaChip.Balanced)
    }
    if (continueCount >= 3) add(ProfileTasteDnaChip.BingeReady)
    if (recentActivityCount >= 5) add(ProfileTasteDnaChip.HighActivity)
    if (libraryCount >= 12) add(ProfileTasteDnaChip.Collector)
    if (upcomingCount > 0) add(ProfileTasteDnaChip.RadarWatcher)
    if (completedCount >= 10) add(ProfileTasteDnaChip.Completionist)
}.distinct().take(4)

private fun LibraryItem.profileReleaseIsoDate(): String? =
    releaseInfo.profileExtractIsoDate()

private fun String?.profileExtractIsoDate(): String? {
    val value = this?.trim().orEmpty()
    if (value.isBlank()) return null

    if (value.length >= 10) {
        for (start in 0..(value.length - 10)) {
            val candidate = value.substring(start, start + 10)
            if (candidate.isIsoDateCandidate()) return candidate
        }
    }

    val normalized = value
        .replace(',', ' ')
        .replace('.', ' ')
        .replace('/', ' ')
        .replace('-', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
    val tokens = normalized.split(' ').filter(String::isNotBlank)
    val yearIndex = tokens.indexOfFirst { token ->
        token.length == 4 && token.all(Char::isDigit) && token.toIntOrNull() in 1000..9999
    }
    if (yearIndex < 0) return null

    val year = tokens[yearIndex].toInt()
    val monthBefore = tokens.getOrNull(yearIndex - 1)?.profileMonthNumber()
    val monthAfter = tokens.getOrNull(yearIndex + 1)?.profileMonthNumber()
    val month = monthBefore ?: monthAfter ?: 12
    val dayBefore = tokens.getOrNull(yearIndex - 1)?.toIntOrNull()?.takeIf { it in 1..31 }
    val dayAfterOne = tokens.getOrNull(yearIndex + 1)?.toIntOrNull()?.takeIf { it in 1..31 }
    val dayAfterTwo = tokens.getOrNull(yearIndex + 2)?.toIntOrNull()?.takeIf { it in 1..31 }
    val day = when {
        monthBefore != null -> dayBefore ?: 1
        monthAfter != null -> dayAfterTwo ?: 1
        else -> dayAfterOne ?: 31
    }.coerceAtMost(profileDaysInMonth(year, month))

    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

private fun String.isIsoDateCandidate(): Boolean =
    length == 10 &&
        this[4] == '-' &&
        this[7] == '-' &&
        take(4).all(Char::isDigit) &&
        substring(5, 7).all(Char::isDigit) &&
        substring(8, 10).all(Char::isDigit)

private fun String.profileMonthNumber(): Int? =
    when (trim().lowercase().take(3)) {
        "jan", "oca" -> 1
        "feb", "şub", "sub" -> 2
        "mar" -> 3
        "apr", "nis" -> 4
        "may", "mai" -> 5
        "jun", "haz" -> 6
        "jul", "tem" -> 7
        "aug", "ağu", "agu" -> 8
        "sep", "eyl" -> 9
        "oct", "eki" -> 10
        "nov", "kas" -> 11
        "dec", "ara" -> 12
        else -> null
    }

private fun profileDaysInMonth(year: Int, month: Int): Int =
    when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
        else -> 31
    }

private fun String.profileNormalizedType(): String? =
    when (trim().lowercase()) {
        "movie", "film" -> "movie"
        "series", "show", "tv", "tvshow", "anime" -> "series"
        "" -> null
        else -> trim().lowercase()
    }

private fun String.profileCompletedContentKind(): String? =
    when (trim().lowercase()) {
        "live-tv", "livetv", "live_tv", "channel", "tv-channel", "tv_channel", "iptv", "m3u", "stalker" -> null
        "movie", "film" -> "movie"
        "series", "show", "tv", "tvshow", "anime" -> "series"
        else -> null
    }

private fun LibraryItem.isProfileInsightContent(): Boolean =
    type.profileCompletedContentKind() != null &&
        !id.isLikelyProfileLiveTvValue() &&
        !name.isLikelyProfileLiveTvValue()

private fun WatchedItem.isProfileInsightContent(): Boolean =
    type.profileCompletedContentKind() != null &&
        !id.isLikelyProfileLiveTvValue() &&
        !name.isLikelyProfileLiveTvValue()

private fun String.isLikelyProfileLiveTvValue(): Boolean {
    val value = trim().lowercase()
    return value.startsWith("http://") ||
        value.startsWith("https://") ||
        value.startsWith("rtmp://") ||
        value.startsWith("rtsp://") ||
        value.endsWith(".m3u") ||
        value.endsWith(".m3u8")
}

private fun List<String>.profileMostCommonValue(): String? =
    filter { value -> value.isNotBlank() }
        .groupingBy { value -> value }
        .eachCount()
        .maxByOrNull { (_, count) -> count }
        ?.key

private fun String.fallbackDisplayLabel(): String {
    val clean = trim()
    if (clean.isBlank()) return clean
    return clean.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase() else char.toString()
    }
}

private data class ProfileInsightsStats(
    val continueCount: Int,
    val completedCount: Int,
    val ongoingSeriesCount: Int,
    val libraryCount: Int,
    val trackedDurationMs: Long,
    val recentActivityCount: Int,
    val upcomingCount: Int,
    val topGenre: String?,
    val topType: String?,
    val tasteSegments: List<ProfileTasteSegment>,
    val movieShare: Float,
    val typeBalanceLabel: ProfileTasteBalanceLabel,
    val dnaChips: List<ProfileTasteDnaChip>,
)


private enum class ProfileInsightCollectionKind {
    Continue,
    Completed,
    Ongoing,
    Library,
    Upcoming,
}

private data class ProfileInsightCollection(
    val title: String,
    val subtitle: String,
    val items: List<ProfileInsightPosterItem>,
)

private data class ProfileInsightPosterItem(
    val id: String,
    val title: String,
    val secondaryText: String? = null,
    val releaseInfo: String? = null,
    val imageUrl: String?,
    val lookupType: String? = null,
    val lookupId: String? = null,
)

private data class ProfileCompletedContentItem(
    val id: String,
    val kind: String,
    val title: String,
    val releaseInfo: String?,
    val imageUrl: String?,
    val markedAtEpochMs: Long,
)

private data class ProfileTasteSegment(
    val label: String,
    val share: Float,
)

private enum class ProfileTasteBalanceLabel {
    Learning,
    MovieLeaning,
    SeriesLeaning,
    Balanced,
}

private enum class ProfileTasteDnaChip {
    Learning,
    MovieLeaning,
    SeriesLeaning,
    Balanced,
    BingeReady,
    HighActivity,
    Collector,
    RadarWatcher,
    Completionist,
}

@Composable
private fun ProfileTasteBalanceLabel.localizedLabel(): String =
    when (this) {
        ProfileTasteBalanceLabel.Learning -> stringResource(Res.string.profile_insights_taste_balance_learning)
        ProfileTasteBalanceLabel.MovieLeaning -> stringResource(Res.string.profile_insights_taste_balance_movie)
        ProfileTasteBalanceLabel.SeriesLeaning -> stringResource(Res.string.profile_insights_taste_balance_series)
        ProfileTasteBalanceLabel.Balanced -> stringResource(Res.string.profile_insights_taste_balance_balanced)
    }

@Composable
private fun ProfileTasteDnaChip.localizedLabel(): String =
    when (this) {
        ProfileTasteDnaChip.Learning -> stringResource(Res.string.profile_insights_taste_chip_learning)
        ProfileTasteDnaChip.MovieLeaning -> stringResource(Res.string.profile_insights_taste_chip_movie)
        ProfileTasteDnaChip.SeriesLeaning -> stringResource(Res.string.profile_insights_taste_chip_series)
        ProfileTasteDnaChip.Balanced -> stringResource(Res.string.profile_insights_taste_chip_balanced)
        ProfileTasteDnaChip.BingeReady -> stringResource(Res.string.profile_insights_taste_chip_binge)
        ProfileTasteDnaChip.HighActivity -> stringResource(Res.string.profile_insights_taste_chip_active)
        ProfileTasteDnaChip.Collector -> stringResource(Res.string.profile_insights_taste_chip_collector)
        ProfileTasteDnaChip.RadarWatcher -> stringResource(Res.string.profile_insights_taste_chip_radar)
        ProfileTasteDnaChip.Completionist -> stringResource(Res.string.profile_insights_taste_chip_completionist)
    }

private const val ProfileInsightsMinuteMs = 60_000L
private const val ProfileInsightsRecentWindowMs = 7L * 24L * 60L * 60L * 1000L

private const val ProfileInsightsFallbackMovieMinutes = 115L
private const val ProfileInsightsFallbackEpisodeMinutes = 42L
private val profileHourTokenRegex = Regex("""(?i)(\d+)\s*h(?:ours?)?""")
private val profileMinuteTokenRegex = Regex("""(?i)(\d+)\s*m(?:in(?:ute)?s?)?""")
private val profileHourMinuteColonRegex = Regex("""^\s*(\d+)\s*:\s*(\d{1,2})\s*$""")
private val profileDigitsOnlyRegex = Regex("""^\s*(\d+)\s*$""")
