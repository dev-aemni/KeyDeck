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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
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
 * Classic soft matte Neumorphic keycap tailored for tightly spaced keyboard grids.
 * Keeps the original clean solid matte surface and dual-sided soft shadow + light rim,
 * while clamping exterior blur spread inside half the inter-key gap and excluding the key
 * interior so adjacent keys never overlap or muddy translucent glass keycaps.
 */
fun Modifier.neumorphicKeyCap(
    surfaceColor: Color = NeuBg,
    shadowLight: Color = NeuShadowLight,
    shadowDark: Color = NeuShadowDark,
    cornerRadius: Dp = 10.dp,
    shadowOffset: Dp = 1.6.dp,
    blurRadius: Dp = 2.4.dp,
    maxSpreadX: Dp = 1.4.dp,
    maxSpreadY: Dp = 1.8.dp,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    shadowsEnabled: Boolean = true,
): Modifier = composed {
    this
        .drawBehind {
            val radiusPx = cornerRadius.toPx()
            val cr = CornerRadius(radiusPx, radiusPx)
            val surfaceAlpha = surfaceColor.alpha.coerceIn(0f, 1f)
            val spreadXPx = maxSpreadX.toPx().coerceAtLeast(0.4f)
            val spreadYPx = maxSpreadY.toPx().coerceAtLeast(0.4f)
            val darkOffsetPx = shadowOffset.toPx().coerceAtMost(spreadXPx.coerceAtLeast(spreadYPx))
            val lightOffsetPx = darkOffsetPx * 0.78f
            val blurPx = blurRadius.toPx().coerceIn(0.8f, 8f)

            val keyPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = size.height,
                        cornerRadius = cr,
                    ),
                )
            }

            if (shadowsEnabled && surfaceAlpha > 0.06f) {
                clipRect(
                    left = -spreadXPx,
                    top = -spreadYPx,
                    right = size.width + spreadXPx,
                    bottom = size.height + spreadYPx,
                ) {
                    clipPath(keyPath, clipOp = ClipOp.Difference) {
                        drawIntoCanvas { canvas ->
                            val darkPaint = Paint().apply {
                                asFrameworkPaint().apply {
                                    isAntiAlias = true
                                    color = shadowDark.copy(alpha = (0.58f * surfaceAlpha).coerceIn(0.06f, 0.64f)).toArgb()
                                    maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                                }
                            }
                            val lightPaint = Paint().apply {
                                asFrameworkPaint().apply {
                                    isAntiAlias = true
                                    color = shadowLight.copy(alpha = (0.42f * surfaceAlpha).coerceIn(0.05f, 0.48f)).toArgb()
                                    maskFilter = BlurMaskFilter((blurPx * 0.85f).coerceAtLeast(0.8f), BlurMaskFilter.Blur.NORMAL)
                                }
                            }

                            // Bottom-right soft shadow within gutter fence
                            canvas.drawRoundRect(
                                left = darkOffsetPx,
                                top = darkOffsetPx,
                                right = size.width + darkOffsetPx,
                                bottom = size.height + darkOffsetPx,
                                radiusX = radiusPx,
                                radiusY = radiusPx,
                                paint = darkPaint,
                            )

                            // Top-left muted soft light rim within gutter fence
                            canvas.drawRoundRect(
                                left = -lightOffsetPx,
                                top = -lightOffsetPx,
                                right = size.width - lightOffsetPx,
                                bottom = size.height - lightOffsetPx,
                                radiusX = radiusPx,
                                radiusY = radiusPx,
                                paint = lightPaint,
                            )
                        }
                    }
                }
            }

            // Clean velvety matte surface (same solid Soft UI feel as original neumorphicRaised)
            drawRoundRect(
                color = surfaceColor,
                cornerRadius = cr,
            )

            // Whisper-soft edge contour so key corners look cushioned rather than flat-cut
            if (shadowsEnabled) {
                val rimStroke = 0.85.dp.toPx()
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            shadowLight.copy(alpha = (0.22f * surfaceAlpha).coerceIn(0.03f, 0.26f)),
                            Color.Transparent,
                            shadowDark.copy(alpha = (0.12f * surfaceAlpha).coerceIn(0.02f, 0.16f)),
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                    topLeft = Offset(rimStroke * 0.5f, rimStroke * 0.5f),
                    size = Size(
                        (size.width - rimStroke).coerceAtLeast(1f),
                        (size.height - rimStroke).coerceAtLeast(1f),
                    ),
                    cornerRadius = cr,
                    style = Stroke(width = rimStroke),
                )
            }

            if (borderColor != null && borderWidth > 0.dp) {
                val bPx = borderWidth.toPx()
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(bPx * 0.5f, bPx * 0.5f),
                    size = Size(
                        (size.width - bPx).coerceAtLeast(1f),
                        (size.height - bPx).coerceAtLeast(1f),
                    ),
                    cornerRadius = cr,
                    style = Stroke(width = bPx),
                )
            }
        }
        .clip(RoundedCornerShape(cornerRadius))
}

