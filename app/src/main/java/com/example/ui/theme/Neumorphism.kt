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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws soft dual-shadow extruded Neumorphism (top-left light highlight + bottom-right dark shadow).
 */
fun Modifier.neumorphicRaised(
    surfaceColor: Color = NeuBg,
    shadowLight: Color = NeuShadowLight,
    shadowDark: Color = NeuShadowDark,
    cornerRadius: Dp = 16.dp,
    shadowOffset: Dp = 5.dp,
    blurRadius: Dp = 10.dp,
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
                        color = shadowDark.copy(alpha = 0.78f).toArgb()
                        maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                    }
                }
                val lightPaint = Paint().apply {
                    asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = shadowLight.copy(alpha = 0.94f).toArgb()
                        maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                    }
                }

                // Bottom-right dark drop shadow
                canvas.drawRoundRect(
                    left = offsetPx,
                    top = offsetPx,
                    right = size.width + offsetPx,
                    bottom = size.height + offsetPx,
                    radiusX = radiusPx,
                    radiusY = radiusPx,
                    paint = darkPaint,
                )

                // Top-left bright highlight shadow
                canvas.drawRoundRect(
                    left = -offsetPx,
                    top = -offsetPx,
                    right = size.width - offsetPx,
                    bottom = size.height - offsetPx,
                    radiusX = radiusPx,
                    radiusY = radiusPx,
                    paint = lightPaint,
                )
            }

            // Crisp soft outer fill
            drawRoundRect(
                color = surfaceColor,
                cornerRadius = CornerRadius(radiusPx, radiusPx),
            )

            // Subtle directional rim highlight
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        shadowLight.copy(alpha = 0.65f),
                        Color.Transparent,
                        shadowDark.copy(alpha = 0.25f),
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = 0.85.dp.toPx()),
            )
        }
        .clip(RoundedCornerShape(cornerRadius))
}

/**
 * Draws soft concave inset Neumorphism (pressed / active / recessed input well).
 */
fun Modifier.neumorphicInset(
    surfaceColor: Color = NeuBg,
    shadowLight: Color = NeuShadowLight,
    shadowDark: Color = NeuShadowDark,
    cornerRadius: Dp = 14.dp,
    depth: Dp = 5.dp,
): Modifier = composed {
    this
        .clip(RoundedCornerShape(cornerRadius))
        .background(surfaceColor)
        .drawWithContent {
            drawContent()
            val radiusPx = cornerRadius.toPx()
            val depthPx = depth.toPx()

            // Top inner dark shadow
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(shadowDark.copy(alpha = 0.55f), Color.Transparent),
                    startY = 0f,
                    endY = depthPx * 2.2f,
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                size = Size(size.width, (depthPx * 2.4f).coerceAtMost(size.height)),
            )

            // Left inner dark shadow
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(shadowDark.copy(alpha = 0.48f), Color.Transparent),
                    startX = 0f,
                    endX = depthPx * 2.2f,
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                size = Size((depthPx * 2.4f).coerceAtMost(size.width), size.height),
            )

            // Bottom-right inner light highlight
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        shadowDark.copy(alpha = 0.45f),
                        Color.Transparent,
                        shadowLight.copy(alpha = 0.88f),
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = 1.4.dp.toPx()),
            )
        }
}
