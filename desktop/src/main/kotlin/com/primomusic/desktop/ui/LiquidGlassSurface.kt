package com.primomusic.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.effects.blur as backdropBlur
import com.kashif_e.backdrop.effects.lens
import com.kashif_e.backdrop.effects.vibrancy
import com.kashif_e.backdrop.highlight.Highlight

/**
 * A Skia-backed liquid-glass surface for Compose Desktop.
 *
 * drawBackdrop() samples pixels that were already rendered into [backdrop]. The
 * lens pass then displaces those pixels (real refraction-like distortion), the
 * small blur removes sampling aliasing and the specular pass is drawn after the
 * sampled background. Child content is composed last, so text/icons stay sharp.
 *
 * The effect is static by design: no infinite animation, no per-frame bitmap
 * allocation and no CPU readback. That keeps the bottom player cheap on Windows.
 */
@Composable
fun LiquidGlassSurface(
    enabled: Boolean,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    shape: Shape,
    solidColor: Color,
    tint: Color,
    borderColor: Color,
    accent: Color,
    refractionHeight: Dp = 18.dp,
    refractionAmount: Dp = 30.dp,
    content: @Composable () -> Unit,
) {
    val baseModifier = if (enabled) {
        modifier
            .clip(shape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    // Keep blur deliberately small: refraction is the primary
                    // optical effect, not a frosted-glass blur shortcut.
                    vibrancy()
                    backdropBlur(3.dp.toPx())
                    lens(
                        refractionHeight = refractionHeight.toPx(),
                        refractionAmount = refractionAmount.toPx(),
                        chromaticAberration = true,
                    )
                },
                highlight = { Highlight.Ambient },
                onDrawSurface = {
                    drawRect(tint.copy(alpha = 0.10f))
                },
            )
            .drawWithCache {
                val stroke = 1.15.dp.toPx()
                val radius = 25.dp.toPx()
                val specular = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.50f),
                        accent.copy(alpha = 0.22f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.10f),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height * 0.62f),
                )
                val innerGlow = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.Transparent,
                        accent.copy(alpha = 0.035f),
                    ),
                )

                onDrawWithContent {
                    drawContent()
                    drawRoundRect(
                        brush = innerGlow,
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                    )
                    drawRoundRect(
                        brush = specular,
                        topLeft = Offset(stroke / 2f, stroke / 2f),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                        style = Stroke(stroke),
                    )
                    // A short top-edge glint gives the material a polished,
                    // three-dimensional reflection without animating every frame.
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.42f),
                                Color.Transparent,
                            ),
                        ),
                        start = Offset(size.width * 0.14f, stroke * 1.7f),
                        end = Offset(size.width * 0.68f, stroke * 1.7f),
                        strokeWidth = 1.2.dp.toPx(),
                    )
                }
            }
    } else {
        modifier
            .clip(shape)
            .background(solidColor)
            .border(1.dp, borderColor, shape)
    }

    Box(baseModifier) {
        content()
    }
}
