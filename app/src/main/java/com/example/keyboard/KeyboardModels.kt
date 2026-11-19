package com.example.keyboard

import android.view.KeyEvent
import androidx.compose.ui.graphics.Color

enum class KeyboardLayoutMode(val label: String, val shortLabel: String, val subtitle: String) {
    PC_WITH_NUMPAD("PC + Numpad", "PC+Num", "10-col QWERTY + dedicated 4x5 numeric pad"),
    PC_STANDARD("PC Full", "PC", "Full 6-row PC QWERTY + F1–F12"),
    NUMPAD_ONLY("Numeric Pad", "Num", "Large 4x5 dedicated numeric keypad"),
    GAMING("Gaming + Mouse", "Game", "Left WASD/QREFGC keys + right mouse touchpad"),
}

enum class KeyboardDockAlignment(val label: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right"),
}

enum class RgbBorderMode(val label: String) {
    GAMING_ONLY("Gaming Only"),
    OFF("Off"),
}

enum class KeyboardSizePreset(
    val label: String,
    val keyHeightDp: Int,
    val widthFraction: Float,
    val hGapDp: Int,
    val vGapDp: Int,
    val deckPaddingDp: Int,
    val fontScale: Float,
    val cornerRadiusDp: Int,
    val bubbleSizeDp: Int,
) {
    MICRO("Compact", 30, 0.90f, 2, 2, 4, 0.86f, 8, 42),
    TIGHT("Tight", 34, 0.96f, 2, 3, 5, 0.92f, 10, 46),
    BALANCED("Balanced", 38, 1.00f, 3, 4, 6, 1.00f, 11, 50),
    COMFORT("Comfort", 46, 1.00f, 4, 5, 8, 1.12f, 13, 58),
}

enum class KeyboardThemeId(
    val displayName: String,
    val deckBg: Color,
    val keyBg: Color,
    val modifierBg: Color,
    val activeFill: Color,
    val accentBg: Color,
    val shadowLight: Color,
    val shadowDark: Color,
    val keyBorder: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val activeIndicator: Color,
    val isDark: Boolean = false,
) {
    SOFT_NEUMORPH(
        displayName = "Soft UI Slate",
        deckBg = Color(0xFFD5DCE5),
        keyBg = Color(0xFFDCE1EA),
        modifierBg = Color(0xFFCFD6E2),
        activeFill = Color(0xFFCDE0F8),
        accentBg = Color(0xFF4F46E5),
        shadowLight = Color(0xFFF1F4F9),
        shadowDark = Color(0xFFA5AEBC),
        keyBorder = Color.Black,
        primaryText = Color(0xFF1F2937),
        secondaryText = Color(0xFF4B5563),
        activeIndicator = Color(0xFF4F46E5),
        isDark = false,
    ),
    LAVENDER_NEU(
        displayName = "Soft Lavender",
        deckBg = Color(0xFFDCD9EC),
        keyBg = Color(0xFFE3E0F1),
        modifierBg = Color(0xFFD3CFE6),
        activeFill = Color(0xFFD2CBFA),
        accentBg = Color(0xFF6D28D9),
        shadowLight = Color(0xFFF3F1FA),
        shadowDark = Color(0xFFAEB0C9),
        keyBorder = Color.Black,
        primaryText = Color(0xFF2E244D),
        secondaryText = Color(0xFF5A5178),
        activeIndicator = Color(0xFF6D28D9),
        isDark = false,
    ),
    WARM_SAND_NEU(
        displayName = "Warm Stone",
        deckBg = Color(0xFFE2DCD4),
        keyBg = Color(0xFFE9E4DC),
        modifierBg = Color(0xFFD8D0C6),
        activeFill = Color(0xFFF2DBC2),
        accentBg = Color(0xFFD97706),
        shadowLight = Color(0xFFF6F2EC),
        shadowDark = Color(0xFFB8AEA2),
        keyBorder = Color.Black,
        primaryText = Color(0xFF332A22),
        secondaryText = Color(0xFF63574B),
        activeIndicator = Color(0xFFD97706),
        isDark = false,
    ),
    MINT_MIST_NEU(
        displayName = "Sage Mist",
        deckBg = Color(0xFFD1DFDA),
        keyBg = Color(0xFFD9E6E1),
        modifierBg = Color(0xFFC6D6D0),
        activeFill = Color(0xFFBFE7DA),
        accentBg = Color(0xFF0D9488),
        shadowLight = Color(0xFFECF4F1),
        shadowDark = Color(0xFFA2B7AF),
        keyBorder = Color.Black,
        primaryText = Color(0xFF1E352E),
        secondaryText = Color(0xFF46655C),
        activeIndicator = Color(0xFF0D9488),
        isDark = false,
    ),
    DARK_SLATE_NEU(
        displayName = "Dark Neumorph",
        deckBg = Color(0xFF222630),
        keyBg = Color(0xFF282D39),
        modifierBg = Color(0xFF1E222B),
        activeFill = Color(0xFF1E2A4A),
        accentBg = Color(0xFF6366F1),
        shadowLight = Color(0xFF353B49),
        shadowDark = Color(0xFF14171E),
        keyBorder = Color.Black,
        primaryText = Color(0xFFE2E8F0),
        secondaryText = Color(0xFF94A3B8),
        activeIndicator = Color(0xFF818CF8),
        isDark = true,
    ),
    MIDNIGHT_NEU(
        displayName = "Midnight Gaming",
        deckBg = Color(0xFF171D2B),
        keyBg = Color(0xFF1D2436),
        modifierBg = Color(0xFF151A27),
        activeFill = Color(0xFF1F355C),
        accentBg = Color(0xFF38BDF8),
        shadowLight = Color(0xFF2A344D),
        shadowDark = Color(0xFF0D111A),
        keyBorder = Color.Black,
        primaryText = Color(0xFFF1F5F9),
        secondaryText = Color(0xFF93C5FD),
        activeIndicator = Color(0xFF38BDF8),
        isDark = true,
    ),
    ROSE_QUARTZ(
        displayName = "Dusty Rose",
        deckBg = Color(0xFFE7D9DD),
        keyBg = Color(0xFFEEE1E5),
        modifierBg = Color(0xFFDDCBD0),
        activeFill = Color(0xFFF7CBD7),
        accentBg = Color(0xFFE11D48),
        shadowLight = Color(0xFFF7EEF1),
        shadowDark = Color(0xFFBDA8AE),
        keyBorder = Color.Black,
        primaryText = Color(0xFF3E242C),
        secondaryText = Color(0xFF6D4C56),
        activeIndicator = Color(0xFFE11D48),
        isDark = false,
    ),
    CYBER_MINT(
        displayName = "Carbon Stealth",
        deckBg = Color(0xFF16191D),
        keyBg = Color(0xFF1C2026),
        modifierBg = Color(0xFF13161A),
        activeFill = Color(0xFF13332C),
        accentBg = Color(0xFF10B981),
        shadowLight = Color(0xFF272C34),
        shadowDark = Color(0xFF0B0D10),
        keyBorder = Color.Black,
        primaryText = Color(0xFFE2FDF8),
        secondaryText = Color(0xFF6EE7B7),
        activeIndicator = Color(0xFF10B981),
        isDark = true,
    ),
    CUSTOM_STUDIO(
        displayName = "Custom Studio",
        deckBg = Color(0xFFD5DCE5),
        keyBg = Color(0xFFDCE1EA),
        modifierBg = Color(0xFFCFD6E2),
        activeFill = Color(0xFFCDE0F8),
        accentBg = Color(0xFF4F46E5),
        shadowLight = Color(0xFFF1F4F9),
        shadowDark = Color(0xFFA5AEBC),
        keyBorder = Color.Black,
        primaryText = Color(0xFF1F2937),
        secondaryText = Color(0xFF4B5563),
        activeIndicator = Color(0xFF4F46E5),
        isDark = false,
    ),
}

