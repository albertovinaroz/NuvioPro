package com.nuvio.app.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.nuvio.app.core.ui.glass.FrostedGlassBar
import com.nuvio.app.core.ui.glass.GlassBarSurface
import com.nuvio.app.core.ui.jelly.JellyMotion
import com.nuvio.app.core.ui.jelly.JellySelectionSource
import com.nuvio.app.core.ui.jelly.JellyTabRow
import com.nuvio.app.core.ui.jelly.JellyTabTargets
import com.nuvio.app.core.ui.jelly.SharedJellyMotions
import com.nuvio.app.core.ui.jelly.drawJellyGlow
import com.nuvio.app.core.ui.jelly.drawJellyPill
import com.nuvio.app.core.ui.jelly.jellyPillPath
import dev.chrisbanes.haze.HazeState
import kotlin.math.abs
import kotlin.math.max
import org.jetbrains.compose.resources.DrawableResource

internal class FloatingNavigationItem(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val drawable: DrawableResource? = null,
    val content: (@Composable (onClick: () -> Unit) -> Unit)? = null,
    /** Unselected look; the filled [icon]/[drawable] shows only under the selection pill. */
    val outlineIcon: ImageVector? = null,
    val outlineDrawable: DrawableResource? = null,
)

/**
 * Whether the pill's accent glow can be switched off. Where it cannot, the glow stays on
 * (see [FloatingNavigationBar]).
 */
internal val LocalNuvioTabletNavLayout = staticCompositionLocalOf { false }

internal expect val floatingNavigationGlowSupported: Boolean

/**
 * Set when the bar lives in a pass-through overlay that only takes touches over the bar itself:
 * anything drawn bigger than the bar from inside it (the profile switcher's popup) reports while
 * it's up, so the overlay takes touches everywhere until it goes away.
 */
val LocalOverlayTouchCapture = staticCompositionLocalOf<((Boolean) -> Unit)?> { null }

/** The jelly track's spring stiffness (see JellyMotion: 240 / 0.9), shared so the bar's own expand/zoom settle in step with it. */
private const val JellyTrackStiffness = 240f / 0.9f
private const val ExpandOnSelectDelayMs = 180L
private const val CollapsedZoom = 0.94f

@Composable
internal fun floatingNavigationBarPadding(): PaddingValues = PaddingValues(
    bottom = nuvioBottomNavigationBarInsets().asPaddingValues().calculateBottomPadding() +
        nuvioBottomNavigationExtraVerticalPadding + 8.dp,
)

