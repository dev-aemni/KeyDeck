package com.example.keyboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class WallpaperSourceType(val label: String) {
    NONE("Solid Theme"),
    PATTERN_CARBON("Carbon Weave"),
    PATTERN_DOTS("Stealth Dots"),
    PATTERN_GRID("Precision Grid"),
    PATTERN_HEX("Hex Matrix"),
    PATTERN_DIAGONAL("Brushed Diagonal"),
    PATTERN_AURORA("Aurora Glow"),
    PATTERN_WAVES("Contour Waves"),
    CUSTOM_IMAGE("Custom Image"),
}

data class KeyboardWallpaper(
    val id: String,
    val name: String,
    val sourceType: WallpaperSourceType,
    val imagePath: String = "",
    val lightness: Float = 0.16f,      // 0.04f..1.0f (~16% visible -> ~85-90% dark stealth by default)
    val saturation: Float = 0.90f,     // 0.0f..2.0f
    val contrast: Float = 1.08f,       // 0.50f..1.80f
    val brightness: Float = 0.0f,      // -0.40f..0.40f
    val tintHex: Long = 0L,            // 0L = neutral / theme default
    val tintIntensity: Float = 0.24f,  // 0.0f..0.85f
    val keyOpacity: Float = 0.64f,     // 0.20f..1.0f
    val darkBackdrop: Boolean = true,  // true = 90% dark black keyboard look
    val fitCenter: Boolean = false,    // false = Crop fill, true = Fit centered (great for logos)
    val isBuiltIn: Boolean = false,
    val createdAtMs: Long = 0L,
)

object BuiltInWallpapers {
    val presets: List<KeyboardWallpaper> = listOf(
        KeyboardWallpaper(
            id = "wp_carbon",
            name = "Dark Carbon",
            sourceType = WallpaperSourceType.PATTERN_CARBON,
            lightness = 0.20f,
            saturation = 0.70f,
            contrast = 1.15f,
            brightness = 0.0f,
            tintHex = 0L,
            tintIntensity = 0.18f,
            keyOpacity = 0.68f,
            darkBackdrop = true,
            isBuiltIn = true,
        ),
        KeyboardWallpaper(
            id = "wp_dots",
            name = "Stealth Dots",
            sourceType = WallpaperSourceType.PATTERN_DOTS,
            lightness = 0.22f,
            saturation = 0.90f,
            contrast = 1.10f,
            brightness = 0.0f,
            tintHex = 0xFF60A5FAL,
            tintIntensity = 0.28f,
            keyOpacity = 0.66f,
            darkBackdrop = true,
            isBuiltIn = true,
        ),
        KeyboardWallpaper(
            id = "wp_grid",
            name = "Blueprint Grid",
            sourceType = WallpaperSourceType.PATTERN_GRID,
            lightness = 0.18f,
            saturation = 0.85f,
            contrast = 1.12f,
            brightness = 0.0f,
            tintHex = 0xFF38BDF8L,
            tintIntensity = 0.30f,
            keyOpacity = 0.66f,
            darkBackdrop = true,
            isBuiltIn = true,
        ),
        KeyboardWallpaper(
            id = "wp_hex",
            name = "Emerald Hex",
            sourceType = WallpaperSourceType.PATTERN_HEX,
            lightness = 0.20f,
            saturation = 1.0f,
            contrast = 1.12f,
            brightness = 0.0f,
            tintHex = 0xFF10B981L,
            tintIntensity = 0.42f,
            keyOpacity = 0.65f,
            darkBackdrop = true,
            isBuiltIn = true,
        ),
        KeyboardWallpaper(
            id = "wp_diagonal",
            name = "Brushed Metal",
            sourceType = WallpaperSourceType.PATTERN_DIAGONAL,
            lightness = 0.18f,
            saturation = 0.50f,
            contrast = 1.15f,
            brightness = 0.0f,
            tintHex = 0xFF94A3B8L,
            tintIntensity = 0.25f,
            keyOpacity = 0.68f,
            darkBackdrop = true,
            isBuiltIn = true,
        ),
        KeyboardWallpaper(
            id = "wp_aurora",
            name = "Midnight Aurora",
            sourceType = WallpaperSourceType.PATTERN_AURORA,
            lightness = 0.24f,
            saturation = 1.10f,
            contrast = 1.10f,
            brightness = 0.0f,
            tintHex = 0xFF6366F1L,
            tintIntensity = 0.45f,
            keyOpacity = 0.60f,
            darkBackdrop = true,
            isBuiltIn = true,
        ),
        KeyboardWallpaper(
            id = "wp_waves",
            name = "Contour Waves",
            sourceType = WallpaperSourceType.PATTERN_WAVES,
            lightness = 0.20f,
            saturation = 0.95f,
            contrast = 1.08f,
            brightness = 0.0f,
            tintHex = 0xFF0D9488L,
            tintIntensity = 0.35f,
            keyOpacity = 0.64f,
            darkBackdrop = true,
            isBuiltIn = true,
        ),
    )

