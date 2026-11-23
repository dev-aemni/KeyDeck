package com.example.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val RgbSpectrumColors = listOf(
    Color(0xFFFF4D6D),
    Color(0xFFFFB703),
    Color(0xFF06D6A0),
    Color(0xFF38BDF8),
    Color(0xFF6366F1),
    Color(0xFFEC4899),
    Color(0xFFFF4D6D),
)

/**
 * Self-contained keycap surface for tightly spaced keyboard grids.
 * Strictly clipped to its own rounded shape so key shadows/highlights NEVER bleed out
 * onto neighboring keys or overlap adjacent keycaps.
 */
fun Modifier.neumorphicKeyCap(
    surfaceColor: Color = NeuBg,
    shadowLight: Color = NeuShadowLight,
    shadowDark: Color = NeuShadowDark,
    cornerRadius: Dp = 10.dp,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
): Modifier = composed {
    this
        .clip(RoundedCornerShape(cornerRadius))
        .drawBehind {
            val radiusPx = cornerRadius.toPx()
            val strokePx = 1.dp.toPx()

            // Crisp keycap fill strictly inside bounds
            drawRoundRect(
                color = surfaceColor,
                cornerRadius = CornerRadius(radiusPx, radiusPx),
            )

            // Subtle internal top bevel highlight (no outward bleed)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        shadowLight.copy(alpha = (0.34f * surfaceColor.alpha).coerceIn(0.05f, 0.45f)),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = (size.height * 0.24f).coerceAtLeast(2f),
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
            )

            // Tactile bottom 3D keycap skirt strictly inside bottom edge
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        shadowDark.copy(alpha = (0.38f * surfaceColor.alpha).coerceIn(0.06f, 0.48f)),
                    ),
                    startY = (size.height * 0.72f).coerceAtLeast(1f),
                    endY = size.height,
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
            )

            // Delicate top-edge specular hairline for keycap definition
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        shadowLight.copy(alpha = (0.28f * surfaceColor.alpha).coerceIn(0.04f, 0.35f)),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = size.height * 0.55f,
                ),
                topLeft = Offset(strokePx * 0.5f, strokePx * 0.5f),
                size = Size(
                    (size.width - strokePx).coerceAtLeast(1f),
                    (size.height - strokePx).coerceAtLeast(1f),
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = strokePx),
            )

            if (borderColor != null && borderWidth > 0.dp) {
                val bPx = borderWidth.toPx()
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(bPx * 0.5f, bPx * 0.5f),
                    size = Size(
                        (size.width - bPx).coerceAtLeast(1f),
                        (size.height - bPx).coerceAtLeast(1f),
                    ),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = bPx),
                )
            }
        }
}

/**
 * Draws balanced extruded Neumorphism with restrained shadow spread for standalone cards/panels.
 */
fun Modifier.neumorphicRaised(
    surfaceColor: Color = NeuBg,
    shadowLight: Color = NeuShadowLight,
    shadowDark: Color = NeuShadowDark,
    cornerRadius: Dp = 14.dp,
    shadowOffset: Dp = 4.dp,
    blurRadius: Dp = 7.dp,
    borderColor: Color? = NeuKeyBorder.copy(alpha = 0.52f),
    borderWidth: Dp = 1.dp,
): Modifier = composed {
    this
        .drawBehind {
            val radiusPx = cornerRadius.toPx()
            val offsetPx = shadowOffset.toPx().coerceAtMost(10f)
            val blurPx = blurRadius.toPx().coerceIn(1f, 14f)

            drawIntoCanvas { canvas ->
                val darkPaint = Paint().apply {
                    asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = shadowDark.copy(alpha = 0.48f).toArgb()
                        maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                    }
                }
                val lightPaint = Paint().apply {
                    asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = shadowLight.copy(alpha = 0.32f).toArgb()
                        maskFilter = BlurMaskFilter(blurPx * 0.8f, BlurMaskFilter.Blur.NORMAL)
                    }
                }

                // Bottom-right soft shadow
                canvas.drawRoundRect(
                    left = offsetPx * 0.65f,
                    top = offsetPx * 0.65f,
                    right = size.width + offsetPx * 0.65f,
                    bottom = size.height + offsetPx * 0.65f,
                    radiusX = radiusPx,
                    radiusY = radiusPx,
                    paint = darkPaint,
                )

                // Top-left gentle light rim
                canvas.drawRoundRect(
                    left = -offsetPx * 0.45f,
                    top = -offsetPx * 0.45f,
                    right = size.width - offsetPx * 0.45f,
                    bottom = size.height - offsetPx * 0.45f,
                    radiusX = radiusPx,
                    radiusY = radiusPx,
                    paint = lightPaint,
                )
            }

            drawRoundRect(
                color = surfaceColor,
                cornerRadius = CornerRadius(radiusPx, radiusPx),
            )

            if (borderColor != null && borderWidth > 0.dp) {
                val bPx = borderWidth.toPx()
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(bPx * 0.5f, bPx * 0.5f),
                    size = Size(
                        (size.width - bPx).coerceAtLeast(1f),
                        (size.height - bPx).coerceAtLeast(1f),
                    ),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = bPx),
                )
            }
        }
        .clip(RoundedCornerShape(cornerRadius))
}

