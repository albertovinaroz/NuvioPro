package com.nuvio.app.features.details.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import nuvio.composeapp.generated.resources.hero_add_to_library
import nuvio.composeapp.generated.resources.hero_mark_unwatched
import nuvio.composeapp.generated.resources.hero_mark_watched
import nuvio.composeapp.generated.resources.hero_remove_from_library
import nuvio.composeapp.generated.resources.playback_unavailable
import nuvio.composeapp.generated.resources.random_episode_title
import nuvio.composeapp.generated.resources.shuffle_stop
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight),
        ) {
        // Width stays plain weight(1f) always — same as the <4 case, which already reads fine —
        // the only thing 4+ secondaryActions changes is the label fading out as menuProgress
        // does, on the same clock as everything else in this row (secondaryActions' own widths,
        // the "..." rotation), rather than reshaping the pill itself.
        val playTextVisibleFraction = if (hasSecondaryActions && secondaryActions.size >= 4) {
            (1f - menuProgress).coerceIn(0f, 1f)
        } else {
            1f
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
                textVisibleFraction = playTextVisibleFraction,
                modifier = Modifier.weight(1f),
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
fun DetailActions(
    playLabel: String,
    playEnabled: Boolean,
    isSaved: Boolean,
    isWatched: Boolean,
    isTablet: Boolean,
    shuffleEnabled: Boolean,
    onPlayClick: () -> Unit,
    onPlayLongClick: (() -> Unit)?,
    onShuffleClick: (() -> Unit)?,
    onWatchedClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveLongClick: (() -> Unit)?,
) {
    val shuffleAction = onShuffleClick?.let { onClick ->
        DetailSecondaryAction(
            label = stringResource(if (shuffleEnabled) Res.string.shuffle_stop else Res.string.random_episode_title),
            icon = Icons.Default.Shuffle,
            isActive = shuffleEnabled,
            onClick = onClick,
        )
    }
    DetailActionButtons(
        playLabel = if (playEnabled) playLabel else stringResource(Res.string.playback_unavailable),
        playEnabled = playEnabled,
        pinnedAction = shuffleAction?.takeIf { shuffleEnabled },
        secondaryActions = buildList {
            if (!shuffleEnabled) shuffleAction?.let(::add)
            add(
                DetailSecondaryAction(
                    label = stringResource(if (isWatched) Res.string.hero_mark_unwatched else Res.string.hero_mark_watched),
                    icon = if (isWatched) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                    isActive = isWatched,
                    onClick = onWatchedClick,
                ),
            )
            add(
                DetailSecondaryAction(
                    label = stringResource(if (isSaved) Res.string.hero_remove_from_library else Res.string.hero_add_to_library),
                    icon = if (isSaved) Icons.Default.Check else Icons.Default.Add,
                    isActive = isSaved,
                    onClick = onSaveClick,
                    onLongClick = onSaveLongClick,
                ),
            )
        },
        isTablet = isTablet,
        onPlayClick = onPlayClick,
        onPlayLongClick = onPlayLongClick,
    )
}

@Composable
// Kept internal (upstream narrowed this to private): HomeHeroSection.kt calls this directly for
// its own hero actions row, a cross-file usage that's Pro-specific here.
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
    textVisibleFraction: Float = 1f,
) {
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
                    contentDescription = if (textVisibleFraction < 0.5f) playLabel else null,
                    modifier = Modifier.size(if (isTablet) 20.dp else 18.dp),
                )
                // Scales the label's own measured width down to textVisibleFraction of itself
                // (rather than an AnimatedVisibility with its own separate clock) so it shrinks
                // and fades in lockstep with menuProgress, same as everything else in this row.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            val width = (placeable.width * textVisibleFraction).roundToInt()
                            layout(width, placeable.height) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                        .graphicsLayer { alpha = textVisibleFraction },
                ) {
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
