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
 * Draws balanced extruded Neumorphism with muted light glare and an optional soft gray border.
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
            val offsetPx = shadowOffset.toPx()
            val blurPx = blurRadius.toPx().coerceAtLeast(1f)

            drawIntoCanvas { canvas ->
                val darkPaint = Paint().apply {
                    asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = shadowDark.copy(alpha = 0.68f).toArgb()
                        maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                    }
                }
                val lightPaint = Paint().apply {
                    asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = shadowLight.copy(alpha = 0.56f).toArgb()
                        maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                    }
                }

                // Bottom-right soft shadow
                canvas.drawRoundRect(
                    left = offsetPx,
                    top = offsetPx,
                    right = size.width + offsetPx,
                    bottom = size.height + offsetPx,
                    radiusX = radiusPx,
                    radiusY = radiusPx,
                    paint = darkPaint,
                )

                // Top-left muted light rim
                canvas.drawRoundRect(
                    left = -offsetPx * 0.85f,
                    top = -offsetPx * 0.85f,
                    right = size.width - offsetPx * 0.85f,
                    bottom = size.height - offsetPx * 0.85f,
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
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = borderWidth.toPx()),
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
                    colors = listOf(shadowDark.copy(alpha = 0.50f), Color.Transparent),
                    startY = 0f,
                    endY = depthPx * 2.1f,
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                size = Size(size.width, (depthPx * 2.2f).coerceAtMost(size.height)),
            )

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(shadowDark.copy(alpha = 0.42f), Color.Transparent),
                    startX = 0f,
                    endX = depthPx * 2.1f,
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                size = Size((depthPx * 2.2f).coerceAtMost(size.width), size.height),
            )

            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        shadowDark.copy(alpha = 0.35f),
                        Color.Transparent,
                        shadowLight.copy(alpha = 0.52f),
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = 1.1.dp.toPx()),
            )

            if (borderColor != null && borderWidth > 0.dp) {
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = borderWidth.toPx()),
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