/**
 * Draws concave inset Neumorphism for pressed / active keys and recessed pads.
 */
fun Modifier.neumorphicInset(
    surfaceColor: Color = NeuBg,
    shadowLight: Color = NeuShadowLight,
    shadowDark: Color = NeuShadowDark,
    cornerRadius: Dp = 12.dp,
    depth: Dp = 4.dp,
    borderColor: Color? = NeuKeyBorder.copy(alpha = 0.58f),
    borderWidth: Dp = 1.dp,
): Modifier = composed {
    this
        .clip(RoundedCornerShape(cornerRadius))
        .background(surfaceColor)
        .drawWithContent {
            drawContent()
            val radiusPx = cornerRadius.toPx()
            val depthPx = depth.toPx()

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(shadowDark.copy(alpha = 0.45f), Color.Transparent),
                    startY = 0f,
                    endY = depthPx * 2.0f,
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                size = Size(size.width, (depthPx * 2.1f).coerceAtMost(size.height)),
            )

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(shadowDark.copy(alpha = 0.36f), Color.Transparent),
                    startX = 0f,
                    endX = depthPx * 2.0f,
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                size = Size((depthPx * 2.1f).coerceAtMost(size.width), size.height),
            )

            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        shadowDark.copy(alpha = 0.28f),
                        Color.Transparent,
                        shadowLight.copy(alpha = 0.38f),
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = 1.dp.toPx()),
            )

            if (borderColor != null && borderWidth > 0.dp) {
                val bPx = borderWidth.toPx()
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(bPx * 0.5f, bPx * 0.5f),
                    size = Size(
                        (size.width - bPx).coerceAtLeast(1f),
                        (size.height - bPx).coerceAtLeast(1f),
                    ),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = bPx),
                )
            }
        }
}

/**
 * Draws a smooth rotating RGB spectrum border around a rounded rectangle.
 */
fun Modifier.rgbAnimatedBorder(
    phaseDegrees: Float,
    cornerRadius: Dp = 12.dp,
    strokeWidth: Dp = 1.4.dp,
    alpha: Float = 0.85f,
): Modifier = this.drawWithContent {
    drawContent()
    val strokePx = strokeWidth.toPx()
    val radiusPx = cornerRadius.toPx()
    val cx = size.width / 2f
    val cy = size.height / 2f
    if (cx <= 0f || cy <= 0f) return@drawWithContent

    val colorInts = RgbSpectrumColors.map { it.copy(alpha = alpha).toArgb() }.toIntArray()
    val brush = object : ShaderBrush() {
        override fun createShader(size: Size): android.graphics.Shader {
            val shader = android.graphics.SweepGradient(cx, cy, colorInts, null)
            val matrix = android.graphics.Matrix()
            matrix.setRotate(phaseDegrees, cx, cy)
            shader.setLocalMatrix(matrix)
            return shader
        }
    }

    drawRoundRect(
        brush = brush,
        topLeft = Offset(strokePx * 0.5f, strokePx * 0.5f),
        size = Size((size.width - strokePx).coerceAtLeast(1f), (size.height - strokePx).coerceAtLeast(1f)),
        cornerRadius = CornerRadius(radiusPx, radiusPx),
        style = Stroke(width = strokePx),
    )
}
