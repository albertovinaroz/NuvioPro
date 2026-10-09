package com.nuvio.app.features.settings

import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nuvio.app.core.build.AppFeaturePolicy
import nuvio.composeapp.generated.resources.support_pro_title
import com.nuvio.app.core.ui.labelRes
import com.nuvio.app.features.downloads.DownloadsRepository
import com.nuvio.app.features.tracking.TrackingProviderId
import com.nuvio.app.features.tracking.TrackingProviderRegistry
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioAsyncImage
import com.nuvio.app.core.ui.ThemeAccentRing
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.profiles.AvatarRepository
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.profiles.parseHexColor
import com.nuvio.app.features.profiles.profileAvatarImageUrl
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.nuvio.app.core.build.AppVersionConfig
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.whats_new_settings_description
import nuvio.composeapp.generated.resources.whats_new_title
import nuvio.composeapp.generated.resources.compose_about_made_with
import nuvio.composeapp.generated.resources.compose_about_version_format
import nuvio.composeapp.generated.resources.compose_settings_page_account
import nuvio.composeapp.generated.resources.compose_settings_page_advanced
import nuvio.composeapp.generated.resources.compose_settings_page_appearance
import nuvio.composeapp.generated.resources.compose_settings_page_integrations
import nuvio.composeapp.generated.resources.compose_settings_page_licenses_attributions
import nuvio.composeapp.generated.resources.compose_settings_page_notifications
import nuvio.composeapp.generated.resources.compose_settings_root_downloads_description
import nuvio.composeapp.generated.resources.compose_settings_root_downloads_title
import nuvio.composeapp.generated.resources.compose_settings_page_playback
import nuvio.composeapp.generated.resources.compose_settings_page_privacy_policy
import nuvio.composeapp.generated.resources.compose_settings_page_supporters_contributors
import nuvio.composeapp.generated.resources.compose_settings_root_account_description
import nuvio.composeapp.generated.resources.compose_settings_root_appearance_description
import nuvio.composeapp.generated.resources.compose_settings_root_check_updates_description
import nuvio.composeapp.generated.resources.compose_settings_root_check_updates_title
import nuvio.composeapp.generated.resources.compose_settings_root_content_discovery_description
import nuvio.composeapp.generated.resources.compose_settings_root_general_section
import nuvio.composeapp.generated.resources.compose_settings_root_integrations_description
import nuvio.composeapp.generated.resources.compose_settings_root_notifications_description
import nuvio.composeapp.generated.resources.compose_settings_root_profile_description
import nuvio.composeapp.generated.resources.compose_settings_root_profile_title
import nuvio.composeapp.generated.resources.compose_settings_root_privacy_policy_description
import nuvio.composeapp.generated.resources.compose_settings_root_switch_profile_description
import nuvio.composeapp.generated.resources.compose_settings_root_switch_profile_title
import nuvio.composeapp.generated.resources.compose_settings_root_tracking_description
import nuvio.composeapp.generated.resources.compose_settings_root_about_section
import nuvio.composeapp.generated.resources.compose_settings_root_account_section
import nuvio.composeapp.generated.resources.compose_settings_root_advanced_description
import nuvio.composeapp.generated.resources.compose_settings_root_advanced_section
import nuvio.composeapp.generated.resources.compose_settings_page_content_discovery
import nuvio.composeapp.generated.resources.compose_settings_page_tracking
import nuvio.composeapp.generated.resources.settings_playback_subtitle
import nuvio.composeapp.generated.resources.updates_debug_test_description
import nuvio.composeapp.generated.resources.updates_debug_test_title
import nuvio.composeapp.generated.resources.about_supporters_contributors_subtitle
import nuvio.composeapp.generated.resources.about_licenses_attributions_subtitle
import org.jetbrains.compose.resources.stringResource

private const val PRIVACY_POLICY_URL = "https://nuvio.tv/privacy-policy"
private val RootSectionExtraGap = 12.dp

