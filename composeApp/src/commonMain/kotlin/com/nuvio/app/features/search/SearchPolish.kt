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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import com.nuvio.app.core.ui.nuvio
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_search_recent_searches
import nuvio.composeapp.generated.resources.compose_search_remove_recent_search
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

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

private const val ChipDismissVelocity = 1200f
