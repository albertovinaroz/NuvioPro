package com.nuvio.app.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_player_downloaded
import nuvio.composeapp.generated.resources.episodes_cd_watched
import org.jetbrains.compose.resources.stringResource

/** A small round status badge in the theme's accent gradient — shared by watched and downloaded. */
@Composable
private fun NuvioAccentStatusBadge(
    icon: ImageVector,
    contentDescription: String,
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
            imageVector = icon,
            contentDescription = contentDescription,
            tint = palette.onSecondary,
            modifier = Modifier.size(NuvioTokens.Icon.xs),
        )
    }
}

@Composable
fun NuvioWatchedBadge(
    modifier: Modifier = Modifier,
) {
    NuvioAccentStatusBadge(
        icon = Icons.Default.Visibility,
        contentDescription = stringResource(Res.string.episodes_cd_watched),
        modifier = modifier,
    )
}

/**
 * Pops [content] in with a small overshoot when [isVisible] turns on while it's on screen (with a
 * light haptic if [haptic]), and fades it out when it turns off. Badges that are already on when
 * first composed — opening a screen, scrolling a list — just show, so browsing never bounces.
 */
@Composable
private fun PoppingStatusBadge(
    isVisible: Boolean,
    haptic: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var wasVisible by remember { mutableStateOf(isVisible) }
    LaunchedEffect(isVisible) {
        if (haptic && isVisible && !wasVisible) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        wasVisible = isVisible
    }
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
            initialScale = 0.3f,
        ) + fadeIn(),
        exit = scaleOut(targetScale = 0.6f) + fadeOut(),
        modifier = modifier,
    ) {
        content()
    }
}

@Composable
fun NuvioAnimatedWatchedBadge(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    PoppingStatusBadge(isVisible = isVisible, haptic = true, modifier = modifier) {
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

/** Marks a poster or episode that's available offline, styled like the watched badge. */
@Composable
fun NuvioDownloadedBadge(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    PoppingStatusBadge(isVisible = isVisible, haptic = false, modifier = modifier) {
        NuvioAccentStatusBadge(
            icon = Icons.Rounded.DownloadDone,
            contentDescription = stringResource(Res.string.compose_player_downloaded),
        )
    }
}

@Composable
fun BoxScope.NuvioPosterDownloadedOverlay(
    isDownloaded: Boolean,
    modifier: Modifier = Modifier,
    padding: Dp = NuvioTokens.Space.s6,
) {
    NuvioDownloadedBadge(
        isVisible = isDownloaded,
        modifier = modifier
            .align(Alignment.BottomEnd)
            .padding(padding),
    )
}
