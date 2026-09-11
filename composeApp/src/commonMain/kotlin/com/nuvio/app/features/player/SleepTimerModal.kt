package com.nuvio.app.features.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.watchprogress.WatchProgressClock
import kotlinx.coroutines.delay
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.player_action_sleep_timer
import nuvio.composeapp.generated.resources.player_sleep_timer_active_label
import nuvio.composeapp.generated.resources.player_sleep_timer_cancel
import nuvio.composeapp.generated.resources.player_sleep_timer_minutes_unit
import nuvio.composeapp.generated.resources.player_sleep_timer_start
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

private const val SleepTimerMaxMinutes = 60
private val SleepTimerWheelItemHeight = 44.dp
private const val SleepTimerWheelVisibleSideCount = 1 // items shown above/below the centered one

@Composable
internal fun SleepTimerModal(
    visible: Boolean,
    isActive: Boolean,
    sleepTimerEndAtMs: Long?,
    onDurationSelected: (minutes: Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss,
                )
                .background(Color.Black.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(tween(300)) { it / 3 } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(250)) { it / 3 } + fadeOut(tween(250)),
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth(0.86f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {},
                        )
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.player_action_sleep_timer),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 8.dp,
                            bottom = if (isActive) 2.dp else 8.dp,
                        ),
                    )
                    if (isActive && sleepTimerEndAtMs != null) {
                        var remainingMs by remember(sleepTimerEndAtMs) {
                            mutableLongStateOf(sleepTimerEndAtMs - WatchProgressClock.nowEpochMs())
                        }
                        LaunchedEffect(sleepTimerEndAtMs) {
                            while (true) {
                                remainingMs = sleepTimerEndAtMs - WatchProgressClock.nowEpochMs()
                                if (remainingMs <= 0L) break
                                delay(1_000L)
                            }
                        }
                        Text(
                            text = stringResource(
                                Res.string.player_sleep_timer_active_label,
                                formatSleepTimerRemaining(remainingMs),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SleepTimerOptionRow(
                            label = stringResource(Res.string.player_sleep_timer_cancel),
                            tint = MaterialTheme.colorScheme.error,
                            onClick = onCancelTimer,
                        )
                    } else {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        var selectedMinutes by remember { mutableIntStateOf(15) }
                        SleepTimerWheelPicker(
                            selectedMinutes = selectedMinutes,
                            onMinutesChanged = { selectedMinutes = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                        )
                        Button(
                            onClick = { onDurationSelected(selectedMinutes) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text(stringResource(Res.string.player_sleep_timer_start))
                        }
                    }
                }
            }
        }
    }
}

/**
 * A scrollable, snap-to-center picker of 1..[SleepTimerMaxMinutes] minutes — the "ruleta" the wheel
 * itself is the live-editing state (matching iOS's own Clock app timer), with a separate button
 * elsewhere committing whatever it currently reads.
 */
@Composable
private fun SleepTimerWheelPicker(
    selectedMinutes: Int,
    onMinutesChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val values = remember { (1..SleepTimerMaxMinutes).toList() }
    val listState = rememberLazyListState()
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        listState.scrollToItem((selectedMinutes - 1).coerceIn(0, values.lastIndex))
    }

    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { item ->
                abs((item.offset + item.size / 2) - viewportCenter)
            }?.index ?: 0
        }
    }

    // One light tick per detent scrolled past, matching iOS's own wheel pickers — skips the very
    // first value so opening the sheet doesn't fire one for free.
    var previousCenteredIndex by remember { mutableIntStateOf(centeredIndex) }
    LaunchedEffect(centeredIndex) {
        if (centeredIndex != previousCenteredIndex) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            previousCenteredIndex = centeredIndex
        }
    }

    LaunchedEffect(centeredIndex, listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            onMinutesChanged(values[centeredIndex.coerceIn(0, values.lastIndex)])
        }
    }

    val wheelHeight = SleepTimerWheelItemHeight * (SleepTimerWheelVisibleSideCount * 2 + 1)
    Box(
        modifier = modifier.height(wheelHeight),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SleepTimerWheelItemHeight)
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        // Only numbers scroll in the wheel itself — "min" is a fixed label to their right, never
        // part of the scrolling content. The number column is a fixed width (not weight(1f), which
        // would hug the Row's own full-width edge instead of letting the "NN min" pair center as
        // one unit) so the group's total width stays constant as the digit count changes, and the
        // outer Box's contentAlignment actually centers it inside the highlight pill.
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .fillMaxHeight(),
            ) {
                LazyColumn(
                    state = listState,
                    flingBehavior = rememberSnapFlingBehavior(listState),
                    contentPadding = PaddingValues(
                        vertical = SleepTimerWheelItemHeight * SleepTimerWheelVisibleSideCount,
                    ),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(values) { index, minutes ->
                        val isCentered = index == centeredIndex
                        val distance = abs(index - centeredIndex)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(SleepTimerWheelItemHeight)
                                .graphicsLayer {
                                    val scale = (1f - distance * 0.14f).coerceAtLeast(0.72f)
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = (1f - distance * 0.35f).coerceAtLeast(0.35f)
                                },
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Text(
                                text = minutes.toString(),
                                style = if (isCentered) {
                                    MaterialTheme.typography.headlineSmall
                                } else {
                                    MaterialTheme.typography.titleMedium
                                },
                                fontWeight = if (isCentered) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
            Text(
                text = stringResource(Res.string.player_sleep_timer_minutes_unit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

private fun formatSleepTimerRemaining(remainingMs: Long): String {
    val totalSeconds = (remainingMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Composable
private fun SleepTimerOptionRow(
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.Unspecified,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint,
        )
    }
}