data class ResolvedKeyboardPalette(
    val deckBg: Color,
    val keyBg: Color,
    val modifierBg: Color,
    val activeFill: Color,
    val accentBg: Color,
    val shadowLight: Color,
    val shadowDark: Color,
    val keyBorder: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val activeIndicator: Color,
    val isDark: Boolean,
)

enum class ModifierKey {
    CTRL, ALT, SHIFT, META, FN, CAPS
}

enum class HotkeyCategory(val label: String) {
    ALL("All"),
    EDITING("Editing"),
    BROWSER("Browser"),
    NAVIGATION("Navigation"),
    CUSTOM("Custom"),
}

data class CustomHotkey(
    val id: String,
    val label: String,
    val keyCode: Int,
    val keyDisplay: String,
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
    val meta: Boolean = false,
    val category: HotkeyCategory = HotkeyCategory.CUSTOM,
    val accentHex: Long = 0xFF4F46E5,
) {
    fun comboText(): String {
        val parts = mutableListOf<String>()
        if (ctrl) parts += "Ctrl"
        if (alt) parts += "Alt"
        if (shift) parts += "Shift"
        if (meta) parts += "Win"
        parts += keyDisplay
        return parts.joinToString("+")
    }

    fun metaState(): Int {
        var metaFlags = 0
        if (ctrl) metaFlags = metaFlags or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (alt) metaFlags = metaFlags or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (shift) metaFlags = metaFlags or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        if (meta) metaFlags = metaFlags or KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
        return metaFlags
    }
}

data class KeySpec(
    val id: String,
    val label: String,
    val shiftLabel: String? = null,
    val fnLabel: String? = null,
    val keyCode: Int,
    val fnKeyCode: Int? = null,
    val charPrimary: Char? = null,
    val charShift: Char? = null,
    val weight: Float = 1f,
    val isModifier: Boolean = false,
    val modifierType: ModifierKey? = null,
    val isAccent: Boolean = false,
    val iconType: KeyIconType? = null,
)