internal fun LazyListScope.settingsRootContent(
    isTablet: Boolean,
    onPlaybackClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onAdvancedClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onContentDiscoveryClick: () -> Unit,
    onIntegrationsClick: () -> Unit,
    onTrackingClick: () -> Unit,
    onSupportersContributorsClick: () -> Unit,
    onLicensesAttributionsClick: () -> Unit,
    onCheckForUpdatesClick: (() -> Unit)? = null,
    onTestUpdateBannerClick: (() -> Unit)? = null,
    onWhatsNewClick: () -> Unit = {},
    onAccountClick: () -> Unit,
    onSwitchProfileClick: (() -> Unit)? = null,
    showAccountSection: Boolean = true,
    showGeneralSection: Boolean = true,
    showAboutSection: Boolean = true,
    showAdvancedSection: Boolean = true,
    showSupportersContributorsPage: Boolean = true,
) {
    // Every card after the first gets one more list gap above it, so the root page separates its
    // groups the way iOS Settings does; sub-pages keep the standard spacing.
    var isFirstSection = true
    fun nextSectionModifier(): Modifier {
        val modifier = if (isFirstSection) Modifier else Modifier.padding(top = RootSectionExtraGap)
        isFirstSection = false
        return modifier
    }
    if (showAccountSection) {
        if (onSwitchProfileClick != null) {
            val sectionModifier1 = nextSectionModifier()
            item {
                SettingsSection(
                    title = null,
                    isTablet = isTablet,
                    modifier = sectionModifier1,
                ) {
                    SettingsGroup(isTablet = isTablet) {
                        SettingsProfileCardRow(isTablet = isTablet, onClick = onSwitchProfileClick)
                    }
                }
            }
        }
        val sectionModifier2 = nextSectionModifier()
        item {
            val connectedTracking by TrackingProviderRegistry.connectedProviderIds.collectAsStateWithLifecycle()
            val trackingValue = TrackingProviderId.entries
                .filter { it in connectedTracking }
                .joinToString(", ") { it.displayName }
                .ifBlank { null }
            SettingsSection(
                title = null,
                isTablet = isTablet,
                modifier = sectionModifier2,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_page_account),
                        icon = Icons.Rounded.AccountCircle,
                        iconTile = true,
                        isTablet = isTablet,
                        onClick = onAccountClick,
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_page_tracking),
                        icon = Icons.Default.Sync,
                        iconTile = true,
                        value = trackingValue,
                        isTablet = isTablet,
                        onClick = onTrackingClick,
                    )
                }
            }
        }
    }
    if (showGeneralSection) {
        val sectionModifier3 = nextSectionModifier()
        item {
            val selectedTheme by remember {
                ThemeSettingsRepository.ensureLoaded()
                ThemeSettingsRepository.selectedTheme
            }.collectAsStateWithLifecycle()
            val downloads by remember {
                DownloadsRepository.ensureLoaded()
                DownloadsRepository.uiState
            }.collectAsStateWithLifecycle()
            val downloadsValue = downloads.completedItems.size.takeIf { it > 0 }?.toString()
            SettingsSection(
                title = null,
                isTablet = isTablet,
                modifier = sectionModifier3,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_page_appearance),
                        icon = Icons.Rounded.Palette,
                        iconTile = true,
                        value = stringResource(selectedTheme.labelRes),
                        isTablet = isTablet,
                        onClick = onAppearanceClick,
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_page_content_discovery),
                        icon = Icons.Rounded.Extension,
                        iconTile = true,
                        isTablet = isTablet,
                        onClick = onContentDiscoveryClick,
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_page_playback),
                        icon = Icons.Rounded.PlayArrow,
                        iconTile = true,
                        isTablet = isTablet,
                        onClick = onPlaybackClick,
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_page_integrations),
                        icon = Icons.Rounded.Link,
                        iconTile = true,
                        isTablet = isTablet,
                        onClick = onIntegrationsClick,
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_page_notifications),
                        icon = Icons.Rounded.Notifications,
                        iconTile = true,
                        isTablet = isTablet,
                        onClick = onNotificationsClick,
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_root_downloads_title),
                        icon = Icons.Rounded.CloudDownload,
                        iconTile = true,
                        value = downloadsValue,
                        isTablet = isTablet,
                        onClick = onDownloadsClick,
                    )
                }
            }
        }
    }
    if (showAboutSection) {
        val sectionModifier4 = nextSectionModifier()
        item {
            val uriHandler = LocalUriHandler.current
            var showSupportSheet by remember { mutableStateOf(false) }
            if (showSupportSheet) {
                SupportProSheet(onDismiss = { showSupportSheet = false })
            }
            SettingsSection(
                title = null,
                isTablet = isTablet,
                modifier = sectionModifier4,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    // Only where donation links are allowed (not App Store / Play Store builds).
                    if (AppFeaturePolicy.donationActionsEnabled) {
                        SettingsNavigationRow(
                            title = stringResource(Res.string.support_pro_title),
                            icon = Icons.Rounded.LocalCafe,
                            iconTile = true,
                            isTablet = isTablet,
                            onClick = { showSupportSheet = true },
                        )
                        SettingsGroupDivider(isTablet = isTablet)
                    }
                    if (showSupportersContributorsPage) {
                        SettingsNavigationRow(
                            title = stringResource(Res.string.compose_settings_page_supporters_contributors),
                            icon = Icons.Rounded.Favorite,
                            iconTile = true,
                            isTablet = isTablet,
                            onClick = onSupportersContributorsClick,
                        )
                        SettingsGroupDivider(isTablet = isTablet)
                    }
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_page_privacy_policy),
                        icon = Icons.Rounded.Policy,
                        iconTile = true,
                        isTablet = isTablet,
                        onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_page_licenses_attributions),
                        icon = Icons.Rounded.Info,
                        iconTile = true,
                        isTablet = isTablet,
                        onClick = onLicensesAttributionsClick,
                    )
                    if (onCheckForUpdatesClick != null) {
                        SettingsGroupDivider(isTablet = isTablet)
                        SettingsNavigationRow(
                            title = stringResource(Res.string.compose_settings_root_check_updates_title),
                            icon = Icons.Rounded.CloudDownload,
                            iconTile = true,
                            isTablet = isTablet,
                            onClick = onCheckForUpdatesClick,
                        )
                    }
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = stringResource(Res.string.whats_new_title),
                        icon = Icons.Rounded.NewReleases,
                        iconTile = true,
                        isTablet = isTablet,
                        onClick = onWhatsNewClick,
                    )
                    if (onTestUpdateBannerClick != null) {
                        SettingsGroupDivider(isTablet = isTablet)
                        SettingsNavigationRow(
                            title = stringResource(Res.string.updates_debug_test_title),
                            icon = Icons.Rounded.BugReport,
                            iconTile = true,
                            isTablet = isTablet,
                            onClick = onTestUpdateBannerClick,
                        )
                    }
                }
            }
        }
    }
    if (showAdvancedSection) {
        val sectionModifier5 = nextSectionModifier()
        item {
            SettingsSection(
                title = null,
                isTablet = isTablet,
                modifier = sectionModifier5,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_page_advanced),
                        icon = Icons.Rounded.Tune,
                        iconTile = true,
                        isTablet = isTablet,
                        onClick = onAdvancedClick,
                    )
                }
            }
        }
    }
    item {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = if (isTablet) 20.dp else 16.dp),
        ) {
            if (showAboutSection) {
                MemberBrandWordmark(
                    height = if (isTablet) 30.dp else 26.dp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.height(if (isTablet) 10.dp else 8.dp),
                )
            }
            Text(
                text = stringResource(Res.string.compose_about_made_with),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(
                    Res.string.compose_about_version_format,
                    AppVersionConfig.VERSION_NAME,
                    AppVersionConfig.VERSION_CODE,
                ),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The active profile as its own card at the top of Settings — avatar, name and a one-line summary,
 * like the account card heading the iOS Settings app — in place of a plain "Profile" row.
 */
@Composable
private fun SettingsProfileCardRow(isTablet: Boolean, onClick: () -> Unit) {
    val tokens = MaterialTheme.nuvio
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val profile = profileState.activeProfile
    val avatars by AvatarRepository.avatars.collectAsStateWithLifecycle()
    val avatarItem = remember(profile?.avatarId, avatars) {
        profile?.avatarId?.let { id -> avatars.find { it.id == id } }
    }
    val avatarImageUrl = remember(profile, avatarItem) { profile?.let { profileAvatarImageUrl(it, avatarItem) } }
    val avatarColor = remember(profile?.avatarColorHex) {
        profile?.avatarColorHex?.let(::parseHexColor)
    } ?: tokens.colors.accent
    val avatarBackground = avatarItem?.bgColor?.let(::parseHexColor) ?: avatarColor
    val name = profile?.name?.takeIf { it.isNotBlank() }
        ?: stringResource(Res.string.compose_settings_root_profile_title)
    val avatarSize = if (isTablet) 64.dp else 58.dp
    val ringSize = avatarSize + 8.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .settingsRowClickable(onClick = onClick)
            .padding(horizontal = if (isTablet) 20.dp else 16.dp, vertical = if (isTablet) 16.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(ringSize), contentAlignment = Alignment.Center) {
        ThemeAccentRing(modifier = Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(if (avatarImageUrl != null) avatarBackground else avatarColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarImageUrl != null) {
                NuvioAsyncImage(
                    imageUrl = avatarImageUrl,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    animateIfPossible = true,
                )
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = avatarColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(Res.string.compose_settings_root_profile_description),
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = tokens.colors.textMuted,
            modifier = Modifier.size(if (isTablet) 22.dp else 20.dp),
        )
    }
}
