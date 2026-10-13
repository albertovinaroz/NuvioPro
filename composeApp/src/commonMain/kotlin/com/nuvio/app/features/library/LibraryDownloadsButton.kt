package com.nuvio.app.features.library

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.gradientMask
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.features.downloads.DownloadsRepository
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_settings_root_downloads_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LibraryDownloadsButton(onClick: () -> Unit) {
    val downloads by remember {
        DownloadsRepository.ensureLoaded()
        DownloadsRepository.uiState
    }.collectAsStateWithLifecycle()
    val hasUnseenCompleted by DownloadsRepository.hasUnseenCompleted.collectAsStateWithLifecycle()

    IconButton(onClick = onClick) {
        when {
            downloads.downloadingProgress != null -> DownloadProgressRing(progress = downloads.downloadingProgress ?: 0f) {
                FlowingDownloadIcon(iconSize = 15.dp)
            }
            hasUnseenCompleted -> DownloadIcon(
                modifier = Modifier.gradientMask(MaterialTheme.themePalette.accentBrush()),
                tint = Color.White,
            )
            else -> DownloadIcon(tint = Color.White)
        }
    }
}

/** The usual download ring around [content], filling clockwise in the theme accent as [progress] (0–1) grows. */
@Composable
private fun DownloadProgressRing(progress: Float, content: @Composable () -> Unit) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "downloadsProgress",
    )
    val ringBrush = MaterialTheme.themePalette.accentBrush()
    val trackColor = Color.White.copy(alpha = 0.18f)
    Box(
        modifier = Modifier
            .size(28.dp)
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                drawCircle(
                    color = trackColor,
                    radius = size.minDimension / 2f - inset,
                    style = Stroke(width = strokeWidth),
                )
                drawArc(
                    brush = ringBrush,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun FlowingDownloadIcon(iconSize: Dp = 19.dp) {
    val palette = MaterialTheme.themePalette
    val dimAlpha = MaterialTheme.nuvio.opacity.overlayLight
    val colors = palette.accentGradient.takeIf { it.size >= 2 }
        ?: listOf(palette.secondary, palette.secondary.copy(alpha = dimAlpha))
    val phase by rememberInfiniteTransition(label = "downloadsFlow").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1600, easing = LinearEasing)),
        label = "downloadsFlowPhase",
    )
    DownloadIcon(
        modifier = Modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val offset = phase * size.height * 2f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = colors,
                        start = Offset(0f, offset),
                        end = Offset(0f, offset + size.height),
                        tileMode = TileMode.Mirror,
                    ),
                    blendMode = BlendMode.SrcIn,
                )
            },
        tint = Color.White,
        size = iconSize,
    )
}

@Composable
private fun DownloadIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 19.dp,
) {
    Icon(
        imageVector = Icons.Rounded.Download,
        contentDescription = stringResource(Res.string.compose_settings_root_downloads_title),
        modifier = Modifier.size(size).then(modifier),
        tint = tint,
    )
}
