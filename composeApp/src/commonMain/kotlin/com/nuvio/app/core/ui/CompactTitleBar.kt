package com.nuvio.app.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Height of [NuvioCompactTitleBar]'s solid part: the status bar plus an iOS navigation bar row. */
@Composable
fun nuvioCompactTitleBarHeight(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 44.dp

private val CompactTitleSize = 17.sp

/** Links a [NuvioCollapsingLargeTitle] in the list to the [NuvioCompactTitleBar] that draws it. */
@Stable
class CollapsingTitleState internal constructor() {
    internal var largeTitlePosition by mutableStateOf<Offset?>(null)
    internal var barOrigin by mutableStateOf<Offset?>(null)
    internal var barWidth by mutableFloatStateOf(0f)
}

@Composable
fun rememberCollapsingTitleState(): CollapsingTitleState = remember { CollapsingTitleState() }

/**
 * The large title's slot in the list. It isn't drawn here: [NuvioCompactTitleBar] draws the one
 * title, starting exactly over this slot and shrinking into the bar's centre as the list scrolls.
 */
@Composable
fun NuvioCollapsingLargeTitle(
    title: String,
    state: CollapsingTitleState,
    topPadding: Dp,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = NuvioTokens.Space.s4,
) {
    Box(modifier = modifier.fillMaxWidth().padding(top = topPadding, bottom = bottomPadding)) {
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.nuvio.colors.textPrimary,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .onGloballyPositioned { state.largeTitlePosition = it.positionInRoot() }
                // Stays visible until the bar can draw its copy, so there's never a blank frame.
                .drawWithContent { if (state.barOrigin == null) drawContent() },
        )
    }
}

/**
 * The compact bar iOS shows once a page's large title scrolls up under the status bar: a soft fade
 * of the page background, with the page's single title travelling from its large spot in the list
 * to the bar's centre, shrinking to navigation-bar size on the way.
 *
 * [largeTitleKey] is the key of the list item holding the [NuvioCollapsingLargeTitle].
 */
@Composable
fun NuvioCompactTitleBar(
    title: String,
    listState: LazyListState,
    largeTitleKey: Any,
    state: CollapsingTitleState,
    modifier: Modifier = Modifier,
    /** How far the background fades out below the bar; 0 when a pinned header sits right under it. */
    fadeHeight: Dp = 18.dp,
) {
    val tokens = MaterialTheme.nuvio
    val density = LocalDensity.current
    val barHeight = nuvioCompactTitleBarHeight()
    val statusBarTopPx = with(density) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
    }
    // 0 at rest; 1 once the large title has slid up under the status bar. Measured from how far
    // the title item has scrolled rather than from its bottom edge, since a short title block
    // already reaches into the bar's row at rest and would otherwise start half way through.
    val titleProgress by remember(listState, largeTitleKey, statusBarTopPx) {
        derivedStateOf {
            val titleItem = listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == largeTitleKey }
            if (titleItem == null) {
                if (listState.firstVisibleItemIndex > 0) 1f else 0f
            } else {
                // The part of the title item below the status bar: what scrolls out of view.
                val travel = (titleItem.size - statusBarTopPx).coerceAtLeast(1f)
                val scrolled = (-titleItem.offset).toFloat()
                ((scrolled - travel * 0.4f) / (travel * 0.6f)).coerceIn(0f, 1f)
            }
        }
    }
    val background = tokens.colors.background
    val largeStyle = MaterialTheme.typography.displayLarge
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight + fadeHeight)
            .onGloballyPositioned {
                state.barOrigin = it.positionInRoot()
                state.barWidth = it.size.width.toFloat()
            },
    ) {
        val backgroundBrush = remember(background, fadeHeight) {
            if (fadeHeight > 0.dp) {
                Brush.verticalGradient(
                    0f to background,
                    0.62f to background.copy(alpha = 0.94f),
                    1f to background.copy(alpha = 0f),
                )
            } else {
                Brush.verticalGradient(listOf(background, background))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight + fadeHeight)
                // Faded while drawing rather than in a graphicsLayer block, which iOS stopped
                // re-running after a rotation, leaving the title over the list with no backdrop.
                .drawBehind { drawRect(backgroundBrush, alpha = titleProgress) },
        )
        val compactScale = with(density) { CompactTitleSize.toPx() / largeStyle.fontSize.toPx() }
        val rowHeightPx = with(density) { 44.dp.toPx() }
        // Moved in the placement phase and scaled while drawing, never through a graphicsLayer
        // block: after a rotation iOS stopped re-running those blocks on scroll, freezing the
        // title wherever it was.
        Text(
            text = title,
            style = largeStyle,
            color = tokens.colors.textPrimary,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        val start = state.largeTitlePosition
                        val origin = state.barOrigin
                        if (start == null || origin == null) {
                            placeable.place(0, 0)
                            return@layout
                        }
                        val progress = FastOutSlowInEasing.transform(titleProgress)
                        val startX = start.x - origin.x
                        val startY = start.y - origin.y
                        // Centred in the bar's 44dp row below the status bar, at navigation-bar size.
                        val endX = (state.barWidth - placeable.width * compactScale) / 2f
                        val endY = statusBarTopPx + (rowHeightPx - placeable.height * compactScale) / 2f
                        placeable.place(
                            (startX + (endX - startX) * progress).roundToInt(),
                            (startY + (endY - startY) * progress).roundToInt(),
                        )
                    }
                }
                .drawWithContent {
                    if (state.largeTitlePosition == null || state.barOrigin == null) return@drawWithContent
                    val progress = FastOutSlowInEasing.transform(titleProgress)
                    scale(1f + (compactScale - 1f) * progress, pivot = Offset.Zero) {
                        this@drawWithContent.drawContent()
                    }
                },
        )
    }
}
