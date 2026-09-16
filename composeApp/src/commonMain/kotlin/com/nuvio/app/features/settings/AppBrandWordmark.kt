package com.nuvio.app.features.settings

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun AppBrandWordmark(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    icon: AppIconOption? = null,
) {
    val state by remember {
        AppIconRepository.ensureLoaded()
        AppIconRepository.state
    }.collectAsStateWithLifecycle()
    // Always the selected App Icon's own wordmark — the color theme picked under Appearance is a
    // separate setting and shouldn't change which one shows here (it used to, by matching a few
    // theme colors to a same-colored wordmark regardless of the chosen icon).
    val resource: DrawableResource = (icon ?: state.selected).wordmarkResource

    // Different color themes (e.g. per-profile Supporter+ tints) are separate baked PNGs, not a
    // single asset with a tintable color — swapping the painter outright is an abrupt hard cut
    // whenever the active profile's theme differs from the previous one. Crossfade so the wordmark
    // eases between the two colors instead of flashing.
    Crossfade(
        targetState = resource,
        animationSpec = tween(durationMillis = 350),
        label = "AppBrandWordmarkCrossfade",
        modifier = modifier,
    ) { targetResource ->
        Image(
            painter = painterResource(targetResource),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
        )
    }
}
