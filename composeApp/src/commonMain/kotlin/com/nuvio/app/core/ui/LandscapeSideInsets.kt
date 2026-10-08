package com.nuvio.app.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection

/**
 * Side padding that keeps content clear of the Dynamic Island and the rounded corners on a phone
 * turned sideways, without the full safe area: iOS reports the island's side on both edges with
 * generous slack, so 70% of it still clears the island while using more of the screen. Zero in
 * portrait.
 */
@Composable
fun nuvioLandscapeSideInsets(): PaddingValues {
    val safe = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = safe.calculateStartPadding(layoutDirection) * LandscapeSafeAreaShare,
        end = safe.calculateEndPadding(layoutDirection) * LandscapeSafeAreaShare,
    )
}

private const val LandscapeSafeAreaShare = 0.7f
