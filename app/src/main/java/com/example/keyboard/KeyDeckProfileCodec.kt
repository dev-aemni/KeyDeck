package com.example.keyboard

import android.view.KeyEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ParsedKeyDeckProfile(
    val profileName: String,
    val formatVersion: Int,
    val dateCreated: String,
    val settings: KeyDeckSettings,
    val hotkeys: List<CustomHotkey>?,
    val activeBuiltInWallpaperId: String?,
    val wallpaperConfig: KeyboardWallpaper?,
    val embeddedImageBytes: ByteArray?,
    val isAnimatedGif: Boolean,
)

object KeyDeckProfileCodec {
    const val FORMAT_HEADER = "@keydeck.aemni"
    const val PROFILE_EXTENSION = ".kdb"
    const val CURRENT_FORMAT_VERSION = 115

    fun sanitizeProfileBaseName(rawName: String): String {
        val trimmed = rawName.trim()
        val withoutExt = when {
            trimmed.endsWith(".kdb", ignoreCase = true) -> trimmed.dropLast(4)
            trimmed.endsWith(".keydeck", ignoreCase = true) -> trimmed.dropLast(8)
            else -> trimmed
        }
        val cleaned = withoutExt
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
            .trim('_')
        return cleaned.ifBlank { "KeyDeck_Profile" }
    }

    fun buildFileName(rawName: String): String {
        return "${sanitizeProfileBaseName(rawName)}$PROFILE_EXTENSION"
    }

    fun serialize(
        profileName: String,
        settings: KeyDeckSettings,
        hotkeys: List<CustomHotkey>,
        activeWallpaper: KeyboardWallpaper?,
        embeddedImageBytes: ByteArray? = null,
        createdAtMs: Long = System.currentTimeMillis(),
        base64Encoder: (ByteArray) -> String = ::encodeBase64Compat,
    ): String {
        val cleanName = profileName.trim().ifBlank { "My KeyDeck Layout" }
        val dateStr = SimpleDateFormat("HHmmddMMyyyy", Locale.US).format(Date(createdAtMs))
        val transparencyPct = ((1f - settings.overlayOpacity.coerceIn(0.25f, 1.0f)) * 100f)
            .toInt()
            .coerceIn(0, 75)
        val isGif = activeWallpaper?.isGifWallpaper() == true

        val themeValue = when {
            activeWallpaper != null &&
                activeWallpaper.sourceType == WallpaperSourceType.CUSTOM_IMAGE &&
                embeddedImageBytes != null &&
                embeddedImageBytes.isNotEmpty() -> {
                base64Encoder(embeddedImageBytes)
            }
            activeWallpaper != null && activeWallpaper.sourceType != WallpaperSourceType.NONE -> {
                activeWallpaper.id
            }
            else -> settings.themeId.name
        }

        val sb = StringBuilder()
        sb.appendLine(FORMAT_HEADER)
        sb.appendLine(" @version:$CURRENT_FORMAT_VERSION")
        sb.appendLine("@kdb.name:$cleanName")
        sb.appendLine("@datecreated:$dateStr")
        sb.appendLine("@kdb.hight:${settings.keyHeightDp}")
        sb.appendLine("@kdb.width:${String.format(Locale.US, "%.2f", settings.keyboardWidthFraction)}")
        sb.appendLine("@kdb.theme:$themeValue")
        sb.appendLine("@animated.gif:$isGif")

        hotkeys.forEach { hk ->
            val safeLabel = hk.label.replace(")", "]").replace(":", " ").trim().ifBlank { "Shortcut" }
            sb.appendLine("@kdb.shortcut($safeLabel): ${hk.comboText().lowercase(Locale.US)}")
            if (hk.actionType != HotkeyActionType.KEY_COMBO || hk.accentHex != 0xFF4F46E5L) {
                val safePkg = hk.targetPackageName.replace("|", "")
                val safeApp = hk.targetAppLabel.replace("|", "")
                sb.appendLine(
                    "@kdb.shortcut_meta($safeLabel): ${hk.actionType.name}|$safePkg|$safeApp|0x${
                        hk.accentHex.toString(16).uppercase(Locale.US)
                    }|${hk.category.name}",
                )
            }
        }