    fun resolve(activeId: String, savedWallpapers: List<KeyboardWallpaper>): KeyboardWallpaper? {
        if (activeId.isBlank()) return null
        return savedWallpapers.firstOrNull { it.id == activeId }
            ?: presets.firstOrNull { it.id == activeId }
    }
}

fun buildWallpaperColorMatrix(
    saturation: Float,
    contrast: Float,
    brightness: Float,
): ColorMatrix {
    val s = saturation.coerceIn(0f, 2f)
    val c = contrast.coerceIn(0.4f, 2.0f)
    val b = brightness.coerceIn(-0.5f, 0.5f) * 255f

    val satMatrix = ColorMatrix().apply { setToSaturation(s) }
    val translate = (1f - c) * 127.5f + b
    val contrastBrightnessMatrix = ColorMatrix(
        floatArrayOf(
            c, 0f, 0f, 0f, translate,
            0f, c, 0f, 0f, translate,
            0f, 0f, c, 0f, translate,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    contrastBrightnessMatrix.timesAssign(satMatrix)
    return contrastBrightnessMatrix
}

object WallpaperBitmapCache {
    private val cache = object : LruCache<String, ImageBitmap>(10) {}

    fun getCached(path: String): ImageBitmap? {
        if (path.isBlank()) return null
        return cache.get(path)
    }

    fun putCached(path: String, bmp: ImageBitmap) {
        if (path.isNotBlank()) {
            cache.put(path, bmp)
        }
    }

    fun evict(path: String) {
        if (path.isNotBlank()) {
            cache.remove(path)
        }
    }
}

suspend fun importWallpaperFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        val maxDim = max(bounds.outWidth, bounds.outHeight)
        if (maxDim <= 0) return@runCatching null

        var sampleSize = 1
        while (maxDim / sampleSize > 1280) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val rawBmp = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return@runCatching null

        val orientation = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val rotatedBmp = rotateBitmapIfNeeded(rawBmp, orientation)
        if (rotatedBmp !== rawBmp) {
            rawBmp.recycle()
        }

        val dir = File(context.filesDir, "wallpapers").apply { mkdirs() }
        val usePng = rotatedBmp.hasAlpha()
        val ext = if (usePng) "png" else "jpg"
        val outFile = File(dir, "wp_${UUID.randomUUID().toString().take(10)}.$ext")
        FileOutputStream(outFile).use { fos ->
            if (usePng) {
                rotatedBmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
            } else {
                rotatedBmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }
        }
        WallpaperBitmapCache.putCached(outFile.absolutePath, rotatedBmp.asImageBitmap())
        outFile.absolutePath
    }.getOrNull()
}

private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun deleteWallpaperFileIfUnused(
    removedWallpaper: KeyboardWallpaper,
    remainingWallpapers: List<KeyboardWallpaper>,
) {
    val path = removedWallpaper.imagePath
    if (path.isBlank()) return
    val stillUsed = remainingWallpapers.any { it.imagePath == path }
    if (!stillUsed) {
        WallpaperBitmapCache.evict(path)
        runCatching {
            val file = File(path)
            if (file.exists()) file.delete()
        }
    }
}

