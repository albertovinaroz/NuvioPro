package com.nuvio.app.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * A number that counts up from 0 to [target] when first shown, and glides to each new [target]
 * after that (stats that arrive late, or change). Read it as the value to display.
 */
@Composable
fun animateCountUp(target: Float, durationMillis: Int = 900): Float {
    val value = remember { Animatable(0f) }
    LaunchedEffect(target) {
        value.animateTo(target, tween(durationMillis = durationMillis, easing = FastOutSlowInEasing))
    }
    return value.value
}