        sb.appendLine("@kdb.tranceparency:${transparencyPct}%")
        sb.appendLine("@kdb.appearane:${settings.appThemeMode.name.lowercase(Locale.US)}")
        sb.appendLine("@kdb.contrast:${String.format(Locale.US, "%.2f", activeWallpaper?.contrast ?: 1.08f)}")

        // Extended settings & wallpaper filter fidelity
        sb.appendLine("@kdb.layout:${settings.layoutMode.name}")
        sb.appendLine("@kdb.theme_id:${settings.themeId.name}")
        sb.appendLine("@kdb.dock:${settings.dockAlignment.name}")
        sb.appendLine("@kdb.hgap:${settings.keyHorizontalGapDp}")
        sb.appendLine("@kdb.vgap:${settings.keyVerticalGapDp}")
        sb.appendLine("@kdb.padding:${settings.deckPaddingDp}")
        sb.appendLine("@kdb.corner:${settings.keyCornerRadiusDp}")
        sb.appendLine("@kdb.fontscale:${String.format(Locale.US, "%.2f", settings.fontScale)}")
        sb.appendLine("@kdb.bubble:${settings.bubbleSizeDp}")
        sb.appendLine("@kdb.modestrip:${settings.showModeStrip}")
        sb.appendLine("@kdb.fnrow:${settings.showFunctionRow}")
        sb.appendLine("@kdb.numlock:${settings.numLockOn}")
        sb.appendLine("@kdb.sticky:${settings.stickyModifiers}")
        sb.appendLine("@kdb.haptic:${settings.hapticFeedback}")
        sb.appendLine("@kdb.suppressime:${settings.suppressAutoImeOnInput}")
        sb.appendLine("@kdb.suggestions:${settings.simpleSuggestionsEnabled}")
        sb.appendLine("@kdb.autocorrect:${settings.simpleAutoCorrectEnabled}")
        sb.appendLine("@kdb.hold_repeat:${settings.holdToRepeat}")
        sb.appendLine("@kdb.sync_dark:${settings.syncKeyboardWithDarkMode}")
        sb.appendLine("@kdb.borders:${settings.showKeyBorder}")
        sb.appendLine("@kdb.theme_borders:${settings.themeAdaptiveBorder}")
        sb.appendLine("@kdb.rgb_mode:${settings.rgbBorderMode.name}")
        sb.appendLine("@kdb.touchpad_sens:${String.format(Locale.US, "%.2f", settings.touchpadSensitivity)}")
        sb.appendLine("@kdb.touchpad_width:${String.format(Locale.US, "%.2f", settings.touchpadWidthFraction)}")
        sb.appendLine("@kdb.mcpe_look:${settings.gamingMcpeLookMode}")

        if (settings.overrideAccentHex != 0L) {
            sb.appendLine("@kdb.override_accent:0x${settings.overrideAccentHex.toString(16).uppercase(Locale.US)}")
        }
        if (settings.themeId == KeyboardThemeId.CUSTOM_STUDIO) {
            sb.appendLine("@kdb.custom_deck:0x${settings.customDeckHex.toString(16).uppercase(Locale.US)}")
            sb.appendLine("@kdb.custom_key:0x${settings.customKeyHex.toString(16).uppercase(Locale.US)}")
            sb.appendLine("@kdb.custom_mod:0x${settings.customModifierHex.toString(16).uppercase(Locale.US)}")
            sb.appendLine("@kdb.custom_text:0x${settings.customTextHex.toString(16).uppercase(Locale.US)}")
            sb.appendLine("@kdb.custom_accent:0x${settings.customAccentHex.toString(16).uppercase(Locale.US)}")
        }