@Composable
fun KeyboardWallpaperBackdrop(
    wallpaper: KeyboardWallpaper?,
    palette: ResolvedKeyboardPalette,
    overlayAlpha: Float = 1f,
    modifier: Modifier = Modifier,
) {
    if (wallpaper == null || wallpaper.sourceType == WallpaperSourceType.NONE) {
        Box(
            modifier = modifier.background(palette.deckBg.copy(alpha = overlayAlpha)),
        )
        return
    }

    val lightness = wallpaper.lightness.coerceIn(0.03f, 1.0f)
    val baseColor = if (wallpaper.darkBackdrop) {
        lerp(palette.deckBg, Color.Black, 0.92f).copy(alpha = overlayAlpha)
    } else {
        palette.deckBg.copy(alpha = overlayAlpha)
    }

    val colorMatrix = remember(wallpaper.saturation, wallpaper.contrast, wallpaper.brightness) {
        buildWallpaperColorMatrix(
            saturation = wallpaper.saturation,
            contrast = wallpaper.contrast,
            brightness = wallpaper.brightness,
        )
    }

    Box(modifier = modifier.background(baseColor)) {
        if (wallpaper.sourceType == WallpaperSourceType.CUSTOM_IMAGE && wallpaper.imagePath.isNotBlank()) {
            var loadedBitmap by remember(wallpaper.imagePath) {
                mutableStateOf(WallpaperBitmapCache.getCached(wallpaper.imagePath))
            }
            LaunchedEffect(wallpaper.imagePath) {
                if (loadedBitmap == null) {
                    val decoded = withContext(Dispatchers.IO) {
                        runCatching {
                            val file = File(wallpaper.imagePath)
                            if (file.exists()) {
                                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                            } else {
                                null
                            }
                        }.getOrNull()
                    }
                    if (decoded != null) {
                        WallpaperBitmapCache.putCached(wallpaper.imagePath, decoded)
                        loadedBitmap = decoded
                    }
                }
            }
            val bmp = loadedBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = wallpaper.name,
                    contentScale = if (wallpaper.fitCenter) ContentScale.Fit else ContentScale.Crop,
                    alpha = lightness * overlayAlpha,
                    colorFilter = ColorFilter.colorMatrix(colorMatrix),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            val basePatternTint = if (wallpaper.tintHex != 0L) {
                Color(wallpaper.tintHex)
            } else {
                palette.accentBg
            }
            val filteredColor = applyScalarFiltersToColor(
                color = lerp(Color(0xFFE2E8F0), basePatternTint, 0.72f),
                saturation = wallpaper.saturation,
                contrast = wallpaper.contrast,
                brightness = wallpaper.brightness,
            )
            PatternWallpaperCanvas(
                sourceType = wallpaper.sourceType,
                primaryColor = filteredColor,
                accentColor = applyScalarFiltersToColor(
                    color = palette.accentBg,
                    saturation = wallpaper.saturation,
                    contrast = wallpaper.contrast,
                    brightness = wallpaper.brightness,
                ),
                lightness = lightness * overlayAlpha,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (wallpaper.tintHex != 0L && wallpaper.tintIntensity > 0.01f) {
            val tintAlpha = (wallpaper.tintIntensity.coerceIn(0f, 0.85f) *
                (0.28f + lightness * 0.72f) * overlayAlpha).coerceIn(0f, 0.85f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(wallpaper.tintHex).copy(alpha = tintAlpha)),
            )
        }

        if (wallpaper.darkBackdrop) {
            // Subtle bottom/edge vignette keeps stealth ~90% dark keyboard edges clean
            val edgeAlpha = ((1f - lightness) * 0.35f * overlayAlpha).coerceIn(0f, 0.45f)
            if (edgeAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = edgeAlpha)),
                )
            }
        }
    }
}

private fun applyScalarFiltersToColor(
    color: Color,
    saturation: Float,
    contrast: Float,
    brightness: Float,
): Color {
    val luma = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
    val s = saturation.coerceIn(0f, 2f)
    val c = contrast.coerceIn(0.4f, 2.0f)
    val b = brightness.coerceIn(-0.5f, 0.5f)
    val rSat = (luma + (color.red - luma) * s)
    val gSat = (luma + (color.green - luma) * s)
    val bSat = (luma + (color.blue - luma) * s)
    return Color(
        red = ((rSat - 0.5f) * c + 0.5f + b).coerceIn(0f, 1f),
        green = ((gSat - 0.5f) * c + 0.5f + b).coerceIn(0f, 1f),
        blue = ((bSat - 0.5f) * c + 0.5f + b).coerceIn(0f, 1f),
        alpha = color.alpha,
    )
}

