package com.nuvio.app.features.settings

import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.NuvioActionLabel
import com.nuvio.app.core.ui.NuvioBackButton
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.gradientMask
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.core.ui.nuvioConsumePointerEvents
import com.nuvio.app.features.home.HomeCatalogSettingsItem
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_homescreen_collection_with_addon
import nuvio.composeapp.generated.resources.settings_homescreen_display_name
import nuvio.composeapp.generated.resources.settings_homescreen_hero_source
import nuvio.composeapp.generated.resources.settings_homescreen_hidden
import nuvio.composeapp.generated.resources.settings_homescreen_landscape_mode
import nuvio.composeapp.generated.resources.settings_homescreen_landscape_mode_description
import nuvio.composeapp.generated.resources.settings_homescreen_not_in_hero
import nuvio.composeapp.generated.resources.settings_homescreen_pinned
import nuvio.composeapp.generated.resources.settings_homescreen_pinned_to_top
import nuvio.composeapp.generated.resources.settings_homescreen_reorder
import nuvio.composeapp.generated.resources.settings_homescreen_top10_number_filled
import nuvio.composeapp.generated.resources.settings_homescreen_top10_number_outlined
import nuvio.composeapp.generated.resources.settings_homescreen_top10_orientation_landscape
import nuvio.composeapp.generated.resources.settings_homescreen_top10_orientation_portrait
import nuvio.composeapp.generated.resources.settings_homescreen_top10_style
import nuvio.composeapp.generated.resources.settings_homescreen_top10_style_description
import nuvio.composeapp.generated.resources.settings_homescreen_top10_style_on
import nuvio.composeapp.generated.resources.settings_homescreen_visible
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope

@Composable
private fun SettingsCard(
    isTablet: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = tokens.colors.surface,
        // Noticeably rounder than the shared compactCard token (12dp), matching the pronounced
        // corner radius native iOS uses for grouped Settings sections — reuses the same xxl
        // radius the app's own sheets/dialogs already use, scoped to just this card rather than
        // bumping compactCard itself, which many other surfaces share.
        shape = RoundedCornerShape(NuvioTokens.Radius.xxl),
        border = BorderStroke(
            tokens.borders.hairline,
            tokens.colors.borderSubtle,
        ),
    ) {
        Column(content = content)
    }
}

@Composable
internal fun SettingsGroup(
    isTablet: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsCard(
        isTablet = isTablet,
        modifier = modifier,
    ) {
        Column(content = content)
    }
}

@Composable
internal fun SettingsGroupDivider(isTablet: Boolean) {
    val tokens = MaterialTheme.nuvio
    HorizontalDivider(
        modifier = Modifier.padding(start = if (isTablet) NuvioTokens.Space.s80 - NuvioTokens.Space.s2 else NuvioTokens.Space.s64 + NuvioTokens.Space.s2),
        thickness = tokens.borders.hairline,
        color = tokens.colors.borderSubtle,
    )
}

@Composable
internal fun TabletPageHeader(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .nuvioConsumePointerEvents(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.listGap),
        ) {
            if (showBack) {
                NuvioBackButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp),
                    shape = tokens.shapes.compactCard,
                    containerColor = tokens.colors.surface,
                    contentColor = tokens.colors.textPrimary,
                    buttonSize = NuvioTokens.Space.s36,
                    iconSize = tokens.icons.md,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun SettingsSidebarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val primary = tokens.colors.accent
    val background = if (selected) primary.copy(alpha = tokens.opacity.hover) else Color.Transparent
    val iconChip = if (selected) primary.copy(alpha = tokens.opacity.selected) else Color.Transparent
    val contentColor = if (selected) tokens.colors.textPrimary else tokens.colors.textMuted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacing.listGap, vertical = NuvioTokens.Space.s2)
            .background(background, RoundedCornerShape(NuvioTokens.Space.s10))
            .clickable(onClick = onClick)
            .padding(horizontal = tokens.spacing.screenHorizontal, vertical = tokens.spacing.listGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(tokens.icons.xl),
            color = iconChip,
            shape = RoundedCornerShape(NuvioTokens.Radius.md),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = if (selected) {
                        Modifier.gradientMask(MaterialTheme.themePalette.accentBrush())
                    } else {
                        Modifier
                    },
                    tint = if (selected) primary else contentColor,
                )
            }
        }
        Spacer(modifier = Modifier.width(tokens.spacing.listGap))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