        if (activeWallpaper != null && activeWallpaper.sourceType != WallpaperSourceType.NONE) {
            sb.appendLine("@kdb.wallpaper_id:${activeWallpaper.id}")
            sb.appendLine("@kdb.wallpaper_name:${activeWallpaper.name}")
            sb.appendLine("@kdb.wallpaper_source:${activeWallpaper.sourceType.name}")
            sb.appendLine("@kdb.lightness:${String.format(Locale.US, "%.2f", activeWallpaper.lightness)}")
            sb.appendLine("@kdb.saturation:${String.format(Locale.US, "%.2f", activeWallpaper.saturation)}")
            sb.appendLine("@kdb.brightness:${String.format(Locale.US, "%.2f", activeWallpaper.brightness)}")
            sb.appendLine("@kdb.tint:${if (activeWallpaper.tintHex == 0L) "0" else "0x${activeWallpaper.tintHex.toString(16).uppercase(Locale.US)}"}")
            sb.appendLine("@kdb.tint_intensity:${String.format(Locale.US, "%.2f", activeWallpaper.tintIntensity)}")
            sb.appendLine("@kdb.key_opacity:${String.format(Locale.US, "%.2f", activeWallpaper.keyOpacity)}")
            sb.appendLine("@kdb.dark_backdrop:${activeWallpaper.darkBackdrop}")
            sb.appendLine("@kdb.fit_center:${activeWallpaper.fitCenter}")
            sb.appendLine("@kdb.crop_zoom:${String.format(Locale.US, "%.2f", activeWallpaper.cropZoom)}")
            sb.appendLine("@kdb.crop_pan_x:${String.format(Locale.US, "%.3f", activeWallpaper.cropPanX)}")
            sb.appendLine("@kdb.crop_pan_y:${String.format(Locale.US, "%.3f", activeWallpaper.cropPanY)}")
        }

