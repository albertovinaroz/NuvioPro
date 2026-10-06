package com.nuvio.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Height of [NuvioCompactTitleBar]'s solid part: the status bar plus an iOS navigation bar row. */
@Composable
fun nuvioCompactTitleBarHeight(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 44.dp

/**
 * The compact bar iOS shows once a page's large title scrolls up under the status bar: a soft fade
 * of the page background with the title small and centered, fading in as the large one leaves.
 *
 * [largeTitleKey] is the key of the list item holding the large title; the bar takes over as that
 * item's bottom edge slides up beneath it.
 */
@Composable
fun NuvioCompactTitleBar(
    title: String,
    listState: LazyListState,
    largeTitleKey: Any,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val barHeight = nuvioCompactTitleBarHeight()
    val barHeightPx = with(density) { barHeight.toPx() }
    val fadePx = with(density) { 20.dp.toPx() }
    // 0 → the large title is still below the bar; 1 → it has slid fully beneath it. Fades in over
    // the last `fadePx` of the large title's bottom edge approaching the bar's.
    val titleProgress by remember(listState, largeTitleKey, barHeightPx, fadePx) {
        derivedStateOf {
            val titleItem = listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == largeTitleKey }
            if (titleItem == null) {
                if (listState.firstVisibleItemIndex > 0) 1f else 0f
            } else {
                val titleBottom = (titleItem.offset + titleItem.size).toFloat()
                ((barHeightPx + fadePx - titleBottom) / fadePx).coerceIn(0f, 1f)
            }
        }
    }
    val background = tokens.colors.background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight + 18.dp)
            .graphicsLayer { alpha = titleProgress }
            .background(
                Brush.verticalGradient(
                    0f to background,
                    0.62f to background.copy(alpha = 0.94f),
                    1f to background.copy(alpha = 0f),
                ),
            ),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
            color = tokens.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusBarTop, start = 96.dp, end = 96.dp)
                .height(44.dp)
                .wrapContentHeight(Alignment.CenterVertically)
                .graphicsLayer { translationY = (1f - titleProgress) * 6.dp.toPx() },
        )
    }
}
