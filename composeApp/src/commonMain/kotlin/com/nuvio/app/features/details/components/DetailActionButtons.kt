package com.nuvio.app.features.details.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import com.nuvio.app.features.ratings.UserRatingStars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import com.nuvio.app.core.ui.AppIconResource
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.core.ui.appIconPainter
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_play
import nuvio.composeapp.generated.resources.details_actions_menu_label
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// Fraction of menuProgress the Play pill spends at rest before its width starts shrinking —
// gives PlayButton's own (quicker, independent) label collapse a head start so the label is
// gone before the pill visibly narrows, instead of both happening at once.
private const val PlayShrinkStartFraction = 0.35f

data class DetailSecondaryAction(
    val label: String,
    val icon: ImageVector,
    val drawable: DrawableResource? = null,
    val isActive: Boolean = false,
    val onClick: () -> Unit = {},
    val onLongClick: (() -> Unit)? = null,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailActionButtons(
    modifier: Modifier = Modifier,
    playLabel: String = stringResource(Res.string.action_play),
    playEnabled: Boolean = true,
    pinnedAction: DetailSecondaryAction? = null,
    secondaryActions: List<DetailSecondaryAction> = emptyList(),
    actionsMenuLabel: String = stringResource(Res.string.details_actions_menu_label),
    isTablet: Boolean = false,
    onPlayClick: () -> Unit = {},
    onPlayLongClick: (() -> Unit)? = null,
    iconActionRow: Boolean = false,
    iconActions: List<DetailSecondaryAction> = emptyList(),
    userRating: Int? = null,
    onRateClick: (() -> Unit)? = null,
) {
    val playPainter = appIconPainter(AppIconResource.PlayerPlay)
    val buttonHeight = if (isTablet) 56.dp else 52.dp
    val iconButtonSize = buttonHeight
    val playShape = RoundedCornerShape(40.dp)
    val hapticFeedback = LocalHapticFeedback.current
    var actionsExpanded by remember { mutableStateOf(false) }
    val menuProgress by animateFloatAsState(
        targetValue = if (actionsExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "detail_action_menu_progress",
    )
    val hasSecondaryActions = secondaryActions.isNotEmpty()

    Column(
        modifier = modifier
            .widthIn(max = if (isTablet) 520.dp else 420.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (iconActionRow) {
            PlayButton(
                playLabel = playLabel,
                playEnabled = playEnabled,
                playPainter = playPainter,
                playShape = playShape,
                buttonHeight = buttonHeight,
                isTablet = isTablet,
                onPlayClick = onPlayClick,
                onPlayLongClick = onPlayLongClick,
                modifier = Modifier.fillMaxWidth(),
            )
            if (iconActions.isNotEmpty()) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val layout = fitIconActionRow(
                        count = iconActions.size,
                        availableWidth = maxWidth,
                        preferredSize = iconButtonSize,
                        preferredSpacing = if (isTablet) 20.dp else 16.dp,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (layout.fits) Modifier else Modifier.horizontalScroll(rememberScrollState())),
                        horizontalArrangement = Arrangement.spacedBy(
                            space = layout.spacing,
                            alignment = Alignment.CenterHorizontally,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        iconActions.forEach { action ->
                            DetailIconAction(
                                label = action.label,
                                icon = action.icon,
                                drawable = action.drawable,
                                active = action.isActive,
                                progress = 1f,
                                size = layout.size,
                                onClick = action.onClick,
                                onLongClick = action.onLongClick,
                            )
                        }
                    }
                }
            }
            onRateClick?.let { rate ->
                UserRatingStars(
                    rating = userRating,
                    onClick = rate,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            return@Column
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight),
        ) {
        // A true lerp between two fixed endpoints — what weight(1f) gives it at rest (maxWidth
        // minus every other sibling's own width, all of which are constant-size regardless of
        // menuProgress; only secondaryActions' OWN widths animate, not this) down to exactly
        // iconButtonSize — rather than subtracting secondaryActions' current width and flooring
        // the result. Flooring only guarantees "at least" iconButtonSize: whatever slack the row
        // actually has left over at menuProgress=1 was free to land above that floor, and a
        // circle needs width to land exactly on it, not just clear it — anything wider renders as
        // a stadium (pill), not a circle, no matter how the corner radius clamps.
        val pinnedWidth = if (pinnedAction != null) iconButtonSize + 12.dp else 0.dp
        val moreButtonWidth = if (hasSecondaryActions) iconButtonSize else 0.dp
        val playRestWidth = maxWidth - pinnedWidth - moreButtonWidth
        val playCollapsesToIcon = hasSecondaryActions && secondaryActions.size >= 4
        // The label's own AnimatedVisibility (see PlayButton) collapses and re-centers the icon
        // on actionsExpanded directly, independent of and quicker than this width shrink — so the
        // label is already gone by the time the pill visibly starts narrowing, rather than both
        // happening at once and reading as the label getting squeezed out by the shrinking pill.
        // PlayShrinkStartFraction holds the width lerp at rest until menuProgress has cleared
        // that head start.
        val playShrinkProgress = if (playCollapsesToIcon) {
            ((menuProgress - PlayShrinkStartFraction) / (1f - PlayShrinkStartFraction)).coerceIn(0f, 1f)
        } else {
            0f
        }
        val playWidth = if (playCollapsesToIcon) {
            lerp(playRestWidth, iconButtonSize, playShrinkProgress).coerceAtLeast(iconButtonSize)
        } else {
            null
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayButton(
                playLabel = playLabel,
                playEnabled = playEnabled,
                playPainter = playPainter,
                playShape = playShape,
                buttonHeight = buttonHeight,
                isTablet = isTablet,
                onPlayClick = onPlayClick,
                onPlayLongClick = onPlayLongClick,
                showText = !(playCollapsesToIcon && actionsExpanded),
                modifier = if (playWidth != null) Modifier.width(playWidth) else Modifier.weight(1f),
            )

            if (pinnedAction != null) {
                Spacer(modifier = Modifier.width(12.dp))
                DetailIconAction(
                    label = pinnedAction.label,
                    icon = pinnedAction.icon,
                    active = pinnedAction.isActive,
                    progress = 1f,
                    size = iconButtonSize,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        pinnedAction.onClick()
                    },
                )
            }

            if (hasSecondaryActions) {
                Spacer(modifier = Modifier.width(12.dp))
                secondaryActions.forEachIndexed { index, action ->
                    Box(
                        modifier = Modifier
                            .width(iconButtonSize * menuProgress)
                            .height(iconButtonSize)
                            .graphicsLayer {
                                clip = true
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (actionsExpanded || menuProgress > 0.01f) {
                            DetailIconAction(
                                label = action.label,
                                icon = action.icon,
                                drawable = action.drawable,
                                active = action.isActive,
                                progress = menuProgress,
                                size = iconButtonSize,
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    action.onClick()
                                },
                                onLongClick = action.onLongClick?.let { longClick ->
                                    {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        longClick()
                                    }
                                },
                            )
                        }
                    }

                    if (index != secondaryActions.lastIndex) {
                        Spacer(modifier = Modifier.width(12.dp * menuProgress))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp * menuProgress))
            }

            if (hasSecondaryActions) {
                Surface(
                    modifier = Modifier.size(iconButtonSize),
                    shape = CircleShape,
                    color = if (actionsExpanded) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
                    },
                    contentColor = if (actionsExpanded) {
                        MaterialTheme.colorScheme.background
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .size(iconButtonSize)
                            .clickable(role = Role.Button) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                actionsExpanded = !actionsExpanded
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = actionsMenuLabel,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    rotationZ = 90f * menuProgress
                                },
                        )
                    }
                }
            }
        }
    }

        onRateClick?.let { rate ->
            UserRatingStars(
                rating = userRating,
                onClick = rate,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DetailIconAction(
    label: String,
    icon: ImageVector,
    active: Boolean,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp,
    drawable: DrawableResource? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.graphicsLayer {
            alpha = progress
            scaleX = 0.86f + (0.14f * progress)
            scaleY = 0.86f + (0.14f * progress)
        },
        shape = CircleShape,
        color = if (active) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (active) {
            MaterialTheme.colorScheme.background
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        tonalElevation = 6.dp,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    role = Role.Button,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (drawable != null) {
                Icon(
                    painter = painterResource(drawable),
                    contentDescription = label,
                    modifier = Modifier.size(21.dp),
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayButton(
    playLabel: String,
    playEnabled: Boolean,
    playPainter: Painter,
    playShape: Shape,
    buttonHeight: Dp,
    isTablet: Boolean,
    onPlayClick: () -> Unit,
    onPlayLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showText: Boolean = true,
) {
        // Deliberately always playShape, never CircleShape: the caller already shrinks this
        // button's own width down to iconButtonSize (matching buttonHeight) once menuProgress
        // finishes, and playShape's corner radius exceeds half of that — Compose's own
        // corner-clamping already renders that as a clean circle on its own. Switching shape
        // outright would just reintroduce a jump cut this is meant to avoid.
        Surface(
            modifier = modifier
                .height(buttonHeight)
                .then(
                    if (playEnabled) {
                        Modifier
                            .clip(playShape)
                            .background(MaterialTheme.themePalette.accentBrush())
                    } else {
                        Modifier
                    },
                ),
            shape = playShape,
            color = if (playEnabled) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (playEnabled) {
                MaterialTheme.nuvio.colors.onAccent
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        enabled = playEnabled,
                        onClick = {
                            onPlayClick()
                        },
                        onLongClick = onPlayLongClick,
                        role = Role.Button,
                    )
                    .height(buttonHeight),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = playPainter,
                    contentDescription = if (!showText) playLabel else null,
                    modifier = Modifier.size(if (isTablet) 20.dp else 18.dp),
                )
                // A quick, independent collapse — deliberately not tied to the caller's own
                // (slower) menuProgress-driven width shrink. Finishing this first, before that
                // shrink even starts (see PlayShrinkStartFraction), is what makes the label read
                // as dismissed on its own rather than squeezed out by the pill narrowing under it.
                AnimatedVisibility(
                    visible = showText,
                    enter = expandHorizontally(tween(120)) + fadeIn(tween(120)),
                    exit = shrinkHorizontally(tween(120)) + fadeOut(tween(120)),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = playLabel,
                            style = if (isTablet) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.titleSmall
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
}

private data class IconActionRowLayout(val size: Dp, val spacing: Dp, val fits: Boolean)

private fun fitIconActionRow(
    count: Int,
    availableWidth: Dp,
    preferredSize: Dp,
    preferredSpacing: Dp,
): IconActionRowLayout {
    if (count <= 1) return IconActionRowLayout(preferredSize, preferredSpacing, true)
    val gaps = count - 1
    val minSpacing = 8.dp
    val minSize = 44.dp
    if (preferredSize * count + preferredSpacing * gaps <= availableWidth) {
        return IconActionRowLayout(preferredSize, preferredSpacing, true)
    }
    val spacingAtPreferredSize = (availableWidth - preferredSize * count) / gaps
    if (spacingAtPreferredSize >= minSpacing) {
        return IconActionRowLayout(preferredSize, spacingAtPreferredSize, true)
    }
    val shrunkSize = (availableWidth - minSpacing * gaps) / count
    return if (shrunkSize >= minSize) {
        IconActionRowLayout(shrunkSize, minSpacing, true)
    } else {
        IconActionRowLayout(minSize, minSpacing, false)
    }
}