// Shared by Android and iOS (the iPad pill and iPhone before iOS 26): the jelly pill with
// drag-to-select, spring motion and haze glass. Only the glass backdrop is per platform.
@Composable
internal fun FloatingNavigationBar(
    items: List<FloatingNavigationItem>,
    modifier: Modifier = Modifier,
    scrollState: NuvioNavBarScrollState? = null,
    hazeState: HazeState? = null,
    contentPadding: PaddingValues = floatingNavigationBarPadding(),
    compactSize: Boolean = false,
    glowEnabled: Boolean = true,
    inlineLabels: Boolean = false,
    showLabels: Boolean = true,
    expandOnSelect: Boolean = false,
    /** Bars drawn by separate Compose scenes under the same key share one pill (see [SharedJellyMotions]). */
    sharedMotionKey: String? = null,
    /**
     * A native blur view behind this (transparent) Compose canvas frosts the backdrop instead of
     * haze; [onGlassBoundsChanged] reports the glass's on-screen bounds, in window pixels and with
     * every zoom/jelly transform applied, each frame they change so that view can track it.
     */
    nativeBackdrop: Boolean = false,
    onGlassBoundsChanged: ((Rect) -> Unit)? = null,
) {
    if (items.isEmpty()) return
    val showGlow = !floatingNavigationGlowSupported || glowEnabled
    val glowStrength by animateFloatAsState(
        targetValue = if (showGlow) 1f else 0f,
        animationSpec = tween(420, easing = NuvioTokens.Motion.standard),
        label = "nav_glow_strength",
    )
    val tokens = MaterialTheme.nuvio
    val accentColor = tokens.colors.accent
    val selectedSurface = accentColor.copy(alpha = NuvioTokens.Opacity.selected)
    // How expanded the bar is (scroll-driven); labels follow it unless they're switched off, in
    // which case the bar still widens/narrows but stays icon-only and icon-height.
    val targetExpansion = scrollState?.labelVisibility ?: 1f
    // Read only from layout/draw below (and via labelFraction, only when labels are shown), so the
    // expand/zoom doesn't recompose the whole bar — both tab rows included — on every frame.
    val expansionState = animateFloatAsState(
        targetValue = targetExpansion,
        // Expanding springs slightly past full width for a "zoom in" pop; collapsing stays a plain
        // tween — an undershoot below 0 would shrink label slots to negative heights.
        animationSpec = if (targetExpansion >= 1f) {
            spring(dampingRatio = 0.72f, stiffness = JellyTrackStiffness)
        } else {
            tween(NuvioTokens.Motion.sheetEnterMillis, easing = NuvioTokens.Motion.standard)
        },
        label = "jelly_labels",
    )
    val labelFraction by remember(showLabels, expansionState) {
        derivedStateOf { if (showLabels) expansionState.value.coerceIn(0f, 1f) else 0f }
    }
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val selectedIndex = items.indexOfFirst { it.selected }
    val visualSelectedIndex = visualNavIndex(selectedIndex, items.size, isRtl)
    val motion = remember(items.size, isRtl, sharedMotionKey) {
        if (sharedMotionKey != null) {
            SharedJellyMotions.obtain("$sharedMotionKey/$isRtl", visualSelectedIndex, items.size)
        } else {
            JellyMotion(visualSelectedIndex, items.size)
        }
    }
    // Not state: only the draw pass that reports the glass bounds reads it.
    val glassCoordinates = remember { object { var value: LayoutCoordinates? = null } }
    val currentItems by rememberUpdatedState(items)
    val currentIsRtl by rememberUpdatedState(isRtl)
    val density = LocalDensity.current

    SideEffect {
        if (visualSelectedIndex >= 0 && JellySelectionSource.lastDragCommit == visualSelectedIndex) {
            motion.snap(visualSelectedIndex)
        }
    }
    LaunchedEffect(visualSelectedIndex, items.size, motion) {
        if (JellySelectionSource.lastDragCommit == visualSelectedIndex) {
            motion.snap(visualSelectedIndex)
        } else {
            motion.sync(visualSelectedIndex)
        }
        if (expandOnSelect && scrollState != null && scrollState.labelVisibility < 1f) {
            // Lets the pill's selection pop play first, then expands/zooms the bar as a follow-on
            // beat rather than both competing at once.
            kotlinx.coroutines.delay(ExpandOnSelectDelayMs)
            scrollState.expand()
        }
    }
    LaunchedEffect(motion, motion.running) {
        while (motion.running) withFrameNanos(motion::tick)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding)
            .layout { measurable, constraints ->
                val horizontal = (58.dp - 30.dp * expansionState.value).roundToPx().coerceAtLeast(0) * 2
                val placeable = measurable.measure(constraints.offset(horizontal = -horizontal))
                val width = constraints.constrainWidth(placeable.width + horizontal)
                layout(width, constraints.constrainHeight(placeable.height)) {
                    placeable.place((width - placeable.width) / 2, 0)
                }
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val expansion = expansionState.value
                    val maxWidth = if (inlineLabels) {
                        // Inline (tablet) pill: expanded fits icon + label per tab, compact shrinks
                        // to icon-only slots instead of keeping most of the expanded width.
                        val compactWidth = 64.dp * items.size + 8.dp
                        compactWidth + (640.dp - compactWidth) * expansion
                    } else {
                        400.dp
                    }.roundToPx()
                    val fraction = if (showLabels) expansion.coerceIn(0f, 1f) else 0f
                    val trackHeight = if (inlineLabels) 48.dp + 4.dp * fraction
                    else 48.dp + (if (compactSize) 8.dp else 16.dp) * fraction
                    val width = constraints.constrainWidth(
                        if (constraints.hasBoundedWidth) minOf(constraints.maxWidth, maxWidth) else maxWidth,
                    )
                    val height = constraints.constrainHeight(trackHeight.roundToPx())
                    val placeable = measurable.measure(Constraints.fixed(width, height))
                    layout(width, height) { placeable.place(0, 0) }
                }
                .graphicsLayer {
                    // The bar's size follows the same animated value as its width, so expanding
                    // reads as one continuous zoom-in (with the spring's slight overshoot as the
                    // pop) instead of a separate scale animation, out of step with the width.
                    val zoom = CollapsedZoom + (1f - CollapsedZoom) * expansionState.value.coerceAtLeast(0f)
                    scaleX = zoom
                    scaleY = zoom
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .onSizeChanged {
                    motion.resize(it.width / density.density, it.height / density.density, items.size)
                }
                .pointerInput(motion, density, items.size, isRtl) {
                    detectJellyTabGestures(motion, density.density, { currentItems }, { currentIsRtl })
                },
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val frame = motion.frame
                        scaleX = frame.trackScale
                        scaleY = frame.trackScale
                    },
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val frame = motion.frame
                            transformOrigin = TransformOrigin(
                                if (size.width > 0) frame.originX * density.density / size.width else 0.5f,
                                0.5f,
                            )
                            scaleX = frame.trackScaleX
                            translationY = frame.trackOffsetY * density.density
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { translationX = motion.frame.panelOffset * density.density },
                    ) {
                        Box(
                            Modifier.matchParentSize()
                                .then(
                                    if (onGlassBoundsChanged != null) {
                                        Modifier.onPlaced { glassCoordinates.value = it }
                                    } else {
                                        Modifier
                                    },
                                )
                                .clip(RoundedCornerShape(50))
                                .drawWithContent {
                                    drawContent()
                                    drawJellyGlow(motion.frame, accentColor.copy(alpha = accentColor.alpha * glowStrength))
                                    if (onGlassBoundsChanged != null) {
                                        // Read here so a zoom (an ancestor layer, which alone
                                        // wouldn't redraw this) still re-reports the bounds.
                                        expansionState.value
                                        glassCoordinates.value
                                            ?.takeIf { it.isAttached }
                                            ?.let { onGlassBoundsChanged(it.boundsInWindow()) }
                                    }
                                },
                        ) {
                            if (nativeBackdrop) {
                                FrostedGlassBar(null, Modifier.matchParentSize(), glowStrength, hazedFillAlpha = 0.12f, backdropProvided = true)
                            } else {
                                GlassBarSurface(hazeState, Modifier.matchParentSize(), glowStrength)
                            }
                        }
                        Box(
                            Modifier.matchParentSize().drawWithContent {
                                if (selectedIndex >= 0) {
                                    clipPath(jellyPillPath(motion.frame, items.size), ClipOp.Difference) {
                                        this@drawWithContent.drawContent()
                                    }
                                } else {
                                    drawContent()
                                }
                            },
                        ) {
                            JellyTabRow(items, labelFraction, motion, active = false, compactSize = compactSize, modifier = Modifier.matchParentSize(), inlineLabels = inlineLabels)
                        }
                        if (selectedIndex >= 0) {
                            Box(
                                Modifier.matchParentSize()
                                    .clearAndSetSemantics {}
                                    .drawWithContent {
                                        drawJellyPill(
                                            motion.frame,
                                            items.size,
                                            selectedSurface,
                                            accentColor.copy(alpha = accentColor.alpha * glowStrength),
                                        ) { drawContent() }
                                    },
                            ) {
                                JellyTabRow(items, labelFraction, motion, active = true, compactSize = compactSize, modifier = Modifier.matchParentSize(), inlineLabels = inlineLabels)
                            }
                        }
                        JellyTabTargets(items, labelFraction, motion, compactSize, Modifier.matchParentSize(), inlineLabels = inlineLabels)
                    }
                }
            }
        }
    }
}

