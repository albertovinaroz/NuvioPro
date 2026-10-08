package com.nuvio.app.features.search

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.sign
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.abs
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.nuvio
import kotlinx.coroutines.delay
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_search_recent_searches
import nuvio.composeapp.generated.resources.compose_search_remove_recent_search
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark

/** Recent searches as clock chips; tap to search again, swipe either way to forget one. */
@Composable
internal fun SearchRecentChips(
    recentSearches: List<String>,
    onSearchPress: (String) -> Unit,
    onRemoveSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = stringResource(Res.string.compose_search_recent_searches),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            recentSearches.forEach { recentQuery ->
                key(recentQuery) {
                    SearchRecentChip(
                        query = recentQuery,
                        onClick = { onSearchPress(recentQuery) },
                        onRemove = { onRemoveSearch(recentQuery) },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun SearchRecentChip(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val removeLabel = stringResource(Res.string.compose_search_remove_recent_search)
    // A plain drag rather than Material's SwipeToDismissBox: that one saves its state, so a chip
    // swiped away just as the section disappeared came back later still dismissed — an
    // invisible gap where a new search with the same text should have been.
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var chipWidthPx by remember { mutableIntStateOf(0) }
    val dragFraction = if (chipWidthPx > 0) (abs(offsetX.value) / chipWidthPx).coerceIn(0f, 1f) else 0f
    Box(
        modifier = Modifier
            .onSizeChanged { chipWidthPx = it.width }
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            // Fades as it's dragged away, so the swipe reads as "forget this".
            .alpha(1f - dragFraction * 0.8f)
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    scope.launch { offsetX.snapTo(offsetX.value + delta) }
                },
                onDragStopped = { velocity ->
                    val width = chipWidthPx.toFloat().coerceAtLeast(1f)
                    val flung = abs(velocity) > ChipDismissVelocity
                    if (abs(offsetX.value) > width * 0.4f || flung) {
                        val direction = if (offsetX.value != 0f) sign(offsetX.value) else sign(velocity)
                        haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                        offsetX.animateTo(direction * width * 1.3f, tween(durationMillis = 160))
                        onRemove()
                    } else {
                        offsetX.animateTo(0f, spring())
                    }
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(CircleShape)
                // The filter chips' fill, which stands out from the page; surfaceCard barely does.
                .background(tokens.colors.surface)
                .clickable(onClick = onClick)
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(removeLabel) {
                            onRemove()
                            true
                        },
                    )
                }
                .padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = tokens.colors.textMuted,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = query,
                style = MaterialTheme.typography.labelLarge,
                color = tokens.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A centred empty state: an icon in a soft disc, a title, the message and an optional action. */
@Composable
internal fun SearchEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    /** The search that came up empty, echoed back under the title. */
    query: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(tokens.colors.accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.colors.accent,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = tokens.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        if (!query.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "“${query.trim()}”",
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.colors.textMuted,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            NuvioPrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

/**
 * Fades and lifts a row in, [index] rows after the first, when it appears shortly after [batch]
 * started (new filter results arriving). Rows composed later — reached by scrolling, or added by
 * pagination — just show, so scrolling never replays the animation.
 */
@Composable
internal fun StaggeredEntrance(
    batch: TimeMark,
    index: Int,
    content: @Composable () -> Unit,
) {
    val progress = remember(batch) {
        Animatable(if (batch.elapsedNow() < EntranceWindow) 0f else 1f)
    }
    LaunchedEffect(batch) {
        if (progress.value < 1f) {
            delay(index.coerceAtMost(MaxStaggeredRows) * StaggerStepMillis)
            progress.animateTo(1f, tween(durationMillis = 340, easing = FastOutSlowInEasing))
        }
    }
    val liftPx = with(androidx.compose.ui.platform.LocalDensity.current) { 18.dp.toPx() }
    Box(
        modifier = Modifier
            // Alpha is read here and the lift in the placement phase rather than through a
            // graphicsLayer block, which iOS can stop re-running after a rotation.
            .alpha(progress.value)
            .offset { IntOffset(0, ((1f - progress.value) * liftPx).roundToInt()) },
    ) {
        content()
    }
}

private val EntranceWindow = 700.milliseconds
private const val ChipDismissVelocity = 1200f
private const val MaxStaggeredRows = 6
private const val StaggerStepMillis = 55L
