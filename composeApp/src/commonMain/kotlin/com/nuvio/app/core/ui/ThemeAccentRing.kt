package com.nuvio.app.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A slow-rotating sweep-gradient ring in the active app theme's accent colors, drawn as a stroke
 * (not a filled circle) so whatever sits behind it — a photo, a fallback color — shows through the
 * gap naturally instead of needing a solid mask color to hide a seam against. Follows the user's
 * chosen app theme (Settings > Appearance) rather than a fixed color — gold for the Gold theme,
 * jade for Jade, etc. — repeating the first stop at the end closes the sweep loop cleanly
 * regardless of how many stops that theme's own gradient has.
 *
 * Shared between Profile Insights' hero avatar and the "who's watching"/switch-profile grid, so
 * every profile avatar in the app reads as a small, living accent rather than a flat static
 * border — size it to match whatever avatar it surrounds via [modifier].
 */
@Composable
fun ThemeAccentRing(
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.5.dp,
    rotationMillis: Int = 3200,
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
    val infiniteTransition = rememberInfiniteTransition(label = "themeAccentRing")
    val ringAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "themeAccentRingAngle",
    )
    Canvas(
        modifier = modifier.graphicsLayer { rotationZ = ringAngle },
    ) {
        val strokeWidthPx = strokeWidth.toPx()
        drawCircle(
            brush = Brush.sweepGradient(ringColors),
            radius = (size.minDimension - strokeWidthPx) / 2f,
            style = Stroke(width = strokeWidthPx),
        )
    }
}