@Composable
private fun PatternWallpaperCanvas(
    sourceType: WallpaperSourceType,
    primaryColor: Color,
    accentColor: Color,
    lightness: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val a = lightness.coerceIn(0.05f, 1.0f)

        when (sourceType) {
            WallpaperSourceType.PATTERN_CARBON -> {
                val cell = 14.dp.toPx()
                val cols = (w / cell).toInt() + 2
                val rows = (h / cell).toInt() + 2
                for (r in 0..rows) {
                    for (c in 0..cols) {
                        val isEven = (r + c) % 2 == 0
                        drawRect(
                            color = primaryColor.copy(alpha = if (isEven) a * 0.24f else a * 0.08f),
                            topLeft = Offset(c * cell, r * cell),
                            size = Size(cell * 0.84f, cell * 0.42f),
                        )
                    }
                }
            }

            WallpaperSourceType.PATTERN_DOTS -> {
                val spacing = 16.dp.toPx()
                val dotRadius = 1.6.dp.toPx()
                var y = spacing * 0.5f
                while (y < h) {
                    var x = spacing * 0.5f
                    while (x < w) {
                        drawCircle(
                            color = primaryColor.copy(alpha = a * 0.55f),
                            radius = dotRadius,
                            center = Offset(x, y),
                        )
                        x += spacing
                    }
                    y += spacing
                }
            }

            WallpaperSourceType.PATTERN_GRID -> {
                val step = 22.dp.toPx()
                val stroke = 0.85.dp.toPx()
                var x = step * 0.5f
                while (x < w) {
                    drawLine(
                        color = primaryColor.copy(alpha = a * 0.34f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = stroke,
                    )
                    x += step
                }
                var y = step * 0.5f
                while (y < h) {
                    drawLine(
                        color = primaryColor.copy(alpha = a * 0.34f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = stroke,
                    )
                    y += step
                }
            }

            WallpaperSourceType.PATTERN_HEX -> {
                val r = 16.dp.toPx()
                val dx = r * 1.5f
                val dy = r * 1.732f
                val stroke = Stroke(width = 1.dp.toPx())
                val cols = (w / dx).toInt() + 2
                val rows = (h / dy).toInt() + 2
                for (col in -1..cols) {
                    for (row in -1..rows) {
                        val cx = col * dx
                        val cy = row * dy + if (col % 2 == 0) 0f else dy * 0.5f
                        val path = Path()
                        for (i in 0..5) {
                            val angle = (60.0 * i) * PI / 180.0
                            val px = cx + r * cos(angle).toFloat()
                            val py = cy + r * sin(angle).toFloat()
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        path.close()
                        drawPath(
                            path = path,
                            color = primaryColor.copy(alpha = a * 0.38f),
                            style = stroke,
                        )
                    }
                }
            }

            WallpaperSourceType.PATTERN_DIAGONAL -> {
                val step = 12.dp.toPx()
                val stroke = 1.dp.toPx()
                var x = -h
                while (x < w + h) {
                    drawLine(
                        color = primaryColor.copy(alpha = a * 0.32f),
                        start = Offset(x, h),
                        end = Offset(x + h, 0f),
                        strokeWidth = stroke,
                    )
                    x += step
                }
            }

            WallpaperSourceType.PATTERN_AURORA -> {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = a * 0.65f),
                            accentColor.copy(alpha = a * 0.25f),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.28f, h * 0.35f),
                        radius = max(w, h) * 0.72f,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = a * 0.55f),
                            primaryColor.copy(alpha = a * 0.18f),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.78f, h * 0.70f),
                        radius = max(w, h) * 0.65f,
                    ),
                )
            }

            WallpaperSourceType.PATTERN_WAVES -> {
                val lines = 7
                val amp = h * 0.08f
                val stroke = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round)
                for (i in 0 until lines) {
                    val baseY = h * (0.14f + i * 0.12f)
                    val phase = i * 0.65f
                    val path = Path()
                    val segments = 24
                    for (sIdx in 0..segments) {
                        val t = sIdx / segments.toFloat()
                        val x = w * t
                        val y = baseY + sin(t * 2f * PI.toFloat() * 1.4f + phase) * amp
                        if (sIdx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = primaryColor.copy(alpha = a * 0.42f),
                        style = stroke,
                    )
                }
            }

            WallpaperSourceType.NONE, WallpaperSourceType.CUSTOM_IMAGE -> Unit
        }
    }
}
