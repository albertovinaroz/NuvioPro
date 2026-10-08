package com.nuvio.app.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark

/**
 * Fades and lifts a row in, [index] rows after the first, when it appears shortly after [batch]
 * started (new filter results arriving). Rows composed later — reached by scrolling, or added by
 * pagination — just show, so scrolling never replays the animation.
 */
@Composable
fun StaggeredEntrance(
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
    val liftPx = with(LocalDensity.current) { 18.dp.toPx() }
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
private const val MaxStaggeredRows = 6
private const val StaggerStepMillis = 55L
