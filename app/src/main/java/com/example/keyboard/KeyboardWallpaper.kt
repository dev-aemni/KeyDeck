package com.example.keyboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
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

enum class WallpaperScaleMode(val label: String) {
    STRETCH("Stretch Full"),
    CROP("Fill Crop"),
    FIT("Fit Center"),
}

data class KeyboardWallpaper(
    val id: String,
    val name: String,
    val sourceType: WallpaperSourceType,
    val imagePath: String = "",
    val lightness: Float = 0.16f,      // 0.04f..1.0f (~16% visible for stealth presets; 1.0f clear for photos/GIFs)
    val saturation: Float = 0.90f,     // 0.0f..2.0f
    val contrast: Float = 1.08f,       // 0.50f..1.80f
    val brightness: Float = 0.0f,      // -0.40f..0.40f
    val tintHex: Long = 0L,            // 0L = neutral / theme default
    val tintIntensity: Float = 0.24f,  // 0.0f..0.85f
    val keyOpacity: Float = 0.64f,     // 0.15f..1.0f
    val darkBackdrop: Boolean = true,  // true = obsidian dark keyboard look
    val fitCenter: Boolean = false,    // legacy compat flag; resolvedScaleMode() honors scaleMode
    val scaleMode: WallpaperScaleMode = WallpaperScaleMode.STRETCH,
    val cropZoom: Float = 1.0f,        // 0.40f..4.0f zoom multiplier from interactive cropper/editor
    val cropPanX: Float = 0.0f,        // normalized horizontal pan fraction (-1.5f..1.5f)
    val cropPanY: Float = 0.0f,        // normalized vertical pan fraction (-1.5f..1.5f)
    val isAnimatedGif: Boolean = false,
    val isBuiltIn: Boolean = false,
    val createdAtMs: Long = 0L,
) {
    fun isGifWallpaper(): Boolean = isAnimatedGif || imagePath.endsWith(".gif", ignoreCase = true)

    fun resolvedScaleMode(): WallpaperScaleMode = when {
        scaleMode != WallpaperScaleMode.STRETCH -> scaleMode
        fitCenter -> WallpaperScaleMode.FIT
        else -> WallpaperScaleMode.STRETCH
    }
}

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

data class ImportedWallpaperResult(
    val path: String,
    val isAnimatedGif: Boolean,
)

private fun isGifHeader(bytes: ByteArray): Boolean {
    if (bytes.size < 6) return false
    return bytes[0] == 'G'.code.toByte() &&
        bytes[1] == 'I'.code.toByte() &&
        bytes[2] == 'F'.code.toByte() &&
        bytes[3] == '8'.code.toByte() &&
        (bytes[4] == '7'.code.toByte() || bytes[4] == '9'.code.toByte()) &&
        bytes[5] == 'a'.code.toByte()
}

suspend fun importWallpaperFromUri(context: Context, uri: Uri): ImportedWallpaperResult? = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)?.lowercase().orEmpty()
        val rawBytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
        if (rawBytes.isEmpty()) return@runCatching null

        val isGif = mime.contains("gif") || isGifHeader(rawBytes)
        if (isGif) {
            val dir = File(context.filesDir, "wallpapers").apply { mkdirs() }
            val outFile = File(dir, "wp_${UUID.randomUUID().toString().take(10)}.gif")
            FileOutputStream(outFile).use { fos -> fos.write(rawBytes) }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)?.let { firstFrame ->
                WallpaperBitmapCache.putCached(outFile.absolutePath, firstFrame.asImageBitmap())
            }
            return@runCatching ImportedWallpaperResult(outFile.absolutePath, isAnimatedGif = true)
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
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
        val rawBmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
            ?: return@runCatching null

        val orientation = runCatching {
            rawBytes.inputStream().use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
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
        ImportedWallpaperResult(outFile.absolutePath, isAnimatedGif = false)
    }.getOrNull()
}