internal fun SettingsSection(
    // Null hides the label row entirely — used on the Settings root page, which groups its
    // cards purely by spacing (no "ACCOUNT" / "GENERAL" headers), matching native iOS Settings.
    // Sub-pages that pass a real title (e.g. "CREDENTIALS", "LOCALIZATION") are unaffected.
    title: String?,
    isTablet: Boolean,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Column(modifier = modifier) {
        if (title != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NuvioSectionLabel(text = title)
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
            Spacer(modifier = Modifier.height(if (isTablet) tokens.spacing.listGap else NuvioTokens.Space.s10))
        }
        content()
    }
}

@Composable
internal fun SettingsClickableRow(
    title: String,
    enabled: Boolean = true,
    isTablet: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    val horizontalPadding = if (isTablet) 20.dp else 16.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            .alpha(if (enabled) NuvioTokens.Opacity.visible else tokens.opacity.medium),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.colors.accent,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
internal fun SettingsNavigationRow(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    // Plain glyph, no background chip — the native iOS Settings / WhatsApp Settings look, where
    // every row's icon just sits directly in front of the label instead of inside a colored
    // square. Tint still follows the user's chosen theme accent by default (a soft halo behind
    // the glyph carries the same accent through), so switching Home → Layout color continues to
    // reach Settings the way it did before this row style changed.
    iconTint: Color? = null,
    enabled: Boolean = true,
    isTablet: Boolean,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    /**
     * Puts [icon] in a white glyph on a colored rounded square instead, iOS Settings' top-level
     * look, so each category reads at a glance. The color comes from [settingsTileColor].
     */
    iconTile: Boolean = false,
    /** A short current value shown before the chevron, e.g. the connected tracking service. */
    value: String? = null,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val resolvedIconTint = iconTint ?: tokens.colors.accent
    val iconSize = if (isTablet) 26.dp else 22.dp
    // A title-only row sits tighter, like iOS Settings' list.
    val compact = description.isNullOrBlank()
    val verticalPadding = when {
        isTablet -> if (compact) 13.dp else 16.dp
        else -> if (compact) 11.dp else 14.dp
    }
    val horizontalPadding = if (isTablet) 20.dp else 16.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .settingsRowClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            .alpha(if (enabled) NuvioTokens.Opacity.visible else tokens.opacity.medium),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
                .widthIn(max = if (isTablet) 560.dp else Dp.Unspecified),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconTile && icon != null) {
                val tileSize = if (isTablet) 32.dp else 30.dp
                Box(
                    modifier = Modifier
                        .size(tileSize)
                        .clip(RoundedCornerShape(if (isTablet) 8.dp else 7.dp))
                        .background(iconTint ?: settingsTileColor(icon)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(if (isTablet) 20.dp else 18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(if (isTablet) 16.dp else 14.dp))
            } else if (icon != null || iconPainter != null) {
                Box(
                    modifier = Modifier.size(iconSize),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (iconPainter != null) {
                            androidx.compose.foundation.Image(
                                painter = iconPainter,
                                contentDescription = null,
                                modifier = Modifier.size(if (isTablet) 28.dp else 24.dp),
                                contentScale = ContentScale.Fit,
                            )
                        } else if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.gradientMask(MaterialTheme.themePalette.accentBrush()),
                                tint = resolvedIconTint,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(if (isTablet) 18.dp else 16.dp))
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                )
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textMuted,
                        modifier = Modifier.alpha(0.92f),
                    )
                }
            }
        }
        if (trailingContent != null) {
            trailingContent(this)
        } else {
            if (!value.isNullOrBlank()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .widthIn(max = if (isTablet) 220.dp else 150.dp)
                        .padding(end = 4.dp),
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
}

@Composable
internal fun SettingsSwitchRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    isTablet: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    val horizontalPadding = if (isTablet) 20.dp else 16.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .settingsRowClickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
                .widthIn(max = if (isTablet) 560.dp else Dp.Unspecified)
                .alpha(if (enabled) NuvioTokens.Opacity.visible else tokens.opacity.medium),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.Medium,
            )
            if (!description.isNullOrBlank()) {
                // Smaller and softer than the title, so a page of toggles scans by their names.
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.textMuted.copy(alpha = 0.85f),
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.padding(start = 4.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = tokens.colors.onAccent,
                checkedTrackColor = tokens.colors.accent,
                uncheckedThumbColor = tokens.colors.textMuted,
                uncheckedTrackColor = tokens.colors.borderDefault,
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsChipRow(
    title: String,
    description: String,
    isTablet: Boolean,
    footer: String? = null,
    chips: @Composable FlowRowScope.() -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isTablet) 20.dp else 16.dp,
                vertical = if (isTablet) 16.dp else 14.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textMuted,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = chips,
        )
        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.colors.textMuted,
            )
        }
    }
}

