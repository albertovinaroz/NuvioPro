package com.nuvio.app.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A slow-rotating sweep-gradient ring in the active app theme's accent colors, drawn as a stroke
 * (not a filled circle) so whatever sits behind it — a photo, a fallback color — shows through the
 * gap naturally instead of needing a solid mask color to hide a seam against. Follows the user's
 * chosen app theme (Settings > Appearance) rather than a fixed color — gold for the Gold theme,
 * jade for Jade, etc. — repeating the first stop at the end closes the sweep loop cleanly
 * regardless of how many stops that theme's own gradient has.
 *
 * With [drawIn], the ring first draws itself on clockwise from 12 o'clock until it closes into a
 * full circle, then starts spinning — so it reads as completing a lap rather than popping in.
 */
@Composable
fun ThemeAccentRing(
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.5.dp,
    rotationMillis: Int = 3200,
    drawIn: Boolean = false,
    drawInMillis: Int = 650,
    rippleKey: Int = 0,
) {
    val themeAccentGradient = MaterialTheme.themePalette.accentGradient
    val ringColors = remember(themeAccentGradient) {
        if (themeAccentGradient.size >= 2) {
            themeAccentGradient + themeAccentGradient.first()
        } else {
            val solid = themeAccentGradient.firstOrNull() ?: Color(0xFFE8A91C)
            listOf(solid, solid)
        }
    }
    val drawProgress = remember { Animatable(if (drawIn) 0f else 1f) }
    val spinAngle = remember { Animatable(0f) }
    val ripple = remember { Animatable(0f) }
    LaunchedEffect(rippleKey) {
        if (rippleKey > 0) {
            ripple.snapTo(0f)
            ripple.animateTo(1f, tween(durationMillis = 900, easing = LinearOutSlowInEasing))
        }
    }
    LaunchedEffect(Unit) {
        if (drawIn) {
            drawProgress.animateTo(1f, tween(durationMillis = drawInMillis, easing = FastOutSlowInEasing))
            launch { ripple.animateTo(1f, tween(durationMillis = 900, easing = LinearOutSlowInEasing)) }
        }
        spinAngle.snapTo(0f)
        spinAngle.animateTo(
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = rotationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        )
    }
    Canvas(
        modifier = modifier.graphicsLayer { rotationZ = spinAngle.value },
    ) {
        val strokeWidthPx = strokeWidth.toPx()
        val radius = (size.minDimension - strokeWidthPx) / 2f
        drawArc(
            brush = Brush.sweepGradient(ringColors),
            startAngle = -90f,
            sweepAngle = 360f * drawProgress.value,
            useCenter = false,
            topLeft = Offset(size.minDimension / 2f - radius, size.minDimension / 2f - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidthPx),
        )
        run {
            val maxRadius = size.minDimension / 2f + 7.dp.toPx()
            listOf(ripple.value, ripple.value - 0.3f).forEach { p ->
                if (p > 0f && p < 1f) {
                    drawCircle(
                        color = ringColors.first(),
                        radius = radius + (maxRadius - radius) * p,
                        center = Offset(size.width / 2f, size.height / 2f),
                        alpha = (1f - p) * 0.9f,
                        style = Stroke(width = strokeWidthPx),
                    )
                }
            }
        }
    }
}