suspend fun importWallpaperFromBytes(
    context: Context,
    rawBytes: ByteArray,
    preferGif: Boolean = false,
): ImportedWallpaperResult? = withContext(Dispatchers.IO) {
    runCatching {
        if (rawBytes.isEmpty()) return@runCatching null
        val dir = File(context.filesDir, "wallpapers").apply { mkdirs() }
        val isGif = preferGif || isGifHeader(rawBytes)
        if (isGif) {
            val outFile = File(dir, "wp_${UUID.randomUUID().toString().take(10)}.gif")
            FileOutputStream(outFile).use { it.write(rawBytes) }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)?.let { firstFrame ->
                WallpaperBitmapCache.putCached(outFile.absolutePath, firstFrame.asImageBitmap())
            }
            return@runCatching ImportedWallpaperResult(outFile.absolutePath, isAnimatedGif = true)
        }

        val rawBmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return@runCatching null
        val usePng = rawBmp.hasAlpha()
        val ext = if (usePng) "png" else "jpg"
        val outFile = File(dir, "wp_${UUID.randomUUID().toString().take(10)}.$ext")
        FileOutputStream(outFile).use { fos ->
            if (usePng) {
                rawBmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
            } else {
                rawBmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }
        }
        WallpaperBitmapCache.putCached(outFile.absolutePath, rawBmp.asImageBitmap())
        ImportedWallpaperResult(outFile.absolutePath, isAnimatedGif = false)
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
    animateGif: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (wallpaper == null || wallpaper.sourceType == WallpaperSourceType.NONE) {
        Box(
            modifier = modifier.background(palette.deckBg.copy(alpha = overlayAlpha)),
        )
        return
    }

    val lightness = wallpaper.lightness.coerceIn(0.03f, 1.0f)
    val resolvedScale = wallpaper.resolvedScaleMode()
    val baseColor = when {
        wallpaper.sourceType == WallpaperSourceType.CUSTOM_IMAGE && !wallpaper.darkBackdrop ->
            Color.Transparent
        wallpaper.darkBackdrop ->
            lerp(palette.deckBg, Color.Black, 0.92f).copy(alpha = overlayAlpha)
        else ->
            palette.deckBg.copy(alpha = overlayAlpha)
    }

    val colorMatrix = remember(wallpaper.saturation, wallpaper.contrast, wallpaper.brightness) {
        buildWallpaperColorMatrix(
            saturation = wallpaper.saturation,
            contrast = wallpaper.contrast,
            brightness = wallpaper.brightness,
        )
    }

    Box(modifier = modifier.clipToBounds().background(baseColor)) {
        if (wallpaper.sourceType == WallpaperSourceType.CUSTOM_IMAGE && wallpaper.imagePath.isNotBlank()) {
            WallpaperMediaLayer(
                imagePath = wallpaper.imagePath,
                contentDescription = wallpaper.name,
                isAnimatedGif = wallpaper.isGifWallpaper(),
                animateGif = animateGif,
                scaleMode = resolvedScale,
                cropZoom = wallpaper.cropZoom,
                cropPanX = wallpaper.cropPanX,
                cropPanY = wallpaper.cropPanY,
                alpha = lightness * overlayAlpha,
                colorMatrix = colorMatrix,
                modifier = Modifier.fillMaxSize(),
            )
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
            // Optional dark vignette only when Stealth Dark Backdrop is enabled
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

@Suppress("DEPRECATION")
@Composable
fun WallpaperMediaLayer(
    imagePath: String,
    contentDescription: String,
    isAnimatedGif: Boolean,
    animateGif: Boolean,
    scaleMode: WallpaperScaleMode = WallpaperScaleMode.STRETCH,
    cropZoom: Float,
    cropPanX: Float,
    cropPanY: Float,
    alpha: Float,
    colorMatrix: ColorMatrix,
    modifier: Modifier = Modifier,
) {
    val zoom = cropZoom.coerceIn(0.4f, 4.0f)
    val panX = cropPanX.coerceIn(-1.5f, 1.5f)
    val panY = cropPanY.coerceIn(-1.5f, 1.5f)
    val clampedAlpha = alpha.coerceIn(0f, 1f)

    Box(modifier = modifier.clipToBounds()) {
        val transformModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = zoom
                scaleY = zoom
                translationX = panX * size.width
                translationY = panY * size.height
            }

        if (isAnimatedGif && animateGif && imagePath.isNotBlank()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                var animDrawable by remember(imagePath) { mutableStateOf<AnimatedImageDrawable?>(null) }
                var frameTick by remember(imagePath) { mutableIntStateOf(0) }

                LaunchedEffect(imagePath) {
                    val decoded = withContext(Dispatchers.IO) {
                        runCatching {
                            val file = File(imagePath)
                            if (file.exists()) {
                                ImageDecoder.decodeDrawable(ImageDecoder.createSource(file)) as? AnimatedImageDrawable
                            } else {
                                null
                            }
                        }.getOrNull()
                    }
                    animDrawable = decoded
                }

                val activeDrawable = animDrawable
                if (activeDrawable != null && activeDrawable.intrinsicWidth > 0 && activeDrawable.intrinsicHeight > 0) {
                    DisposableEffect(activeDrawable) {
                        val mainHandler = Handler(Looper.getMainLooper())
                        val cb = object : Drawable.Callback {
                            override fun invalidateDrawable(who: Drawable) {
                                frameTick++
                            }

                            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                                mainHandler.postAtTime(what, who, `when`)
                            }

                            override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                                mainHandler.removeCallbacks(what, who)
                            }
                        }
                        val iw = activeDrawable.intrinsicWidth.coerceAtLeast(1)
                        val ih = activeDrawable.intrinsicHeight.coerceAtLeast(1)
                        activeDrawable.setBounds(0, 0, iw, ih)
                        activeDrawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                        activeDrawable.callback = cb
                        activeDrawable.start()
                        onDispose {
                            runCatching {
                                activeDrawable.stop()
                                mainHandler.removeCallbacksAndMessages(activeDrawable)
                                activeDrawable.callback = null
                            }
                        }
                    }

                    val nativeFilter = remember(colorMatrix) { ColorMatrixColorFilter(colorMatrix.values) }
                    Canvas(modifier = transformModifier) {
                        @Suppress("UNUSED_VARIABLE")
                        val currentFrame = frameTick
                        val iw = activeDrawable.intrinsicWidth.coerceAtLeast(1).toFloat()
                        val ih = activeDrawable.intrinsicHeight.coerceAtLeast(1).toFloat()
                        val (scaleXBase, scaleYBase) = computeScalePair(
                            srcW = iw,
                            srcH = ih,
                            dstW = size.width,
                            dstH = size.height,
                            scaleMode = scaleMode,
                        )
                        val dx = (size.width - iw * scaleXBase) * 0.5f
                        val dy = (size.height - ih * scaleYBase) * 0.5f

                        activeDrawable.alpha = (clampedAlpha * 255f).roundToInt().coerceIn(0, 255)
                        activeDrawable.colorFilter = nativeFilter
                        drawIntoCanvas { canvas ->
                            val nc = canvas.nativeCanvas
                            val saveCount = nc.save()
                            nc.translate(dx, dy)
                            nc.scale(scaleXBase, scaleYBase)
                            activeDrawable.draw(nc)
                            nc.restoreToCount(saveCount)
                        }
                    }
                    return@Box
                }
            } else {
                var movie by remember(imagePath) { mutableStateOf<android.graphics.Movie?>(null) }
                var movieTimeMs by remember(imagePath) { mutableIntStateOf(0) }
                LaunchedEffect(imagePath) {
                    movie = withContext(Dispatchers.IO) {
                        runCatching {
                            val file = File(imagePath)
                            if (file.exists()) android.graphics.Movie.decodeFile(file.absolutePath) else null
                        }.getOrNull()
                    }
                }
                val activeMovie = movie
                if (activeMovie != null && activeMovie.width() > 0 && activeMovie.height() > 0) {
                    val duration = activeMovie.duration().coerceAtLeast(1000)
                    LaunchedEffect(activeMovie, duration) {
                        val startMs = SystemClock.uptimeMillis()
                        while (isActive) {
                            movieTimeMs = ((SystemClock.uptimeMillis() - startMs) % duration).toInt()
                            delay(33L)
                        }
                    }
                    Canvas(modifier = transformModifier) {
                        val iw = activeMovie.width().toFloat()
                        val ih = activeMovie.height().toFloat()
                        val (scaleXBase, scaleYBase) = computeScalePair(
                            srcW = iw,
                            srcH = ih,
                            dstW = size.width,
                            dstH = size.height,
                            scaleMode = scaleMode,
                        )
                        val dx = (size.width - iw * scaleXBase) * 0.5f
                        val dy = (size.height - ih * scaleYBase) * 0.5f

                        activeMovie.setTime(movieTimeMs)
                        val paint = android.graphics.Paint().apply {
                            this.alpha = (clampedAlpha * 255f).roundToInt().coerceIn(0, 255)
                            this.colorFilter = ColorMatrixColorFilter(colorMatrix.values)
                        }
                        drawIntoCanvas { canvas ->
                            val nc = canvas.nativeCanvas
                            val saveCount = nc.save()
                            nc.translate(dx, dy)
                            nc.scale(scaleXBase, scaleYBase)
                            activeMovie.draw(nc, 0f, 0f, paint)
                            nc.restoreToCount(saveCount)
                        }
                    }
                    return@Box
                }
            }
        }

        var loadedBitmap by remember(imagePath) {
            mutableStateOf(WallpaperBitmapCache.getCached(imagePath))
        }
        LaunchedEffect(imagePath) {
            if (loadedBitmap == null) {
                val decoded = withContext(Dispatchers.IO) {
                    runCatching {
                        val file = File(imagePath)
                        if (file.exists()) {
                            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                        } else {
                            null
                        }
                    }.getOrNull()
                }
                if (decoded != null) {
                    WallpaperBitmapCache.putCached(imagePath, decoded)
                    loadedBitmap = decoded
                }
            }
        }
        val bmp = loadedBitmap
        if (bmp != null) {
            val contentScale = when (scaleMode) {
                WallpaperScaleMode.STRETCH -> ContentScale.FillBounds
                WallpaperScaleMode.FIT -> ContentScale.Fit
                WallpaperScaleMode.CROP -> ContentScale.Crop
            }
            Image(
                bitmap = bmp,
                contentDescription = contentDescription,
                contentScale = contentScale,
                alpha = clampedAlpha,
                colorFilter = ColorFilter.colorMatrix(colorMatrix),
                modifier = transformModifier,
            )
        }
    }
}

private fun computeScalePair(
    srcW: Float,
    srcH: Float,
    dstW: Float,
    dstH: Float,
    scaleMode: WallpaperScaleMode,
): Pair<Float, Float> {
    if (srcW <= 0f || srcH <= 0f || dstW <= 0f || dstH <= 0f) return 1f to 1f
    val sx = dstW / srcW
    val sy = dstH / srcH
    return when (scaleMode) {
        WallpaperScaleMode.STRETCH -> sx to sy
        WallpaperScaleMode.FIT -> {
            val u = min(sx, sy)
            u to u
        }
        WallpaperScaleMode.CROP -> {
            val u = max(sx, sy)
            u to u
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