@Composable
internal fun HomescreenCatalogRow(
    item: HomeCatalogSettingsItem,
    isTablet: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTitleChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onTop10StyleChange: (Boolean) -> Unit,
    onLandscapeModeChange: (Boolean) -> Unit,
    onTop10OutlinedNumberChange: (Boolean) -> Unit,
    dragHandleScope: ReorderableCollectionItemScope,
    onPinnedDragAttempt: () -> Unit = {},
) {
    val tokens = MaterialTheme.nuvio
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 18.dp else 16.dp
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandedChange(!expanded) }
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
                    .then(if (isTablet) Modifier.widthIn(max = 560.dp) else Modifier),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.displayTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (item.isCollection) {
                        stringResource(Res.string.settings_homescreen_collection_with_addon, item.addonName)
                    } else {
                        item.addonName
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                )
                Text(
                    text = buildString {
                        append(
                            if (item.enabled) {
                                stringResource(Res.string.settings_homescreen_visible)
                            } else {
                                stringResource(Res.string.settings_homescreen_hidden)
                            },
                        )
                        if (item.isCollection) {
                            if (item.isPinnedToTop) {
                                append(" • ")
                                append(stringResource(Res.string.settings_homescreen_pinned_to_top))
                            }
                        } else {
                            append(" • ")
                            append(
                                if (item.heroSourceEnabled) {
                                    stringResource(Res.string.settings_homescreen_hero_source)
                                } else {
                                    stringResource(Res.string.settings_homescreen_not_in_hero)
                                },
                            )
                            if (item.landscapeModeEnabled) {
                                append(" • ")
                                append(stringResource(Res.string.settings_homescreen_top10_orientation_landscape))
                            }
                            if (item.top10StyleEnabled) {
                                append(" • ")
                                append(stringResource(Res.string.settings_homescreen_top10_style_on))
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.textMuted,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Switch(
                    checked = item.enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = tokens.colors.onAccent,
                        checkedTrackColor = tokens.colors.accent,
                        uncheckedThumbColor = tokens.colors.textMuted,
                        uncheckedTrackColor = tokens.colors.borderDefault,
                    ),
                )
                if (item.isPinnedToTop) {
                    IconButton(
                        onClick = onPinnedDragAttempt,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = stringResource(Res.string.settings_homescreen_pinned),
                            tint = tokens.colors.textMuted.copy(alpha = tokens.opacity.medium),
                        )
                    }
                } else {
                    IconButton(
                        modifier = with(dragHandleScope) {
                            Modifier.draggableHandle(
                                onDragStarted = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                            )
                        },
                        onClick = {},
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = stringResource(Res.string.settings_homescreen_reorder),
                            tint = tokens.colors.textMuted,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = expanded && !item.isCollection) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = item.customTitle,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(Res.string.settings_homescreen_display_name)) },
                    placeholder = { Text(item.defaultTitle) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = tokens.colors.borderFocus.copy(alpha = tokens.opacity.strong),
                        unfocusedBorderColor = tokens.colors.borderDefault.copy(alpha = tokens.opacity.medium),
                        focusedContainerColor = tokens.colors.surface,
                        unfocusedContainerColor = tokens.colors.surface,
                        disabledContainerColor = tokens.colors.surface,
                    ),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(Res.string.settings_homescreen_landscape_mode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textPrimary,
                    )
                    Text(
                        text = stringResource(Res.string.settings_homescreen_landscape_mode_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.textMuted,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Top10OrientationOption(
                            modifier = Modifier.weight(1f),
                            label = stringResource(Res.string.settings_homescreen_top10_orientation_portrait),
                            selected = !item.landscapeModeEnabled,
                            onClick = { onLandscapeModeChange(false) },
                        )
                        Top10OrientationOption(
                            modifier = Modifier.weight(1f),
                            label = stringResource(Res.string.settings_homescreen_top10_orientation_landscape),
                            selected = item.landscapeModeEnabled,
                            onClick = { onLandscapeModeChange(true) },
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTop10StyleChange(!item.top10StyleEnabled) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_homescreen_top10_style),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.colors.textPrimary,
                        )
                        Text(
                            text = stringResource(Res.string.settings_homescreen_top10_style_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.colors.textMuted,
                        )
                    }
                    Switch(
                        checked = item.top10StyleEnabled,
                        onCheckedChange = onTop10StyleChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = tokens.colors.onAccent,
                            checkedTrackColor = tokens.colors.accent,
                            uncheckedThumbColor = tokens.colors.textMuted,
                            uncheckedTrackColor = tokens.colors.borderDefault,
                        ),
                    )
                }
                AnimatedVisibility(visible = item.top10StyleEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Top10OrientationOption(
                                modifier = Modifier.weight(1f),
                                label = stringResource(Res.string.settings_homescreen_top10_number_filled),
                                selected = !item.top10OutlinedNumberEnabled,
                                onClick = { onTop10OutlinedNumberChange(false) },
                            )
                            Top10OrientationOption(
                                modifier = Modifier.weight(1f),
                                label = stringResource(Res.string.settings_homescreen_top10_number_outlined),
                                selected = item.top10OutlinedNumberEnabled,
                                onClick = { onTop10OutlinedNumberChange(true) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Top10OrientationOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) {
            tokens.colors.accent.copy(alpha = tokens.opacity.selected)
        } else {
            tokens.colors.surfaceCard.copy(alpha = tokens.opacity.medium)
        },
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) tokens.colors.accent else tokens.colors.textMuted,
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/**
 * Category colors for [SettingsNavigationRow]'s icon tiles, after the iOS system palette. Keyed
 * on the row's icon so Settings search results get the same tile as the category they lead to;
 * anything unlisted takes the theme accent.
 */
@Composable
internal fun settingsTileColor(icon: ImageVector): Color = when (icon) {
    Icons.Rounded.AccountCircle -> Color(0xFF8E8E93)
    Icons.Default.Sync -> Color(0xFF30D158)
    Icons.Rounded.Palette -> Color(0xFFBF5AF2)
    Icons.Rounded.Extension -> Color(0xFF5E5CE6)
    Icons.Rounded.PlayArrow -> Color(0xFFFF453A)
    Icons.Rounded.Link -> Color(0xFF40C8E0)
    Icons.Rounded.Notifications -> Color(0xFFFF375F)
    Icons.Rounded.CloudDownload -> Color(0xFF0A84FF)
    Icons.Rounded.Favorite -> Color(0xFFFF2D55)
    Icons.Rounded.LocalCafe -> KofiBrandColor
    Icons.Rounded.Policy -> Color(0xFF64D2FF)
    Icons.Rounded.Info -> Color(0xFF8E8E93)
    Icons.Rounded.NewReleases -> Color(0xFFFF9F0A)
    Icons.Rounded.BugReport -> Color(0xFFAC8E68)
    Icons.Rounded.Tune -> Color(0xFF636366)
    else -> MaterialTheme.nuvio.colors.accent
}

/**
 * A settings row's tap target with iOS' grey cell highlight instead of a ripple: on at once while
 * pressed, fading out on release.
 */
@Composable
internal fun Modifier.settingsRowClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val highlight by animateColorAsState(
        targetValue = if (pressed) tokens.colors.textPrimary.copy(alpha = 0.09f) else Color.Transparent,
        animationSpec = tween(durationMillis = if (pressed) 0 else 260),
        label = "settingsRowHighlight",
    )
    return background(highlight).clickable(
        enabled = enabled,
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
    )
}