enum class KeyIconType {
    BACKSPACE,
    TAB,
    ENTER,
    SHIFT,
    ARROW_UP,
    ARROW_DOWN,
    ARROW_LEFT,
    ARROW_RIGHT,
    META_WIN,
    SPACE,
    MIC_OR_NUM,
    LAYER_CIRCLE,
}

data class KeyDeckSettings(
    val layoutMode: KeyboardLayoutMode = KeyboardLayoutMode.PC_STANDARD,
    val themeId: KeyboardThemeId = KeyboardThemeId.SOFT_NEUMORPH,
    val showHotkeyBar: Boolean = true,
    val showModeStrip: Boolean = true,
    val showFunctionRow: Boolean = false,
    val numLockOn: Boolean = true,
    val stickyModifiers: Boolean = true,
    val hapticFeedback: Boolean = true,
    val suppressAutoImeOnInput: Boolean = true,
    // Granular size & tightness customization
    val keyHeightDp: Int = 38,
    val keyboardWidthFraction: Float = 1.0f,
    val dockAlignment: KeyboardDockAlignment = KeyboardDockAlignment.CENTER,
    val keyHorizontalGapDp: Int = 3,
    val keyVerticalGapDp: Int = 4,
    val deckPaddingDp: Int = 6,
    val keyCornerRadiusDp: Int = 10,
    val fontScale: Float = 0.96f,
    val overlayOpacity: Float = 0.96f,
    val bubbleSizeDp: Int = 48,
    // Visual borders & RGB
    val rgbBorderMode: RgbBorderMode = RgbBorderMode.GAMING_ONLY,
    val showKeyBorder: Boolean = true,
    // Gaming & Touchpad tuning
    val touchpadSensitivity: Float = 1.6f,
    val touchpadWidthFraction: Float = 0.42f,
    val gamingMcpeLookMode: Boolean = true,
    // Custom color overrides
    val overrideAccentHex: Long = 0L,
    val customDeckHex: Long = 0xFFD5DCE5,
    val customKeyHex: Long = 0xFFDCE1EA,
    val customModifierHex: Long = 0xFFCFD6E2,
    val customTextHex: Long = 0xFF1F2937,
    val customAccentHex: Long = 0xFF4F46E5,
) {
    fun isRgbActiveForCurrentMode(): Boolean =
        rgbBorderMode == RgbBorderMode.GAMING_ONLY && layoutMode == KeyboardLayoutMode.GAMING

    fun resolvePalette(): ResolvedKeyboardPalette {
        return if (themeId == KeyboardThemeId.CUSTOM_STUDIO) {
            val deck = Color(customDeckHex)
            val key = Color(customKeyHex)
            val mod = Color(customModifierHex)
            val text = Color(customTextHex)
            val accent = Color(if (overrideAccentHex != 0L) overrideAccentHex else customAccentHex)
            val isLightDeck = (deck.red * 0.299f + deck.green * 0.587f + deck.blue * 0.114f) > 0.5f
            val sLight = if (isLightDeck) Color(0xFFF3F6FA) else androidx.compose.ui.graphics.lerp(deck, Color.White, 0.12f)
            val sDark = if (isLightDeck) androidx.compose.ui.graphics.lerp(deck, Color(0xFF7B8596), 0.45f) else androidx.compose.ui.graphics.lerp(deck, Color.Black, 0.48f)
            val active = if (isLightDeck) Color(0xFFCDE0F8) else androidx.compose.ui.graphics.lerp(deck, accent, 0.26f)
            val border = Color.Black
            ResolvedKeyboardPalette(
                deckBg = deck,
                keyBg = key,
                modifierBg = mod,
                activeFill = active,
                accentBg = accent,
                shadowLight = sLight,
                shadowDark = sDark,
                keyBorder = border,
                primaryText = text,
                secondaryText = text.copy(alpha = 0.74f),
                activeIndicator = accent,
                isDark = !isLightDeck,
            )
        } else {
            val accent = if (overrideAccentHex != 0L) Color(overrideAccentHex) else themeId.accentBg
            val indicator = if (overrideAccentHex != 0L) Color(overrideAccentHex) else themeId.activeIndicator
            ResolvedKeyboardPalette(
                deckBg = themeId.deckBg,
                keyBg = themeId.keyBg,
                modifierBg = themeId.modifierBg,
                activeFill = themeId.activeFill,
                accentBg = accent,
                shadowLight = themeId.shadowLight,
                shadowDark = themeId.shadowDark,
                keyBorder = themeId.keyBorder,
                primaryText = themeId.primaryText,
                secondaryText = themeId.secondaryText,
                activeIndicator = indicator,
                isDark = themeId.isDark,
            )
        }
    }
}

data class DispatchedKeyRecord(
    val timestampMs: Long,
    val comboLabel: String,
    val keyCode: Int,
    val route: String,
    val detail: String,
)