internal fun visualNavIndex(logicalIndex: Int, count: Int, isRtl: Boolean): Int =
    if (logicalIndex in 0 until count && isRtl) count - 1 - logicalIndex else logicalIndex

internal fun logicalNavIndex(visualIndex: Int, count: Int, isRtl: Boolean): Int =
    if (visualIndex in 0 until count && isRtl) count - 1 - visualIndex else visualIndex

internal suspend fun PointerInputScope.detectJellyTabGestures(
    motion: JellyMotion,
    density: Float,
    currentItems: () -> List<FloatingNavigationItem>,
    isRtl: () -> Boolean,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        motion.begin(down.position.x / density, down.position.y / density)
        var claimed = false
        var finished = false
        try {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (!motion.dragging) {
                    finished = true
                    break
                }
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (event.changes.count { it.pressed } > 1) break
                val delta = change.position - down.position
                if (max(abs(delta.x), abs(delta.y)) > viewConfiguration.touchSlop) claimed = true
                if (claimed) change.consume()
                awaitPointerEvent(PointerEventPass.Main)
                if (!motion.dragging) {
                    finished = true
                    break
                }
                if (change.isConsumed && !claimed) {
                    // A plain tap released onto a tab: that tab's own selectable consumed the up
                    // and handles the click itself. Finish the motion toward it rather than letting
                    // the cancel below snap the pill back to the old tab until the new selection
                    // lands (late on iOS, via the native tab bridge) — the back-and-forth flicker.
                    // A long press (the profile tab's switcher) isn't a selection, so it still
                    // falls through to the cancel and returns to the selected tab.
                    val isTap = change.uptimeMillis - down.uptimeMillis < viewConfiguration.longPressTimeoutMillis
                    if (!change.pressed && isTap) {
                        motion.finish()
                        finished = true
                    }
                    break
                }
                motion.drag(delta.x / density, delta.y / density)
                if (!change.pressed) {
                    val visualIndex = motion.finish()
                    val items = currentItems()
                    val logicalIndex = logicalNavIndex(visualIndex, items.size, isRtl())
                    finished = true
                    items.getOrNull(logicalIndex)?.onClick?.invoke()
                    change.consume()
                    break
                }
            }
        } finally {
            if (!finished) {
                val items = currentItems()
                val selectedIndex = items.indexOfFirst { it.selected }
                motion.cancel(visualNavIndex(selectedIndex, items.size, isRtl()))
            }
        }
    }
}
