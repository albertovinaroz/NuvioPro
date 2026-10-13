package com.nuvio.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** How far the indicator travels down with the pull, at the refresh threshold. */
private val PullIndicatorTravel = 44.dp

/** Even an instant refresh keeps the spinner up this long, so it reads as having happened. */
private const val MinRefreshSpinMillis = 700L

/**
 * State for a pull-to-refresh: the Material gesture plus whether a refresh is running. [refresh]
 * runs [onRefresh] and keeps [isRefreshing] on until it returns (and at least briefly).
 */
@OptIn(ExperimentalMaterial3Api::class)
class NuvioPullRefreshController internal constructor(
    val state: PullToRefreshState,
    private val launch: (suspend () -> Unit) -> Unit,
    private val onRefresh: suspend () -> Unit,
) {
    var isRefreshing by mutableStateOf(false)
        private set

    fun refresh() {
        if (isRefreshing) return
        isRefreshing = true
        launch {
            try {
                val started = kotlin.time.TimeSource.Monotonic.markNow()
                onRefresh()
                val remaining = MinRefreshSpinMillis - started.elapsedNow().inWholeMilliseconds
                if (remaining > 0) delay(remaining)
            } finally {
                isRefreshing = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberNuvioPullRefreshController(onRefresh: suspend () -> Unit): NuvioPullRefreshController {
    val state = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    val latestOnRefresh by androidx.compose.runtime.rememberUpdatedState(onRefresh)
    return remember(state, scope) {
        NuvioPullRefreshController(
            state = state,
            launch = { block -> scope.launch { block() } },
            onRefresh = { latestOnRefresh() },
        )
    }
}

/**
 * The pull gesture for a scrolling list. Put it after any modifier that eats the top overscroll
 * (like `nuvioNoTopOverscroll`), so it sees the pull first.
 */
@OptIn(ExperimentalMaterial3Api::class)
fun Modifier.nuvioPullToRefresh(controller: NuvioPullRefreshController): Modifier =
    pullToRefresh(
        isRefreshing = controller.isRefreshing,
        state = controller.state,
        onRefresh = controller::refresh,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuvioPullRefreshIndicator(
    controller: NuvioPullRefreshController,
    modifier: Modifier = Modifier,
) {
    NuvioPullRefreshIndicator(
        pullFraction = controller.state.distanceFraction,
        isRefreshing = controller.isRefreshing,
        modifier = modifier,
    )
}

/**
 * A small round badge that slides down with the pull, its ring filling in the theme accent up to
 * the threshold (with a tick of haptic there), then spinning while [isRefreshing].
 */
@Composable
fun NuvioPullRefreshIndicator(
    pullFraction: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val reachedThreshold = pullFraction >= 1f
    LaunchedEffect(reachedThreshold) {
        if (reachedThreshold && !isRefreshing) {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
    }
    if (!isRefreshing && pullFraction <= 0.02f) return

    val tokens = MaterialTheme.nuvio
    val accent = tokens.colors.accent
    val travelPx = with(LocalDensity.current) { PullIndicatorTravel.toPx() }
    val shown = if (isRefreshing) 1f else pullFraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .offset { IntOffset(0, (pullFraction.coerceIn(0f, 1.4f) * travelPx).roundToInt()) }
            .alpha(shown)
            .size(36.dp)
            .clip(CircleShape)
            .background(tokens.colors.surfaceElevated),
        contentAlignment = Alignment.Center,
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = accent,
                strokeWidth = 2.dp,
            )
        } else {
            CircularProgressIndicator(
                progress = { pullFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .size(20.dp)
                    .rotate(pullFraction * 180f),
                color = accent,
                strokeWidth = 2.dp,
                trackColor = Color.Transparent,
            )
        }
    }
}
