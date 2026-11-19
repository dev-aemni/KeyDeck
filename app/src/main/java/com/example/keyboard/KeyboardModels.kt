package com.example.keyboard

import android.view.KeyEvent
import androidx.compose.ui.graphics.Color

enum class KeyboardLayoutMode(val label: String, val subtitle: String) {
    PC_STANDARD("PC Full", "6-row PC QWERTY + F1-F12"),
    PC_WITH_NUMPAD("PC + Numpad", "QWERTY & side numeric pad"),
    NUMPAD_ONLY("Numeric Pad", "Full 4x5 dedicated numpad"),
}

enum class KeyboardDockAlignment(val label: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right"),
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
    MICRO("Micro", 26, 0.86f, 2, 2, 4, 0.80f, 8, 40),
    TIGHT("Tight", 32, 0.94f, 3, 3, 5, 0.90f, 10, 46),
    BALANCED("Balanced", 38, 1.00f, 4, 4, 7, 1.00f, 12, 52),
    COMFORT("Comfort", 46, 1.00f, 5, 5, 9, 1.12f, 14, 60),
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
    val primaryText: Color,
    val secondaryText: Color,
    val activeIndicator: Color,
    val isDark: Boolean = false,
) {
    SOFT_NEUMORPH(
        displayName = "Soft UI Light",
        deckBg = Color(0xFFE0E5EC),
        keyBg = Color(0xFFE0E5EC),
        modifierBg = Color(0xFFE0E5EC),
        activeFill = Color(0xFFD7E3F4),
        accentBg = Color(0xFF4F46E5),
        shadowLight = Color(0xFFFFFFFF),
        shadowDark = Color(0xFFB9BCC6),
        primaryText = Color(0xFF4B5563),
        secondaryText = Color(0xFF6B7280),
        activeIndicator = Color(0xFF4F46E5),
        isDark = false,
    ),
    LAVENDER_NEU(
        displayName = "Soft Lavender",
        deckBg = Color(0xFFE6E4F2),
        keyBg = Color(0xFFE6E4F2),
        modifierBg = Color(0xFFE0DDF0),
        activeFill = Color(0xFFD7D1F5),
        accentBg = Color(0xFF6D28D9),
        shadowLight = Color(0xFFFFFFFF),
        shadowDark = Color(0xFFBFC0D6),
        primaryText = Color(0xFF433863),
        secondaryText = Color(0xFF6B628A),
        activeIndicator = Color(0xFF6D28D9),
        isDark = false,
    ),
    WARM_SAND_NEU(
        displayName = "Warm Sand",
        deckBg = Color(0xFFECE6DF),
        keyBg = Color(0xFFECE6DF),
        modifierBg = Color(0xFFE5DDD4),
        activeFill = Color(0xFFF3DEC9),
        accentBg = Color(0xFFD97706),
        shadowLight = Color(0xFFFFFFFF),
        shadowDark = Color(0xFFC8BFB4),
        primaryText = Color(0xFF4A3F35),
        secondaryText = Color(0xFF786B5E),
        activeIndicator = Color(0xFFD97706),
        isDark = false,
    ),
    MINT_MIST_NEU(
        displayName = "Mint Mist",
        deckBg = Color(0xFFDCE9E5),
        keyBg = Color(0xFFDCE9E5),
        modifierBg = Color(0xFFD4E4DF),
        activeFill = Color(0xFFC6EAE0),
        accentBg = Color(0xFF0D9488),
        shadowLight = Color(0xFFFFFFFF),
        shadowDark = Color(0xFFB3C7C1),
        primaryText = Color(0xFF28423B),
        secondaryText = Color(0xFF527269),
        activeIndicator = Color(0xFF0D9488),
        isDark = false,
    ),
    DARK_SLATE_NEU(
        displayName = "Dark Neumorph",
        deckBg = Color(0xFF262A34),
        keyBg = Color(0xFF262A34),
        modifierBg = Color(0xFF232730),
        activeFill = Color(0xFF1F2A44),
        accentBg = Color(0xFF6366F1),
        shadowLight = Color(0xFF343946),
        shadowDark = Color(0xFF181B22),
        primaryText = Color(0xFFE2E8F0),
        secondaryText = Color(0xFF94A3B8),
        activeIndicator = Color(0xFF818CF8),
        isDark = true,
    ),
    MIDNIGHT_NEU(
        displayName = "Midnight Velvet",
        deckBg = Color(0xFF1B2233),
        keyBg = Color(0xFF1B2233),
        modifierBg = Color(0xFF181F2E),
        activeFill = Color(0xFF223354),
        accentBg = Color(0xFF38BDF8),
        shadowLight = Color(0xFF273147),
        shadowDark = Color(0xFF101521),
        primaryText = Color(0xFFF1F5F9),
        secondaryText = Color(0xFF93C5FD),
        activeIndicator = Color(0xFF38BDF8),
        isDark = true,
    ),
    ROSE_QUARTZ(
        displayName = "Rose Blush",
        deckBg = Color(0xFFF2E4E8),
        keyBg = Color(0xFFF2E4E8),
        modifierBg = Color(0xFFECDCE0),
        activeFill = Color(0xFFFAD1DC),
        accentBg = Color(0xFFE11D48),
        shadowLight = Color(0xFFFFFFFF),
        shadowDark = Color(0xFFCDB9BF),
        primaryText = Color(0xFF52353E),
        secondaryText = Color(0xFF7F5C67),
        activeIndicator = Color(0xFFE11D48),
        isDark = false,
    ),
    CYBER_MINT(
        displayName = "Deep Carbon",
        deckBg = Color(0xFF181B1F),
        keyBg = Color(0xFF181B1F),
        modifierBg = Color(0xFF14171A),
        activeFill = Color(0xFF16302B),
        accentBg = Color(0xFF10B981),
        shadowLight = Color(0xFF24292F),
        shadowDark = Color(0xFF0D0F12),
        primaryText = Color(0xFFE2FDF8),
        secondaryText = Color(0xFF6EE7B7),
        activeIndicator = Color(0xFF10B981),
        isDark = true,
    ),
    CUSTOM_STUDIO(
        displayName = "Custom Studio",
        deckBg = Color(0xFFE0E5EC),
        keyBg = Color(0xFFE0E5EC),
        modifierBg = Color(0xFFE0E5EC),
        activeFill = Color(0xFFD7E3F4),
        accentBg = Color(0xFF4F46E5),
        shadowLight = Color(0xFFFFFFFF),
        shadowDark = Color(0xFFB9BCC6),
        primaryText = Color(0xFF4B5563),
        secondaryText = Color(0xFF6B7280),
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
    val showFunctionRow: Boolean = true,
    val numLockOn: Boolean = true,
    val stickyModifiers: Boolean = true,
    val hapticFeedback: Boolean = true,
    // Granular size & tightness customization
    val keyHeightDp: Int = 34,
    val keyboardWidthFraction: Float = 1.0f,
    val dockAlignment: KeyboardDockAlignment = KeyboardDockAlignment.CENTER,
    val keyHorizontalGapDp: Int = 3,
    val keyVerticalGapDp: Int = 4,
    val deckPaddingDp: Int = 6,
    val keyCornerRadiusDp: Int = 11,
    val fontScale: Float = 0.92f,
    val overlayOpacity: Float = 0.97f,
    val bubbleSizeDp: Int = 48,
    // Custom color overrides
    val overrideAccentHex: Long = 0L,
    val customDeckHex: Long = 0xFFE0E5EC,
    val customKeyHex: Long = 0xFFE0E5EC,
    val customModifierHex: Long = 0xFFE0E5EC,
    val customTextHex: Long = 0xFF4B5563,
    val customAccentHex: Long = 0xFF4F46E5,
) {
    fun resolvePalette(): ResolvedKeyboardPalette {
        return if (themeId == KeyboardThemeId.CUSTOM_STUDIO) {
            val deck = Color(customDeckHex)
            val key = Color(customKeyHex)
            val mod = Color(customModifierHex)
            val text = Color(customTextHex)
            val accent = Color(if (overrideAccentHex != 0L) overrideAccentHex else customAccentHex)
            val isLightDeck = (deck.red * 0.299f + deck.green * 0.587f + deck.blue * 0.114f) > 0.5f
            val sLight = if (isLightDeck) Color.White else androidx.compose.ui.graphics.lerp(deck, Color.White, 0.12f)
            val sDark = if (isLightDeck) androidx.compose.ui.graphics.lerp(deck, Color(0xFF8A94A6), 0.42f) else androidx.compose.ui.graphics.lerp(deck, Color.Black, 0.45f)
            val active = if (isLightDeck) Color(0xFFD7E3F4) else androidx.compose.ui.graphics.lerp(deck, accent, 0.25f)
            ResolvedKeyboardPalette(
                deckBg = deck,
                keyBg = key,
                modifierBg = mod,
                activeFill = active,
                accentBg = accent,
                shadowLight = sLight,
                shadowDark = sDark,
                keyBorder = sLight.copy(alpha = 0.5f),
                primaryText = text,
                secondaryText = text.copy(alpha = 0.72f),
                activeIndicator = accent,
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
                keyBorder = themeId.shadowLight.copy(alpha = 0.55f),
                primaryText = themeId.primaryText,
                secondaryText = themeId.secondaryText,
                activeIndicator = indicator,
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
