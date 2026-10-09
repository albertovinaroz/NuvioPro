package com.nuvio.app.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.nuvio

/** The Profile page's collapsible groups, in display order. */
internal enum class ProfileSection(val key: String) {
    Friends("friends"),
    Stats("stats"),
    Achievements("achievements"),
    Diary("diary"),
}

/**
 * Which Profile sections are open. Everything starts collapsed so all section headers fit right
 * under the hero; whatever the user opens stays open across visits and restarts.
 */
internal object ProfileSectionsState {
    private val expanded = mutableStateOf(load())

    fun isExpanded(section: ProfileSection): Boolean = section in expanded.value

    fun expand(section: ProfileSection) {
        if (section !in expanded.value) toggle(section)
    }

    fun toggle(section: ProfileSection) {
        val next = if (section in expanded.value) expanded.value - section else expanded.value + section
        expanded.value = next
        ProfileSectionsStorage.saveExpanded(next.joinToString(",") { it.key })
    }

    private fun load(): Set<ProfileSection> {
        val keys = ProfileSectionsStorage.loadExpanded()?.split(',')?.toSet().orEmpty()
        return ProfileSection.entries.filterTo(mutableSetOf()) { it.key in keys }
    }
}

/**
 * One foldable row of the Profile's grouped card (see [ProfileSectionGroup]): a header (tile
 * icon, title, one-line summary, an optional glanceable [preview], chevron) that folds its
 * [content] in and out. [showDivider] draws the hairline separating it from the row above.
 */
@Composable
internal fun ProfileCollapsibleSection(
    section: ProfileSection,
    title: String,
    summary: String?,
    icon: ImageVector,
    tileColor: Color,
    isTablet: Boolean,
    /** A red count bubble before the chevron, for things waiting on the user; hidden at 0. */
    badgeCount: Int = 0,
    showDivider: Boolean = false,
    /** A small at-a-glance preview (avatars, badges, posters) shown while collapsed. */
    preview: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val expanded = ProfileSectionsState.isExpanded(section)
    val chevronAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "profileSectionChevron",
    )
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val tileSize = if (isTablet) 32.dp else 30.dp
    val tileGap = if (isTablet) 16.dp else 14.dp
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showDivider) {
            HorizontalDivider(
                // Starts under the title, like iOS grouped lists.
                modifier = Modifier.padding(start = horizontalPadding + tileSize + tileGap),
                thickness = tokens.borders.hairline,
                color = tokens.colors.borderSubtle,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .settingsRowClickable { ProfileSectionsState.toggle(section) }
                .padding(horizontal = horizontalPadding, vertical = if (isTablet) 14.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tileGap),
        ) {
            Box(
                modifier = Modifier
                    .size(tileSize)
                    .clip(RoundedCornerShape(if (isTablet) 8.dp else 7.dp))
                    .background(tileColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(if (isTablet) 20.dp else 18.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (preview != null) {
                AnimatedVisibility(visible = !expanded, enter = fadeIn(), exit = fadeOut()) { preview() }
            }
            if (badgeCount > 0) CountBadge(count = badgeCount)
            // Rotated while drawing rather than through graphicsLayer, which stops updating
            // after a device rotation on iOS.
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = tokens.colors.textMuted,
                modifier = Modifier
                    .size(24.dp)
                    .drawWithContent { rotate(chevronAngle) { this@drawWithContent.drawContent() } },
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding)
                    .padding(top = 6.dp, bottom = if (isTablet) 22.dp else 18.dp),
                content = content,
            )
        }
    }
}

/** The single grouped card holding the Profile's foldable sections, iOS Settings style. */
@Composable
internal fun ProfileSectionGroup(
    isTablet: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsGroup(isTablet = isTablet, content = content)
}

/** The iOS-style red count bubble ("9+" past nine). */
@Composable
internal fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 20.dp)
            .widthIn(min = 20.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFFF3B30))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 9) "9+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}