/**
 * Draws balanced extruded Neumorphism with soft light glare and an optional soft border.
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
    shadowsEnabled: Boolean = true,
): Modifier = composed {
    this
        .drawBehind {
            val radiusPx = cornerRadius.toPx()
            val cr = CornerRadius(radiusPx, radiusPx)
            val offsetPx = shadowOffset.toPx()
            val blurPx = blurRadius.toPx().coerceAtLeast(1f)
            val surfaceAlpha = surfaceColor.alpha.coerceIn(0f, 1f)

            if (shadowsEnabled) {
                val cardPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = 0f,
                            top = 0f,
                            right = size.width,
                            bottom = size.height,
                            cornerRadius = cr,
                        ),
                    )
                }

                clipPath(cardPath, clipOp = ClipOp.Difference) {
                    drawIntoCanvas { canvas ->
                        val darkPaint = Paint().apply {
                            asFrameworkPaint().apply {
                                isAntiAlias = true
                                color = shadowDark.copy(alpha = (0.64f * surfaceAlpha).coerceIn(0.08f, 0.68f)).toArgb()
                                maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                            }
                        }
                        val lightPaint = Paint().apply {
                            asFrameworkPaint().apply {
                                isAntiAlias = true
                                color = shadowLight.copy(alpha = (0.50f * surfaceAlpha).coerceIn(0.06f, 0.54f)).toArgb()
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
                            left = -offsetPx * 0.80f,
                            top = -offsetPx * 0.80f,
                            right = size.width - offsetPx * 0.80f,
                            bottom = size.height - offsetPx * 0.80f,
                            radiusX = radiusPx,
                            radiusY = radiusPx,
                            paint = lightPaint,
                        )
                    }
                }
            }

            drawRoundRect(
                color = surfaceColor,
                cornerRadius = cr,
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
                    cornerRadius = cr,
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
    shadowsEnabled: Boolean = true,
): Modifier = composed {
    this
        .clip(RoundedCornerShape(cornerRadius))
        .background(surfaceColor)
        .drawWithContent {
            drawContent()
            val radiusPx = cornerRadius.toPx()
            val depthPx = depth.toPx()

            if (shadowsEnabled) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(shadowDark.copy(alpha = 0.48f), Color.Transparent),
                        startY = 0f,
                        endY = depthPx * 2.1f,
                    ),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    size = Size(size.width, (depthPx * 2.2f).coerceAtMost(size.height)),
                )

                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(shadowDark.copy(alpha = 0.40f), Color.Transparent),
                        startX = 0f,
                        endX = depthPx * 2.1f,
                    ),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    size = Size((depthPx * 2.2f).coerceAtMost(size.width), size.height),
                )

                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            shadowDark.copy(alpha = 0.34f),
                            Color.Transparent,
                            shadowLight.copy(alpha = 0.48f),
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = 1.1.dp.toPx()),
                )
            }

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
