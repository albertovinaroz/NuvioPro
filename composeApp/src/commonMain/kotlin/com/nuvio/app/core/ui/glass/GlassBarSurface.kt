package com.nuvio.app.core.ui.glass

import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

internal val GlassBarShape = RoundedCornerShape(50)

/**
 * The backdrop of the floating navigation pill: Kyant's liquid glass (vibrancy, blur and lens
 * refraction) with the [LiquidGlassStyle] flags and the tunable [settings] numbers (already
 * filtered by the material, see forNavigationMaterial). Without a backdrop, on devices without
 * RenderEffect or with the solid material it is [SolidGlassBar].
 */
@Composable
internal fun GlassBarSurface(
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    style: LiquidGlassStyle = LiquidGlassStyle.Default,
    settings: LiquidGlassEffectSettings = LiquidGlassEffectDefaults.floatingBar,
    exportedBackdrop: LayerBackdrop? = null,
) {
    if (backdrop != null && style.usesBackdrop && isLiquidGlassSupported()) {
        LiquidGlassBar(backdrop, style, settings, exportedBackdrop, modifier)
    } else {
        SolidGlassBar(modifier, style.edgeHighlight)
    }
}

@Composable
private fun LiquidGlassBar(
    backdrop: Backdrop,
    style: LiquidGlassStyle,
    settings: LiquidGlassEffectSettings,
    exportedBackdrop: LayerBackdrop?,
    modifier: Modifier,
) {
    Box(
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { GlassBarShape },
            effects = {
                if (style.vibrancy) vibrancy()
                if (settings.blurRadiusDp > 0f) blur(settings.blurRadiusDp.dp.toPx())
                if (settings.refractionHeightDp > 0f && settings.refractionAmountDp > 0f) {
                    lens(
                        refractionHeight = settings.refractionHeightDp.dp.toPx().coerceAtMost(size.minDimension / 2f),
                        refractionAmount = settings.refractionAmountDp.dp.toPx(),
                        depthEffect = settings.depthEffect,
                        chromaticAberration = settings.chromaticAberration,
                    )
                }
            },
            highlight = if (style.edgeHighlight) DefaultGlassHighlight else null,
            shadow = if (style.shadow) DefaultGlassShadow else null,
            exportedBackdrop = exportedBackdrop,
            onDrawSurface = {
                drawRect(GlassSurfaceColor.copy(alpha = settings.tintOpacity))
            },
        ),
    )
}

/**
 * Haze blur, a dark fill and a thin top-lit edge: the glass bar without the refraction shader.
 * [backdropProvided] means something outside Compose (a native blur view behind the canvas)
 * already frosts the backdrop, so only the light fill and edge are drawn.
 */
@Composable
internal fun FrostedGlassBar(
    hazeState: HazeState?,
    modifier: Modifier,
    glowStrength: Float,
    hazedFillAlpha: Float = 0.55f,
    backdropProvided: Boolean = false,
) {
    Box(
        modifier
            .then(if (hazeState != null) Modifier.barBackdrop(hazeState) else Modifier)
            .drawWithCache {
                val fill = GlassSurfaceColor.copy(alpha = if (hazeState != null || backdropProvided) hazedFillAlpha else 0.82f)
                val edge = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.27f), Color.White.copy(alpha = 0.02f)),
                )
                val width = 0.75.dp.toPx()
                onDrawBehind {
                    drawRect(fill)
                    drawRoundRect(
                        brush = edge,
                        topLeft = Offset(width / 2, width / 2),
                        size = Size(size.width - width, size.height - width),
                        cornerRadius = CornerRadius((size.height - width) / 2),
                        style = Stroke(width),
                        alpha = glowStrength,
                    )
                }
            },
    )
}

internal fun Modifier.barBackdrop(hazeState: HazeState): Modifier = hazeEffect(state = hazeState) {
    blurRadius = 24.dp
    backgroundColor = GlassSurfaceColor
    tints = listOf(HazeTint(Color.Transparent))
    noiseFactor = 0f
}
