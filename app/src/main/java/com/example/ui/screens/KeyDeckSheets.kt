package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyboard.KeyDeckSettings
import com.example.keyboard.KeyboardDockAlignment
import com.example.keyboard.KeyboardLayoutMode
import com.example.keyboard.KeyboardSizePreset
import com.example.keyboard.KeyboardThemeId
import com.example.keyboard.ResolvedKeyboardPalette
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeuActiveFill
import com.example.ui.theme.NeuBg
import com.example.ui.theme.NeuEmerald
import com.example.ui.theme.NeuIndigo
import com.example.ui.theme.NeuShadowDark
import com.example.ui.theme.NeuShadowLight
import com.example.ui.theme.NeuTextPrimary
import com.example.ui.theme.NeuTextSecondary
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised
import kotlin.math.roundToInt

private val ACCENT_SWATCHES = listOf(
    0xFF4F46E5L, // Indigo (Soft UI default)
    0xFF6D28D9L, // Violet
    0xFF0D9488L, // Teal
    0xFF10B981L, // Emerald
    0xFFE11D48L, // Rose
    0xFFD97706L, // Amber
    0xFF0284C7L, // Sky Blue
    0xFFEF6470L, // Coral
    0xFF6366F1L, // Soft Periwinkle
    0xFF334155L, // Slate
)

private val DECK_BG_SWATCHES = listOf(
    0xFFE0E5ECL, // Classic Soft UI
    0xFFE6E4F2L, // Soft Lavender
    0xFFECE6DFL, // Warm Sand
    0xFFDCE9E5L, // Mint Mist
    0xFFF2E4E8L, // Rose Blush
    0xFF262A34L, // Dark Neumorph
    0xFF1B2233L, // Midnight Velvet
    0xFF181B1FL, // Deep Carbon
)

private val KEY_BG_SWATCHES = listOf(
    0xFFE0E5ECL,
    0xFFE6E4F2L,
    0xFFECE6DFL,
    0xFFDCE9E5L,
    0xFFF2E4E8L,
    0xFFFFFFFFL,
    0xFF262A34L,
    0xFF1B2233L,
)