        return sb.toString()
    }

    fun parse(
        content: String,
        base64Decoder: (String) -> ByteArray? = ::decodeBase64Compat,
    ): ParsedKeyDeckProfile? {
        val lines = content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        if (lines.none { it.equals(FORMAT_HEADER, ignoreCase = true) }) {
            return null
        }

        val props = mutableMapOf<String, String>()
        val shortcutEntries = mutableListOf<Pair<String, String>>()
        val shortcutMeta = mutableMapOf<String, String>()

        lines.forEach { line ->
            if (line.equals(FORMAT_HEADER, ignoreCase = true) || !line.startsWith("@")) return@forEach
            val colonIdx = line.indexOf(':')
            if (colonIdx <= 1) return@forEach
            val rawKey = line.substring(0, colonIdx).trim()
            val value = line.substring(colonIdx + 1).trim()
            val lowerKey = rawKey.lowercase(Locale.US)
            when {
                lowerKey.startsWith("@kdb.shortcut(") && rawKey.endsWith(")") -> {
                    val label = rawKey.substring(rawKey.indexOf('(') + 1, rawKey.length - 1).trim()
                    shortcutEntries.add(label to value)
                }
                lowerKey.startsWith("@kdb.shortcut_meta(") && rawKey.endsWith(")") -> {
                    val label = rawKey.substring(rawKey.indexOf('(') + 1, rawKey.length - 1).trim()
                    shortcutMeta[label] = value
                }
                else -> props[lowerKey] = value
            }
        }

        val profileName = props["@kdb.name"].orEmpty().ifBlank { "Imported KeyDeck" }
        val version = props["@version"]?.toIntOrNull() ?: CURRENT_FORMAT_VERSION
        val dateCreated = props["@datecreated"].orEmpty()
        val isAnimatedGif = props["@animated.gif"]?.toBooleanStrictOrNull() ?: false

        var resolvedSettings = KeyDeckSettings()

        // Height (supports @kdb.hight or @kdb.height)
        val heightStr = props["@kdb.hight"] ?: props["@kdb.height"]
        heightStr?.toIntOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(keyHeightDp = it.coerceIn(26, 54))
        }

        // Width (supports fraction 0.60..1.00 or percentage 60..100 / 60%)
        props["@kdb.width"]?.let { wRaw ->
            val cleaned = wRaw.removeSuffix("%").trim().toFloatOrNull()
            if (cleaned != null) {
                val frac = if (cleaned > 1.5f) cleaned / 100f else cleaned
                resolvedSettings = resolvedSettings.copy(keyboardWidthFraction = frac.coerceIn(0.60f, 1.0f))
            }
        }

        // Transparency (supports @kdb.tranceparency or @kdb.transparency)
        val transpRaw = props["@kdb.tranceparency"] ?: props["@kdb.transparency"]
        transpRaw?.let { raw ->
            val hasPct = raw.contains("%")
            val num = raw.removeSuffix("%").trim().toFloatOrNull()
            if (num != null) {
                val opacity = if (hasPct || num > 1.0f) {
                    1f - (num.coerceIn(0f, 75f) / 100f)
                } else {
                    num.coerceIn(0.25f, 1.0f)
                }
                resolvedSettings = resolvedSettings.copy(overlayOpacity = opacity.coerceIn(0.25f, 1.0f))
            }
        }

        // Appearance (supports @kdb.appearane or @kdb.appearance)
        val appearRaw = props["@kdb.appearane"] ?: props["@kdb.appearance"]
        appearRaw?.let { raw ->
            val mode = when (raw.lowercase(Locale.US)) {
                "dark" -> AppThemeMode.DARK
                "light" -> AppThemeMode.LIGHT
                else -> AppThemeMode.SYSTEM
            }
            resolvedSettings = resolvedSettings.copy(appThemeMode = mode)
        }

        props["@kdb.layout"]?.let { raw ->
            runCatching { KeyboardLayoutMode.valueOf(raw.uppercase(Locale.US)) }.getOrNull()?.let {
                resolvedSettings = resolvedSettings.copy(layoutMode = it)
            }
        }
        props["@kdb.theme_id"]?.let { raw ->
            resolveThemeByName(raw)?.let { resolvedSettings = resolvedSettings.copy(themeId = it) }
        }
        props["@kdb.dock"]?.let { raw ->
            runCatching { KeyboardDockAlignment.valueOf(raw.uppercase(Locale.US)) }.getOrNull()?.let {
                resolvedSettings = resolvedSettings.copy(dockAlignment = it)
            }
        }
        props["@kdb.hgap"]?.toIntOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(keyHorizontalGapDp = it.coerceIn(1, 6))
        }
        props["@kdb.vgap"]?.toIntOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(keyVerticalGapDp = it.coerceIn(1, 7))
        }
        props["@kdb.padding"]?.toIntOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(deckPaddingDp = it.coerceIn(1, 10))
        }
        props["@kdb.corner"]?.toIntOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(keyCornerRadiusDp = it.coerceIn(4, 16))
        }
        props["@kdb.fontscale"]?.toFloatOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(fontScale = it.coerceIn(0.75f, 1.25f))
        }
        props["@kdb.bubble"]?.toIntOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(bubbleSizeDp = it.coerceIn(36, 68))
        }
        props["@kdb.modestrip"]?.toBooleanStrictOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(showModeStrip = it)
        }
        props["@kdb.fnrow"]?.toBooleanStrictOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(showFunctionRow = it)
        }
        props["@kdb.numlock"]?.toBooleanStrictOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(numLockOn = it)
        }
        props["@kdb.sticky"]?.toBooleanStrictOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(stickyModifiers = it)
        }
        props["@kdb.haptic"]?.toBooleanStrictOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(hapticFeedback = it)
        }
        props["@kdb.suppressime"]?.toBooleanStrictOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(suppressAutoImeOnInput = it)
        }
        props["@kdb.suggestions"]?.toBooleanStrictOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(simpleSuggestionsEnabled = it)
        }
        props["@kdb.autocorrect"]?.toBooleanStrictOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(simpleAutoCorrectEnabled = it)
        }
        props["@kdb.hold_repeat"]?.toBooleanStrictOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(holdToRepeat = it)
        }
        props["@kdb.sync_dark"]?.toBooleanStrictOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(syncKeyboardWithDarkMode = it)
        }
        props["@kdb.borders"]?.toBooleanStrictOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(showKeyBorder = it)
        }
        props["@kdb.theme_borders"]?.toBooleanStrictOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(themeAdaptiveBorder = it)
        }
        props["@kdb.rgb_mode"]?.let { raw ->
            runCatching { RgbBorderMode.valueOf(raw.uppercase(Locale.US)) }.getOrNull()?.let {
                resolvedSettings = resolvedSettings.copy(rgbBorderMode = it)
            }
        }
        props["@kdb.touchpad_sens"]?.toFloatOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(touchpadSensitivity = it.coerceIn(0.6f, 3.5f))
        }
        props["@kdb.touchpad_width"]?.toFloatOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(touchpadWidthFraction = it.coerceIn(0.30f, 0.55f))
        }
        props["@kdb.mcpe_look"]?.toBooleanStrictOrNull()?.let {
            resolvedSettings = resolvedSettings.copy(gamingMcpeLookMode = it)
        }
        parseHexLong(props["@kdb.override_accent"])?.let {
            resolvedSettings = resolvedSettings.copy(overrideAccentHex = it)
        }
        parseHexLong(props["@kdb.custom_deck"])?.let {
            resolvedSettings = resolvedSettings.copy(customDeckHex = it)
        }
        parseHexLong(props["@kdb.custom_key"])?.let {
            resolvedSettings = resolvedSettings.copy(customKeyHex = it)
        }
        parseHexLong(props["@kdb.custom_mod"])?.let {
            resolvedSettings = resolvedSettings.copy(customModifierHex = it)
        }
        parseHexLong(props["@kdb.custom_text"])?.let {
            resolvedSettings = resolvedSettings.copy(customTextHex = it)
        }
        parseHexLong(props["@kdb.custom_accent"])?.let {
            resolvedSettings = resolvedSettings.copy(customAccentHex = it)
        }

        // Theme / Wallpaper resolution (@kdb.theme: built-in name OR base64 image/gif)
        val rawThemeField = props["@kdb.theme"].orEmpty()
        var embeddedBytes: ByteArray? = null
        var builtInWallpaperId: String? = null

        val namedTheme = resolveThemeByName(rawThemeField)
        val builtInWp = BuiltInWallpapers.presets.firstOrNull {
            it.id.equals(rawThemeField, ignoreCase = true) ||
                it.name.equals(rawThemeField, ignoreCase = true)
        }
        when {
            namedTheme != null -> {
                if (!props.containsKey("@kdb.theme_id")) {
                    resolvedSettings = resolvedSettings.copy(themeId = namedTheme)
                }
            }
            builtInWp != null -> {
                builtInWallpaperId = builtInWp.id
            }
            rawThemeField.isNotBlank() -> {
                val b64Payload = if (rawThemeField.startsWith("data:", ignoreCase = true)) {
                    rawThemeField.substringAfter("base64,", "")
                } else {
                    rawThemeField
                }
                if (b64Payload.length >= 16) {
                    val decoded = base64Decoder(b64Payload)
                    if (decoded != null && decoded.isNotEmpty()) {
                        embeddedBytes = decoded
                    }
                }
            }
        }

        val wpSource = props["@kdb.wallpaper_source"]?.let { raw ->
            runCatching { WallpaperSourceType.valueOf(raw.uppercase(Locale.US)) }.getOrNull()
        }
        val hasFilterOverrides = listOf(
            "@kdb.contrast",
            "@kdb.lightness",
            "@kdb.saturation",
            "@kdb.brightness",
            "@kdb.tint",
            "@kdb.wallpaper_id",
        ).any { props.containsKey(it) }

        val wpConfig: KeyboardWallpaper? = when {
            embeddedBytes != null -> {
                KeyboardWallpaper(
                    id = "wp_imp_${UUID.randomUUID().toString().take(8)}",
                    name = props["@kdb.wallpaper_name"]?.ifBlank { null } ?: profileName,
                    sourceType = WallpaperSourceType.CUSTOM_IMAGE,
                    imagePath = "",
                    lightness = props["@kdb.lightness"]?.toFloatOrNull()?.coerceIn(0.03f, 1.0f) ?: 0.20f,
                    saturation = props["@kdb.saturation"]?.toFloatOrNull()?.coerceIn(0.0f, 2.0f) ?: 0.92f,
                    contrast = props["@kdb.contrast"]?.toFloatOrNull()?.coerceIn(0.4f, 2.0f) ?: 1.08f,
                    brightness = props["@kdb.brightness"]?.toFloatOrNull()?.coerceIn(-0.5f, 0.5f) ?: 0.0f,
                    tintHex = parseHexLong(props["@kdb.tint"]) ?: 0L,
                    tintIntensity = props["@kdb.tint_intensity"]?.toFloatOrNull()?.coerceIn(0f, 0.85f) ?: 0.22f,
                    keyOpacity = props["@kdb.key_opacity"]?.toFloatOrNull()?.coerceIn(0.15f, 1.0f) ?: 0.64f,
                    darkBackdrop = props["@kdb.dark_backdrop"]?.toBooleanStrictOrNull() ?: true,
                    fitCenter = props["@kdb.fit_center"]?.toBooleanStrictOrNull() ?: false,
                    cropZoom = props["@kdb.crop_zoom"]?.toFloatOrNull()?.coerceIn(0.4f, 4.0f) ?: 1.0f,
                    cropPanX = props["@kdb.crop_pan_x"]?.toFloatOrNull()?.coerceIn(-1.5f, 1.5f) ?: 0.0f,
                    cropPanY = props["@kdb.crop_pan_y"]?.toFloatOrNull()?.coerceIn(-1.5f, 1.5f) ?: 0.0f,
                    isAnimatedGif = isAnimatedGif,
                    isBuiltIn = false,
                    createdAtMs = System.currentTimeMillis(),
                )
            }
            builtInWp != null && hasFilterOverrides -> {
                builtInWp.copy(
                    id = "wp_imp_${UUID.randomUUID().toString().take(8)}",
                    name = props["@kdb.wallpaper_name"]?.ifBlank { null } ?: builtInWp.name,
                    lightness = props["@kdb.lightness"]?.toFloatOrNull()?.coerceIn(0.03f, 1.0f) ?: builtInWp.lightness,
                    saturation = props["@kdb.saturation"]?.toFloatOrNull()?.coerceIn(0.0f, 2.0f) ?: builtInWp.saturation,
                    contrast = props["@kdb.contrast"]?.toFloatOrNull()?.coerceIn(0.4f, 2.0f) ?: builtInWp.contrast,
                    brightness = props["@kdb.brightness"]?.toFloatOrNull()?.coerceIn(-0.5f, 0.5f) ?: builtInWp.brightness,
                    tintHex = parseHexLong(props["@kdb.tint"]) ?: builtInWp.tintHex,
                    tintIntensity = props["@kdb.tint_intensity"]?.toFloatOrNull()?.coerceIn(0f, 0.85f) ?: builtInWp.tintIntensity,
                    keyOpacity = props["@kdb.key_opacity"]?.toFloatOrNull()?.coerceIn(0.15f, 1.0f) ?: builtInWp.keyOpacity,
                    darkBackdrop = props["@kdb.dark_backdrop"]?.toBooleanStrictOrNull() ?: builtInWp.darkBackdrop,
                    isBuiltIn = false,
                    createdAtMs = System.currentTimeMillis(),
                )
            }
            wpSource != null && wpSource != WallpaperSourceType.NONE && wpSource != WallpaperSourceType.CUSTOM_IMAGE -> {
                KeyboardWallpaper(
                    id = "wp_imp_${UUID.randomUUID().toString().take(8)}",
                    name = props["@kdb.wallpaper_name"]?.ifBlank { null } ?: profileName,
                    sourceType = wpSource,
                    lightness = props["@kdb.lightness"]?.toFloatOrNull()?.coerceIn(0.03f, 1.0f) ?: 0.20f,
                    saturation = props["@kdb.saturation"]?.toFloatOrNull()?.coerceIn(0.0f, 2.0f) ?: 0.90f,
                    contrast = props["@kdb.contrast"]?.toFloatOrNull()?.coerceIn(0.4f, 2.0f) ?: 1.08f,
                    brightness = props["@kdb.brightness"]?.toFloatOrNull()?.coerceIn(-0.5f, 0.5f) ?: 0.0f,
                    tintHex = parseHexLong(props["@kdb.tint"]) ?: 0L,
                    tintIntensity = props["@kdb.tint_intensity"]?.toFloatOrNull()?.coerceIn(0f, 0.85f) ?: 0.24f,
                    keyOpacity = props["@kdb.key_opacity"]?.toFloatOrNull()?.coerceIn(0.15f, 1.0f) ?: 0.65f,
                    darkBackdrop = props["@kdb.dark_backdrop"]?.toBooleanStrictOrNull() ?: true,
                    isBuiltIn = false,
                    createdAtMs = System.currentTimeMillis(),
                )
            }
            else -> null
        }

        val parsedHotkeys = if (shortcutEntries.isNotEmpty()) {
            shortcutEntries.mapIndexedNotNull { idx, (label, comboStr) ->
                parseHotkeyEntry(
                    id = "hk_imp_${idx}_${UUID.randomUUID().toString().take(4)}",
                    label = label,
                    combo = comboStr,
                    metaSpec = shortcutMeta[label],
                )
            }.takeIf { it.isNotEmpty() }
        } else {
            null
        }

        return ParsedKeyDeckProfile(
            profileName = profileName,
            formatVersion = version,
            dateCreated = dateCreated,
            settings = resolvedSettings,
            hotkeys = parsedHotkeys,
            activeBuiltInWallpaperId = builtInWallpaperId,
            wallpaperConfig = wpConfig,
            embeddedImageBytes = embeddedBytes,
            isAnimatedGif = isAnimatedGif,
        )
    }

    private fun resolveThemeByName(raw: String): KeyboardThemeId? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        return KeyboardThemeId.entries.firstOrNull {
            it.name.equals(trimmed, ignoreCase = true) ||
                it.displayName.equals(trimmed, ignoreCase = true)
        }
    }

    private fun parseHexLong(raw: String?): Long? {
        val trimmed = raw?.trim()?.lowercase(Locale.US) ?: return null
        if (trimmed.isBlank()) return null
        if (trimmed == "0") return 0L
        return if (trimmed.startsWith("0x")) {
            trimmed.removePrefix("0x").toLongOrNull(16)
        } else {
            trimmed.toLongOrNull()
        }
    }

    private fun parseHotkeyEntry(
        id: String,
        label: String,
        combo: String,
        metaSpec: String?,
    ): CustomHotkey? {
        val tokens = combo.split('+').map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        var ctrl = false
        var alt = false
        var shift = false
        var meta = false
        var keyToken: String? = null

        tokens.forEach { token ->
            when (token.lowercase(Locale.US)) {
                "ctrl", "control" -> ctrl = true
                "alt" -> alt = true
                "shift" -> shift = true
                "win", "meta", "cmd", "super" -> meta = true
                else -> keyToken = token
            }
        }

        val resolvedKey = keyToken ?: return null
        val (keyCode, keyDisplay) = resolveKeyToken(resolvedKey) ?: return null

        val metaParts = metaSpec?.split('|')
        val actionType = metaParts?.getOrNull(0)?.let { raw ->
            runCatching { HotkeyActionType.valueOf(raw) }.getOrNull()
        } ?: if (label.contains("Float", ignoreCase = true) && ctrl && alt && keyCode == KeyEvent.KEYCODE_F) {
            HotkeyActionType.TOGGLE_FLOATING
        } else {
            HotkeyActionType.KEY_COMBO
        }
        val pkg = metaParts?.getOrNull(1).orEmpty()
        val appLabel = metaParts?.getOrNull(2).orEmpty()
        val accentHex = parseHexLong(metaParts?.getOrNull(3)) ?: 0xFF4F46E5L
        val category = metaParts?.getOrNull(4)?.let { raw ->
            runCatching { HotkeyCategory.valueOf(raw) }.getOrNull()
        } ?: HotkeyCategory.CUSTOM

        return CustomHotkey(
            id = id,
            label = label.ifBlank { keyDisplay },
            keyCode = keyCode,
            keyDisplay = keyDisplay,
            ctrl = ctrl,
            alt = alt,
            shift = shift,
            meta = meta,
            category = category,
            accentHex = accentHex,
            actionType = actionType,
            targetPackageName = pkg,
            targetAppLabel = appLabel,
        )
    }

    private fun resolveKeyToken(token: String): Pair<Int, String>? {
        val upper = token.trim().uppercase(Locale.US)
        if (upper.length == 1) {
            val ch = upper[0]
            if (ch in 'A'..'Z') {
                return (KeyEvent.KEYCODE_A + (ch - 'A')) to ch.toString()
            }
            if (ch in '0'..'9') {
                return (KeyEvent.KEYCODE_0 + (ch - '0')) to ch.toString()
            }
        }
        return when (upper) {
            "F1" -> KeyEvent.KEYCODE_F1 to "F1"
            "F2" -> KeyEvent.KEYCODE_F2 to "F2"
            "F3" -> KeyEvent.KEYCODE_F3 to "F3"
            "F4" -> KeyEvent.KEYCODE_F4 to "F4"
            "F5" -> KeyEvent.KEYCODE_F5 to "F5"
            "F6" -> KeyEvent.KEYCODE_F6 to "F6"
            "F7" -> KeyEvent.KEYCODE_F7 to "F7"
            "F8" -> KeyEvent.KEYCODE_F8 to "F8"
            "F9" -> KeyEvent.KEYCODE_F9 to "F9"
            "F10" -> KeyEvent.KEYCODE_F10 to "F10"
            "F11" -> KeyEvent.KEYCODE_F11 to "F11"
            "F12" -> KeyEvent.KEYCODE_F12 to "F12"
            "SPACE", " " -> KeyEvent.KEYCODE_SPACE to "Space"
            "TAB" -> KeyEvent.KEYCODE_TAB to "Tab"
            "ENTER", "RETURN" -> KeyEvent.KEYCODE_ENTER to "Enter"
            "ESC", "ESCAPE" -> KeyEvent.KEYCODE_ESCAPE to "Esc"
            "DEL", "DELETE" -> KeyEvent.KEYCODE_FORWARD_DEL to "Del"
            "BACKSPACE", "BS" -> KeyEvent.KEYCODE_DEL to "Backspace"
            "LEFT" -> KeyEvent.KEYCODE_DPAD_LEFT to "Left"
            "RIGHT" -> KeyEvent.KEYCODE_DPAD_RIGHT to "Right"
            "UP" -> KeyEvent.KEYCODE_DPAD_UP to "Up"
            "DOWN" -> KeyEvent.KEYCODE_DPAD_DOWN to "Down"
            "HOME" -> KeyEvent.KEYCODE_MOVE_HOME to "Home"
            "END" -> KeyEvent.KEYCODE_MOVE_END to "End"
            "PGUP", "PAGEUP" -> KeyEvent.KEYCODE_PAGE_UP to "PgUp"
            "PGDN", "PAGEDOWN" -> KeyEvent.KEYCODE_PAGE_DOWN to "PgDn"
            "/" -> KeyEvent.KEYCODE_SLASH to "/"
            "." -> KeyEvent.KEYCODE_PERIOD to "."
            "," -> KeyEvent.KEYCODE_COMMA to ","
            "-" -> KeyEvent.KEYCODE_MINUS to "-"
            "=" -> KeyEvent.KEYCODE_EQUALS to "="
            else -> null
        }
    }

    private fun encodeBase64Compat(bytes: ByteArray): String {
        return runCatching {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }.getOrElse {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    private fun decodeBase64Compat(raw: String): ByteArray? {
        return runCatching {
            android.util.Base64.decode(raw, android.util.Base64.DEFAULT)
        }.getOrElse {
            runCatching { java.util.Base64.getDecoder().decode(raw) }.getOrNull()
        }
    }
}
