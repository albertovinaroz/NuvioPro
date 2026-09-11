package com.nuvio.app.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.episodes_cd_watched
import org.jetbrains.compose.resources.stringResource

@Composable
fun NuvioWatchedBadge(
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    Box(
        modifier = modifier
            .size(NuvioTokens.Icon.md)
            .clip(tokens.shapes.avatar)
            .background(palette.accentBrush()),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Visibility,
            contentDescription = stringResource(Res.string.episodes_cd_watched),
            tint = palette.onSecondary,
            modifier = Modifier.size(NuvioTokens.Icon.xs),
        )
    }
}

@Composable
fun NuvioAnimatedWatchedBadge(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        NuvioWatchedBadge()
    }
}

@Composable
fun BoxScope.NuvioPosterWatchedOverlay(
    isWatched: Boolean,
    modifier: Modifier = Modifier,
    padding: Dp = NuvioTokens.Space.s6,
) {
    NuvioAnimatedWatchedBadge(
        isVisible = isWatched,
        modifier = modifier
            .align(Alignment.TopEnd)
            .padding(padding),
    )
}

// A fixed white rather than the theme's own accent (MaterialTheme.colorScheme.primary), which
// doubles as the app's decorative brand color and shifts per theme — a plain white dot reads
// consistently as a status indicator no matter which theme is active.
private val RecentlyAddedDotColor = Color.White

@Composable
fun NuvioRecentlyAddedDot(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(RecentlyAddedDotColor),
    )
}

@Composable
fun NuvioAnimatedRecentlyAddedDot(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        NuvioRecentlyAddedDot()
    }
}

/** Marks a poster recently saved to the library — mirrors the unread dot on the notification feed. */
@Composable
fun BoxScope.NuvioPosterRecentlyAddedOverlay(
    isRecentlyAdded: Boolean,
    modifier: Modifier = Modifier,
    padding: Dp = NuvioTokens.Space.s6,
) {
    NuvioAnimatedRecentlyAddedDot(
        isVisible = isRecentlyAdded,
        modifier = modifier
            .align(Alignment.TopStart)
            .padding(padding),
    )
}