private val TEXT_SWATCHES = listOf(
    0xFF4B5563L,
    0xFF374151L,
    0xFF433863L,
    0xFF28423BL,
    0xFFF1F5F9L,
    0xFFE2FDF8L,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerSheet(
    settings: KeyDeckSettings,
    onSelectTheme: (KeyboardThemeId) -> Unit,
    onUpdateSettings: ((KeyDeckSettings) -> KeyDeckSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val resolvedPalette = settings.resolvePalette()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NeuBg,
        contentColor = NeuTextPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Themes & Color Studio",
                style = MaterialTheme.typography.headlineMedium,
                color = NeuTextPrimary,
            )

            MiniKeyboardPreviewCard(
                palette = resolvedPalette,
                cornerDp = settings.keyCornerRadiusDp,
                opacity = settings.overlayOpacity,
            )

            Text(
                text = "Soft Neumorphic Presets",
                style = MaterialTheme.typography.labelLarge,
                color = NeuTextSecondary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KeyboardThemeId.entries.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        pair.forEach { theme ->
                            val selected = settings.themeId == theme
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (selected) {
                                            Modifier.neumorphicInset(
                                                surfaceColor = NeuActiveFill,
                                                shadowLight = NeuShadowLight,
                                                shadowDark = NeuShadowDark,
                                                cornerRadius = 14.dp,
                                                depth = 4.dp,
                                            )
                                        } else {
                                            Modifier.neumorphicRaised(
                                                surfaceColor = NeuBg,
                                                shadowLight = NeuShadowLight,
                                                shadowDark = NeuShadowDark,
                                                cornerRadius = 14.dp,
                                                shadowOffset = 4.dp,
                                                blurRadius = 8.dp,
                                            )
                                        },
                                    )
                                    .clickable { onSelectTheme(theme) }
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                                    .testTag("theme_option_${theme.name.lowercase()}"),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(theme.deckBg)
                                                .border(2.dp, theme.accentBg, CircleShape),
                                        )
                                        Text(
                                            text = theme.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selected) NeuIndigo else NeuTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = NeuIndigo,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Accent Color", style = MaterialTheme.typography.labelLarge, color = NeuTextSecondary)
                    if (settings.overrideAccentHex != 0L) {
                        Text(
                            text = "Reset",
                            fontSize = 12.sp,
                            color = NeuIndigo,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onUpdateSettings { it.copy(overrideAccentHex = 0L) } }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                ColorSwatchRow(
                    swatches = ACCENT_SWATCHES,
                    selectedHex = if (settings.overrideAccentHex != 0L) settings.overrideAccentHex else -1L,
                    onSelect = { hex -> onUpdateSettings { it.copy(overrideAccentHex = hex) } },
                )
            }

            if (settings.themeId == KeyboardThemeId.CUSTOM_STUDIO) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphicInset(
                            surfaceColor = NeuBg,
                            shadowLight = NeuShadowLight,
                            shadowDark = NeuShadowDark,
                            cornerRadius = 16.dp,
                            depth = 4.dp,
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Custom Studio Colors", style = MaterialTheme.typography.titleMedium, color = NeuIndigo)
                    Text("Deck Surface", fontSize = 12.sp, color = NeuTextSecondary)
                    ColorSwatchRow(
                        swatches = DECK_BG_SWATCHES,
                        selectedHex = settings.customDeckHex,
                        onSelect = { hex -> onUpdateSettings { it.copy(customDeckHex = hex) } },
                    )
                    Text("Keycap Surface", fontSize = 12.sp, color = NeuTextSecondary)
                    ColorSwatchRow(
                        swatches = KEY_BG_SWATCHES,
                        selectedHex = settings.customKeyHex,
                        onSelect = { hex ->
                            val modArgb = lerp(Color(hex), Color.Black, 0.08f).toArgb().toLong() and 0xFFFFFFFFL
                            onUpdateSettings {
                                it.copy(
                                    customKeyHex = hex,
                                    customModifierHex = modArgb,
                                )
                            }
                        },
                    )
                    Text("Legend Text", fontSize = 12.sp, color = NeuTextSecondary)
                    ColorSwatchRow(
                        swatches = TEXT_SWATCHES,
                        selectedHex = settings.customTextHex,
                        onSelect = { hex -> onUpdateSettings { it.copy(customTextHex = hex) } },
                    )
                }
            }

            LabeledSlider(
                label = "Key Corner Softness",
                valueLabel = "${settings.keyCornerRadiusDp} dp",
                value = settings.keyCornerRadiusDp.toFloat(),
                range = 4f..16f,
                accentColor = NeuIndigo,
                onValueChange = { v -> onUpdateSettings { it.copy(keyCornerRadiusDp = v.roundToInt()) } },
            )

            LabeledSlider(
                label = "Overlay Opacity",
                valueLabel = "${(settings.overlayOpacity * 100).roundToInt()}%",
                value = settings.overlayOpacity,
                range = 0.50f..1.0f,
                accentColor = NeuIndigo,
                onValueChange = { v -> onUpdateSettings { it.copy(overlayOpacity = v) } },
            )

            NeuSheetPrimaryButton(
                label = "Done",
                testTag = "theme_sheet_done",
                onClick = onDismiss,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutPickerSheet(
    settings: KeyDeckSettings,
    onSelectLayout: (KeyboardLayoutMode) -> Unit,
    onUpdateSettings: ((KeyDeckSettings) -> KeyDeckSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = settings.resolvePalette()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NeuBg,
        contentColor = NeuTextPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Size, Tightness & Layout",
                style = MaterialTheme.typography.headlineMedium,
                color = NeuTextPrimary,
            )

            Text("Quick Size Presets", style = MaterialTheme.typography.labelLarge, color = NeuTextSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KeyboardSizePreset.entries.forEach { preset ->
                    val isActive = settings.keyHeightDp == preset.keyHeightDp &&
                        kotlin.math.abs(settings.keyboardWidthFraction - preset.widthFraction) < 0.02f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .then(
                                if (isActive) {
                                    Modifier.neumorphicInset(
                                        surfaceColor = NeuActiveFill,
                                        shadowLight = NeuShadowLight,
                                        shadowDark = NeuShadowDark,
                                        cornerRadius = 12.dp,
                                        depth = 4.dp,
                                    )
                                } else {
                                    Modifier.neumorphicRaised(
                                        surfaceColor = NeuBg,
                                        shadowLight = NeuShadowLight,
                                        shadowDark = NeuShadowDark,
                                        cornerRadius = 12.dp,
                                        shadowOffset = 4.dp,
                                        blurRadius = 7.dp,
                                    )
                                },
                            )
                            .clickable {
                                onUpdateSettings {
                                    it.copy(
                                        keyHeightDp = preset.keyHeightDp,
                                        keyboardWidthFraction = preset.widthFraction,
                                        keyHorizontalGapDp = preset.hGapDp,
                                        keyVerticalGapDp = preset.vGapDp,
                                        deckPaddingDp = preset.deckPaddingDp,
                                        fontScale = preset.fontScale,
                                        keyCornerRadiusDp = preset.cornerRadiusDp,
                                        bubbleSizeDp = preset.bubbleSizeDp,
                                    )
                                }
                            }
                            .testTag("size_preset_${preset.name.lowercase()}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = preset.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) NeuIndigo else NeuTextPrimary,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicInset(
                        surfaceColor = NeuBg,
                        shadowLight = NeuShadowLight,
                        shadowDark = NeuShadowDark,
                        cornerRadius = 14.dp,
                        depth = 4.dp,
                    )
                    .padding(10.dp),
                contentAlignment = when (settings.dockAlignment) {
                    KeyboardDockAlignment.LEFT -> Alignment.CenterStart
                    KeyboardDockAlignment.CENTER -> Alignment.Center
                    KeyboardDockAlignment.RIGHT -> Alignment.CenterEnd
                },
            ) {
                MiniKeyboardPreviewCard(
                    palette = palette,
                    cornerDp = settings.keyCornerRadiusDp,
                    opacity = settings.overlayOpacity,
                    modifier = Modifier.fillMaxWidth(settings.keyboardWidthFraction),
                )
            }

            LabeledSlider(
                label = "Keycap Height (Compact <-> Tall)",
                valueLabel = "${settings.keyHeightDp} dp",
                value = settings.keyHeightDp.toFloat(),
                range = 24f..54f,
                accentColor = NeuIndigo,
                onValueChange = { v -> onUpdateSettings { it.copy(keyHeightDp = v.roundToInt()) } },
            )

            LabeledSlider(
                label = "Keyboard Width",
                valueLabel = "${(settings.keyboardWidthFraction * 100).roundToInt()}%",
                value = settings.keyboardWidthFraction,
                range = 0.60f..1.0f,
                accentColor = NeuIndigo,
                onValueChange = { v -> onUpdateSettings { it.copy(keyboardWidthFraction = v) } },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Horizontal Dock", style = MaterialTheme.typography.bodyLarge, color = NeuTextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyboardDockAlignment.entries.forEach { align ->
                        val selected = settings.dockAlignment == align
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .then(
                                    if (selected) {
                                        Modifier.neumorphicInset(
                                            surfaceColor = NeuActiveFill,
                                            shadowLight = NeuShadowLight,
                                            shadowDark = NeuShadowDark,
                                            cornerRadius = 10.dp,
                                            depth = 3.dp,
                                        )
                                    } else {
                                        Modifier.neumorphicRaised(
                                            surfaceColor = NeuBg,
                                            shadowLight = NeuShadowLight,
                                            shadowDark = NeuShadowDark,
                                            cornerRadius = 10.dp,
                                            shadowOffset = 3.dp,
                                            blurRadius = 6.dp,
                                        )
                                    },
                                )
                                .clickable { onUpdateSettings { it.copy(dockAlignment = align) } }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = align.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) NeuIndigo else NeuTextPrimary,
                            )
                        }
                    }
                }
            }

            LabeledSlider(
                label = "Horizontal Key Gap (Tightness)",
                valueLabel = "${settings.keyHorizontalGapDp} dp",
                value = settings.keyHorizontalGapDp.toFloat(),
                range = 1f..6f,
                accentColor = NeuEmerald,
                onValueChange = { v -> onUpdateSettings { it.copy(keyHorizontalGapDp = v.roundToInt()) } },
            )

            LabeledSlider(
                label = "Vertical Row Gap",
                valueLabel = "${settings.keyVerticalGapDp} dp",
                value = settings.keyVerticalGapDp.toFloat(),
                range = 1f..7f,
                accentColor = NeuEmerald,
                onValueChange = { v -> onUpdateSettings { it.copy(keyVerticalGapDp = v.roundToInt()) } },
            )

            LabeledSlider(
                label = "Outer Deck Padding",
                valueLabel = "${settings.deckPaddingDp} dp",
                value = settings.deckPaddingDp.toFloat(),
                range = 1f..10f,
                accentColor = NeuEmerald,
                onValueChange = { v -> onUpdateSettings { it.copy(deckPaddingDp = v.roundToInt()) } },
            )

            LabeledSlider(
                label = "Key Label Scale",
                valueLabel = "${(settings.fontScale * 100).roundToInt()}%",
                value = settings.fontScale,
                range = 0.70f..1.25f,
                accentColor = NeuIndigo,
                onValueChange = { v -> onUpdateSettings { it.copy(fontScale = v) } },
            )

            Text("Keyboard Mode", style = MaterialTheme.typography.labelLarge, color = NeuTextSecondary)
            KeyboardLayoutMode.entries.forEach { mode ->
                val selected = settings.layoutMode == mode
                val icon = when (mode) {
                    KeyboardLayoutMode.PC_STANDARD -> Icons.Default.Keyboard
                    KeyboardLayoutMode.PC_WITH_NUMPAD -> Icons.Default.ViewColumn
                    KeyboardLayoutMode.NUMPAD_ONLY -> Icons.Default.Dialpad
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (selected) {
                                Modifier.neumorphicInset(
                                    surfaceColor = NeuActiveFill,
                                    shadowLight = NeuShadowLight,
                                    shadowDark = NeuShadowDark,
                                    cornerRadius = 14.dp,
                                    depth = 4.dp,
                                )
                            } else {
                                Modifier.neumorphicRaised(
                                    surfaceColor = NeuBg,
                                    shadowLight = NeuShadowLight,
                                    shadowDark = NeuShadowDark,
                                    cornerRadius = 14.dp,
                                    shadowOffset = 4.dp,
                                    blurRadius = 8.dp,
                                )
                            },
                        )
                        .clickable { onSelectLayout(mode) }
                        .padding(14.dp)
                        .testTag("layout_option_${mode.name.lowercase()}"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = mode.label,
                                tint = if (selected) NeuIndigo else NeuTextSecondary,
                            )
                            Column {
                                Text(
                                    mode.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selected) NeuIndigo else NeuTextPrimary,
                                )
                                Text(
                                    mode.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeuTextSecondary,
                                )
                            }
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected layout",
                                tint = NeuIndigo,
                            )
                        }
                    }
                }
            }

            SettingsSwitchRow(
                title = "Show Top Mode Strip",
                subtitle = "Hide PC / PC+NUM / NUM chips for a shorter, slimmer deck",
                checked = settings.showModeStrip,
                testTag = "switch_mode_strip",
                onCheckedChange = { checked -> onUpdateSettings { it.copy(showModeStrip = checked) } },
            )

            SettingsSwitchRow(
                title = "Show Customizable Hotkey Bar",
                subtitle = "Pin quick-fire macro shortcuts above row 1",
                checked = settings.showHotkeyBar,
                testTag = "switch_hotkey_bar",
                onCheckedChange = { checked -> onUpdateSettings { it.copy(showHotkeyBar = checked) } },
            )

            SettingsSwitchRow(
                title = "Always Show Function Row (F1–F12)",
                subtitle = "Keep Esc and F1–F12 visible at the top like a PC keyboard",
                checked = settings.showFunctionRow,
                testTag = "switch_function_row",
                onCheckedChange = { checked -> onUpdateSettings { it.copy(showFunctionRow = checked) } },
            )

            NeuSheetPrimaryButton(
                label = "Apply Layout",
                testTag = "layout_sheet_done",
                onClick = onDismiss,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: KeyDeckSettings,
    onUpdateSettings: ((KeyDeckSettings) -> KeyDeckSettings) -> Unit,
    onResetHotkeys: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NeuBg,
        contentColor = NeuTextPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "KeyDeck Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = NeuTextPrimary,
            )

            SettingsSwitchRow(
                title = "Sticky Modifiers (Ctrl / Alt / Shift)",
                subtitle = "Tap Ctrl once, then tap C — modifier clears automatically after key",
                checked = settings.stickyModifiers,
                testTag = "switch_sticky_modifiers",
                onCheckedChange = { checked -> onUpdateSettings { it.copy(stickyModifiers = checked) } },
            )

            SettingsSwitchRow(
                title = "Tactile Haptic Feedback",
                subtitle = "Subtle mechanical pulse on every keycap & hotkey press",
                checked = settings.hapticFeedback,
                testTag = "switch_haptics",
                onCheckedChange = { checked -> onUpdateSettings { it.copy(hapticFeedback = checked) } },
            )

            SettingsSwitchRow(
                title = "NumLock Active by Default",
                subtitle = "Numpad outputs 0–9 when ON, or Home/End/PgUp/Arrows when OFF",
                checked = settings.numLockOn,
                testTag = "switch_numlock",
                onCheckedChange = { checked -> onUpdateSettings { it.copy(numLockOn = checked) } },
            )

            LabeledSlider(
                label = "Floating Bubble Size",
                valueLabel = "${settings.bubbleSizeDp} dp",
                value = settings.bubbleSizeDp.toFloat(),
                range = 36f..68f,
                accentColor = NeuIndigo,
                onValueChange = { v -> onUpdateSettings { it.copy(bubbleSizeDp = v.roundToInt()) } },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicInset(
                        surfaceColor = NeuBg,
                        shadowLight = NeuShadowLight,
                        shadowDark = NeuShadowDark,
                        cornerRadius = 14.dp,
                        depth = 4.dp,
                    )
                    .padding(14.dp),
            ) {
                Text(
                    text = "Session Lifecycle: KeyDeck's floating keyboard is bound to the active app task (stopWithTask=true). Closing KeyDeck from Recents stops the overlay immediately.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeuTextSecondary,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .neumorphicRaised(
                        surfaceColor = NeuBg,
                        shadowLight = NeuShadowLight,
                        shadowDark = NeuShadowDark,
                        cornerRadius = 14.dp,
                        shadowOffset = 4.dp,
                        blurRadius = 8.dp,
                    )
                    .clickable { onResetHotkeys() }
                    .testTag("reset_hotkeys_button"),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = NeuIndigo,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Restore Default Hotkeys",
                        color = NeuIndigo,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            NeuSheetPrimaryButton(
                label = "Close",
                testTag = "settings_sheet_done",
                onClick = onDismiss,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NeuSheetPrimaryButton(
    label: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .neumorphicRaised(
                surfaceColor = NeuIndigo,
                shadowLight = NeuShadowLight,
                shadowDark = NeuShadowDark,
                cornerRadius = 14.dp,
                shadowOffset = 4.dp,
                blurRadius = 8.dp,
            )
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun MiniKeyboardPreviewCard(
    palette: ResolvedKeyboardPalette,
    cornerDp: Int,
    opacity: Float,
    modifier: Modifier = Modifier,
) {
    val corner = (cornerDp - 2).coerceIn(4, 14).dp
    Column(
        modifier = modifier
            .neumorphicRaised(
                surfaceColor = palette.deckBg.copy(alpha = opacity),
                shadowLight = palette.shadowLight,
                shadowDark = palette.shadowDark,
                cornerRadius = 16.dp,
                shadowOffset = 4.dp,
                blurRadius = 8.dp,
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("ctrl", "q", "w", "e", "r", "t", "enter").forEachIndexed { idx, label ->
                val isMod = idx == 0
                val isAcc = label == "enter"
                Box(
                    modifier = Modifier
                        .weight(if (isMod || isAcc) 1.5f else 1f)
                        .height(26.dp)
                        .then(
                            if (isAcc) {
                                Modifier.neumorphicInset(
                                    surfaceColor = palette.activeFill,
                                    shadowLight = palette.shadowLight,
                                    shadowDark = palette.shadowDark,
                                    cornerRadius = corner,
                                    depth = 2.5.dp,
                                )
                            } else {
                                Modifier.neumorphicRaised(
                                    surfaceColor = if (isMod) palette.modifierBg else palette.keyBg,
                                    shadowLight = palette.shadowLight,
                                    shadowDark = palette.shadowDark,
                                    cornerRadius = corner,
                                    shadowOffset = 2.dp,
                                    blurRadius = 4.dp,
                                )
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontFamily = MonoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAcc) palette.accentBg else palette.primaryText,
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("shift", "a", "s", "space", "fn", "◁", "▷").forEachIndexed { idx, label ->
                val isMod = idx == 0 || idx >= 4
                val isSpace = label == "space"
                Box(
                    modifier = Modifier
                        .weight(if (isSpace) 2.5f else 1f)
                        .height(24.dp)
                        .neumorphicRaised(
                            surfaceColor = if (isMod) palette.modifierBg else palette.keyBg,
                            shadowLight = palette.shadowLight,
                            shadowDark = palette.shadowDark,
                            cornerRadius = corner,
                            shadowOffset = 2.dp,
                            blurRadius = 4.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontFamily = MonoFontFamily,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (label == "fn") palette.activeIndicator else palette.primaryText,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchRow(
    swatches: List<Long>,
    selectedHex: Long,
    onSelect: (Long) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        swatches.forEach { hex ->
            val selected = hex == selectedHex
            val swatchColor = Color(hex)
            val isLight = (swatchColor.red * 0.299f + swatchColor.green * 0.587f + swatchColor.blue * 0.114f) > 0.58f
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .neumorphicRaised(
                        surfaceColor = swatchColor,
                        shadowLight = NeuShadowLight,
                        shadowDark = NeuShadowDark,
                        cornerRadius = 17.dp,
                        shadowOffset = 2.dp,
                        blurRadius = 4.dp,
                    )
                    .border(
                        width = if (selected) 2.5.dp else 1.dp,
                        color = if (selected) NeuIndigo else NeuShadowDark.copy(alpha = 0.5f),
                        shape = CircleShape,
                    )
                    .clickable { onSelect(hex) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected color",
                        tint = if (isLight) NeuIndigo else Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = NeuTextPrimary)
            Text(
                valueLabel,
                fontFamily = MonoFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = NeuShadowDark.copy(alpha = 0.45f),
            ),
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicRaised(
                surfaceColor = NeuBg,
                shadowLight = NeuShadowLight,
                shadowDark = NeuShadowDark,
                cornerRadius = 14.dp,
                shadowOffset = 3.dp,
                blurRadius = 6.dp,
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = NeuTextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = NeuTextSecondary)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NeuIndigo,
                uncheckedThumbColor = NeuTextSecondary,
                uncheckedTrackColor = NeuBg,
                uncheckedBorderColor = NeuShadowDark,
            ),
        )
    }
}
