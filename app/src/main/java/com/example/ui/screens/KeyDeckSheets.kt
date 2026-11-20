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
import androidx.compose.material.icons.filled.SportsEsports
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
import com.example.keyboard.AppThemeMode
import com.example.keyboard.KeyDeckSettings
import com.example.keyboard.KeyboardDockAlignment
import com.example.keyboard.KeyboardLayoutMode
import com.example.keyboard.KeyboardSizePreset
import com.example.keyboard.KeyboardThemeId
import com.example.keyboard.ResolvedKeyboardPalette
import com.example.keyboard.RgbBorderMode
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.KeyDeckColors
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
    0xFFD5DCE5L, // Soft UI Slate
    0xFFDCD9ECL, // Soft Lavender
    0xFFE2DCD4L, // Warm Stone
    0xFFD1DFDAL, // Sage Mist
    0xFFE7D9DDL, // Dusty Rose
    0xFF222630L, // Dark Neumorph
    0xFF171D2BL, // Midnight Gaming
    0xFF16191DL, // Carbon Stealth
)

private val KEY_BG_SWATCHES = listOf(
    0xFFDCE1EAL,
    0xFFE3E0F1L,
    0xFFE9E4DCL,
    0xFFD9E6E1L,
    0xFFEEE1E5L,
    0xFFCFD6E2L,
    0xFF282D39L,
    0xFF1D2436L,
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
    val colors = KeyDeckColors.current
    val resolvedPalette = settings.resolvePalette(colors.isDark)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.bg,
        contentColor = colors.textPrimary,
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
                color = colors.textPrimary,
            )

            AppThemeModeSelector(
                selectedMode = settings.appThemeMode,
                onSelectMode = { mode -> onUpdateSettings { it.copy(appThemeMode = mode) } },
            )

            SettingsSwitchRow(
                title = "Auto Dark Keyboard",
                subtitle = "Switch default Soft UI keyboard to Dark Neumorph in Dark Mode",
                checked = settings.syncKeyboardWithDarkMode,
                testTag = "switch_sync_keyboard_dark",
                onCheckedChange = { enabled -> onUpdateSettings { it.copy(syncKeyboardWithDarkMode = enabled) } },
            )

            MiniKeyboardPreviewCard(
                palette = resolvedPalette,
                cornerDp = settings.keyCornerRadiusDp,
                opacity = settings.overlayOpacity,
                showKeyBorder = settings.showKeyBorder,
            )

            Text(
                text = "Soft Neumorphic Presets",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
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
                                                surfaceColor = colors.activeFill,
                                                shadowLight = colors.shadowLight,
                                                shadowDark = colors.shadowDark,
                                                cornerRadius = 14.dp,
                                                depth = 4.dp,
                                                borderColor = colors.keyBorder.copy(alpha = 0.45f),
                                            )
                                        } else {
                                            Modifier.neumorphicRaised(
                                                surfaceColor = colors.bg,
                                                shadowLight = colors.shadowLight,
                                                shadowDark = colors.shadowDark,
                                                cornerRadius = 14.dp,
                                                shadowOffset = 4.dp,
                                                blurRadius = 8.dp,
                                                borderColor = colors.keyBorder.copy(alpha = 0.45f),
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
                                            color = if (selected) colors.indigo else colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = colors.indigo,
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
                    Text("Accent Color", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                    if (settings.overrideAccentHex != 0L) {
                        Text(
                            text = "Reset",
                            fontSize = 12.sp,
                            color = colors.indigo,
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
                            surfaceColor = colors.bg,
                            shadowLight = colors.shadowLight,
                            shadowDark = colors.shadowDark,
                            cornerRadius = 16.dp,
                            depth = 4.dp,
                            borderColor = colors.keyBorder.copy(alpha = 0.45f),
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Custom Studio Colors", style = MaterialTheme.typography.titleMedium, color = colors.indigo)
                    Text("Deck Surface", fontSize = 12.sp, color = colors.textSecondary)
                    ColorSwatchRow(
                        swatches = DECK_BG_SWATCHES,
                        selectedHex = settings.customDeckHex,
                        onSelect = { hex -> onUpdateSettings { it.copy(customDeckHex = hex) } },
                    )
                    Text("Keycap Surface", fontSize = 12.sp, color = colors.textSecondary)
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
                    Text("Legend Text", fontSize = 12.sp, color = colors.textSecondary)
                    ColorSwatchRow(
                        swatches = TEXT_SWATCHES,
                        selectedHex = settings.customTextHex,
                        onSelect = { hex -> onUpdateSettings { it.copy(customTextHex = hex) } },
                    )
                }
            }

            Text(
                text = "RGB Animated Key Border",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RgbBorderMode.entries.forEach { mode ->
                    val selected = settings.rgbBorderMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .then(
                                if (selected) {
                                    Modifier.neumorphicInset(
                                        surfaceColor = colors.activeFill,
                                        shadowLight = colors.shadowLight,
                                        shadowDark = colors.shadowDark,
                                        cornerRadius = 12.dp,
                                        depth = 4.dp,
                                        borderColor = colors.keyBorder.copy(alpha = 0.45f),
                                    )
                                } else {
                                    Modifier.neumorphicRaised(
                                        surfaceColor = colors.bg,
                                        shadowLight = colors.shadowLight,
                                        shadowDark = colors.shadowDark,
                                        cornerRadius = 12.dp,
                                        shadowOffset = 4.dp,
                                        blurRadius = 7.dp,
                                        borderColor = colors.keyBorder.copy(alpha = 0.45f),
                                    )
                                },
                            )
                            .clickable { onUpdateSettings { it.copy(rgbBorderMode = mode) } }
                            .testTag("rgb_mode_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = mode.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) colors.indigo else colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            SettingsSwitchRow(
                title = "Black Key Borders",
                subtitle = "Define key edges cleanly with a crisp black border (Gaming uses RGB)",
                checked = settings.showKeyBorder,
                testTag = "switch_key_border",
                onCheckedChange = { enabled -> onUpdateSettings { it.copy(showKeyBorder = enabled) } },
            )

            LabeledSlider(
                label = "Key Corner Softness",
                valueLabel = "${settings.keyCornerRadiusDp} dp",
                value = settings.keyCornerRadiusDp.toFloat(),
                range = 4f..16f,
                accentColor = colors.indigo,
                onValueChange = { v -> onUpdateSettings { it.copy(keyCornerRadiusDp = v.roundToInt()) } },
            )

            LabeledSlider(
                label = "Overlay Opacity",
                valueLabel = "${(settings.overlayOpacity * 100).roundToInt()}%",
                value = settings.overlayOpacity,
                range = 0.50f..1.0f,
                accentColor = colors.indigo,
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
    val colors = KeyDeckColors.current
    val palette = settings.resolvePalette(colors.isDark)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.bg,
        contentColor = colors.textPrimary,
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
                color = colors.textPrimary,
            )

            Text("Quick Size Presets", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
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
                                        surfaceColor = colors.activeFill,
                                        shadowLight = colors.shadowLight,
                                        shadowDark = colors.shadowDark,
                                        cornerRadius = 12.dp,
                                        depth = 4.dp,
                                        borderColor = colors.keyBorder.copy(alpha = 0.45f),
                                    )
                                } else {
                                    Modifier.neumorphicRaised(
                                        surfaceColor = colors.bg,
                                        shadowLight = colors.shadowLight,
                                        shadowDark = colors.shadowDark,
                                        cornerRadius = 12.dp,
                                        shadowOffset = 4.dp,
                                        blurRadius = 7.dp,
                                        borderColor = colors.keyBorder.copy(alpha = 0.45f),
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
                            color = if (isActive) colors.indigo else colors.textPrimary,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicInset(
                        surfaceColor = colors.bg,
                        shadowLight = colors.shadowLight,
                        shadowDark = colors.shadowDark,
                        cornerRadius = 14.dp,
                        depth = 4.dp,
                        borderColor = colors.keyBorder.copy(alpha = 0.45f),
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
                    showKeyBorder = settings.showKeyBorder,
                    modifier = Modifier.fillMaxWidth(settings.keyboardWidthFraction),
                )
            }

            LabeledSlider(
                label = "Keycap Height (Compact <-> Tall)",
                valueLabel = "${settings.keyHeightDp} dp",
                value = settings.keyHeightDp.toFloat(),
                range = 24f..54f,
                accentColor = colors.indigo,
                onValueChange = { v -> onUpdateSettings { it.copy(keyHeightDp = v.roundToInt()) } },
            )

            LabeledSlider(
                label = "Keyboard Width",
                valueLabel = "${(settings.keyboardWidthFraction * 100).roundToInt()}%",
                value = settings.keyboardWidthFraction,
                range = 0.60f..1.0f,
                accentColor = colors.indigo,
                onValueChange = { v -> onUpdateSettings { it.copy(keyboardWidthFraction = v) } },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Horizontal Dock", style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyboardDockAlignment.entries.forEach { align ->
                        val selected = settings.dockAlignment == align
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .then(
                                    if (selected) {
                                        Modifier.neumorphicInset(
                                            surfaceColor = colors.activeFill,
                                            shadowLight = colors.shadowLight,
                                            shadowDark = colors.shadowDark,
                                            cornerRadius = 10.dp,
                                            depth = 3.dp,
                                            borderColor = colors.keyBorder.copy(alpha = 0.45f),
                                        )
                                    } else {
                                        Modifier.neumorphicRaised(
                                            surfaceColor = colors.bg,
                                            shadowLight = colors.shadowLight,
                                            shadowDark = colors.shadowDark,
                                            cornerRadius = 10.dp,
                                            shadowOffset = 3.dp,
                                            blurRadius = 6.dp,
                                            borderColor = colors.keyBorder.copy(alpha = 0.45f),
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
                                color = if (selected) colors.indigo else colors.textPrimary,
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
                accentColor = colors.emerald,
                onValueChange = { v -> onUpdateSettings { it.copy(keyHorizontalGapDp = v.roundToInt()) } },
            )

            LabeledSlider(
                label = "Vertical Row Gap",
                valueLabel = "${settings.keyVerticalGapDp} dp",
                value = settings.keyVerticalGapDp.toFloat(),
                range = 1f..7f,
                accentColor = colors.emerald,
                onValueChange = { v -> onUpdateSettings { it.copy(keyVerticalGapDp = v.roundToInt()) } },
            )

            LabeledSlider(
                label = "Outer Deck Padding",
                valueLabel = "${settings.deckPaddingDp} dp",
                value = settings.deckPaddingDp.toFloat(),
                range = 1f..10f,
                accentColor = colors.emerald,
                onValueChange = { v -> onUpdateSettings { it.copy(deckPaddingDp = v.roundToInt()) } },
            )

            LabeledSlider(
                label = "Key Label Scale",
                valueLabel = "${(settings.fontScale * 100).roundToInt()}%",
                value = settings.fontScale,
                range = 0.70f..1.25f,
                accentColor = colors.indigo,
                onValueChange = { v -> onUpdateSettings { it.copy(fontScale = v) } },
            )

            Text("Keyboard Mode", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            KeyboardLayoutMode.entries.forEach { mode ->
                val selected = settings.layoutMode == mode
                val icon = when (mode) {
                    KeyboardLayoutMode.PC_STANDARD -> Icons.Default.Keyboard
                    KeyboardLayoutMode.PC_WITH_NUMPAD -> Icons.Default.ViewColumn
                    KeyboardLayoutMode.NUMPAD_ONLY -> Icons.Default.Dialpad
                    KeyboardLayoutMode.GAMING -> Icons.Default.SportsEsports
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (selected) {
                                Modifier.neumorphicInset(
                                    surfaceColor = colors.activeFill,
                                    shadowLight = colors.shadowLight,
                                    shadowDark = colors.shadowDark,
                                    cornerRadius = 14.dp,
                                    depth = 4.dp,
                                    borderColor = colors.keyBorder.copy(alpha = 0.45f),
                                )
                            } else {
                                Modifier.neumorphicRaised(
                                    surfaceColor = colors.bg,
                                    shadowLight = colors.shadowLight,
                                    shadowDark = colors.shadowDark,
                                    cornerRadius = 14.dp,
                                    shadowOffset = 4.dp,
                                    blurRadius = 8.dp,
                                    borderColor = colors.keyBorder.copy(alpha = 0.45f),
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
                                tint = if (selected) colors.indigo else colors.textSecondary,
                            )
                            Column {
                                Text(
                                    mode.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selected) colors.indigo else colors.textPrimary,
                                )
                                Text(
                                    mode.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                )
                            }
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected layout",
                                tint = colors.indigo,
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
    val colors = KeyDeckColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.bg,
        contentColor = colors.textPrimary,
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
                color = colors.textPrimary,
            )

            AppThemeModeSelector(
                selectedMode = settings.appThemeMode,
                onSelectMode = { mode -> onUpdateSettings { it.copy(appThemeMode = mode) } },
            )

            SettingsSwitchRow(
                title = "Manual Bubble Only (No Input Auto-Open)",
                subtitle = "Prevent keyboards from popping open on input taps so two keyboards never stack",
                checked = settings.suppressAutoImeOnInput,
                testTag = "switch_suppress_auto_ime",
                onCheckedChange = { checked -> onUpdateSettings { it.copy(suppressAutoImeOnInput = checked) } },
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

            SettingsSwitchRow(
                title = "Gaming MCPE Camera Look Mode",
                subtitle = "Touchpad swipes look around in Minecraft PE; turn off for cursor pointer",
                checked = settings.gamingMcpeLookMode,
                testTag = "switch_mcpe_look",
                onCheckedChange = { checked -> onUpdateSettings { it.copy(gamingMcpeLookMode = checked) } },
            )

            LabeledSlider(
                label = "Gaming Touchpad Sensitivity",
                valueLabel = "${String.format("%.1fx", settings.touchpadSensitivity)}",
                value = settings.touchpadSensitivity,
                range = 0.6f..3.5f,
                accentColor = colors.indigo,
                onValueChange = { v -> onUpdateSettings { it.copy(touchpadSensitivity = v) } },
            )

            LabeledSlider(
                label = "Gaming Touchpad Width Split",
                valueLabel = "${(settings.touchpadWidthFraction * 100).roundToInt()}%",
                value = settings.touchpadWidthFraction,
                range = 0.30f..0.55f,
                accentColor = colors.emerald,
                onValueChange = { v -> onUpdateSettings { it.copy(touchpadWidthFraction = v) } },
            )

            LabeledSlider(
                label = "Floating Bubble Size",
                valueLabel = "${settings.bubbleSizeDp} dp",
                value = settings.bubbleSizeDp.toFloat(),
                range = 36f..68f,
                accentColor = colors.indigo,
                onValueChange = { v -> onUpdateSettings { it.copy(bubbleSizeDp = v.roundToInt()) } },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicInset(
                        surfaceColor = colors.bg,
                        shadowLight = colors.shadowLight,
                        shadowDark = colors.shadowDark,
                        cornerRadius = 14.dp,
                        depth = 4.dp,
                        borderColor = colors.keyBorder.copy(alpha = 0.45f),
                    )
                    .padding(14.dp),
            ) {
                Text(
                    text = "Session Lifecycle: KeyDeck's floating keyboard is bound to the active app task (stopWithTask=true). Closing KeyDeck from Recents stops the overlay immediately.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .neumorphicRaised(
                        surfaceColor = colors.bg,
                        shadowLight = colors.shadowLight,
                        shadowDark = colors.shadowDark,
                        cornerRadius = 14.dp,
                        shadowOffset = 4.dp,
                        blurRadius = 8.dp,
                        borderColor = colors.keyBorder.copy(alpha = 0.45f),
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
                        tint = colors.indigo,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Restore Default Hotkeys",
                        color = colors.indigo,
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
private fun AppThemeModeSelector(
    selectedMode: AppThemeMode,
    onSelectMode: (AppThemeMode) -> Unit,
) {
    val colors = KeyDeckColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "App Appearance (Dark Mode)",
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppThemeMode.entries.forEach { mode ->
                val selected = selectedMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .then(
                            if (selected) {
                                Modifier.neumorphicInset(
                                    surfaceColor = colors.activeFill,
                                    shadowLight = colors.shadowLight,
                                    shadowDark = colors.shadowDark,
                                    cornerRadius = 12.dp,
                                    depth = 4.dp,
                                    borderColor = colors.keyBorder.copy(alpha = 0.45f),
                                )
                            } else {
                                Modifier.neumorphicRaised(
                                    surfaceColor = colors.bg,
                                    shadowLight = colors.shadowLight,
                                    shadowDark = colors.shadowDark,
                                    cornerRadius = 12.dp,
                                    shadowOffset = 3.dp,
                                    blurRadius = 6.dp,
                                    borderColor = colors.keyBorder.copy(alpha = 0.45f),
                                )
                            },
                        )
                        .clickable { onSelectMode(mode) }
                        .testTag("app_theme_mode_${mode.name.lowercase()}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = mode.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) colors.indigo else colors.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun NeuSheetPrimaryButton(
    label: String,
    testTag: String,
    onClick: () -> Unit,
) {
    val colors = KeyDeckColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .neumorphicRaised(
                surfaceColor = colors.accentFill,
                shadowLight = colors.shadowLight,
                shadowDark = colors.shadowDark,
                cornerRadius = 14.dp,
                shadowOffset = 4.dp,
                blurRadius = 8.dp,
                borderColor = null,
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
    showKeyBorder: Boolean,
    modifier: Modifier = Modifier,
) {
    val corner = (cornerDp - 2).coerceIn(4, 14).dp
    val borderMod = if (showKeyBorder) {
        Modifier.border(1.dp, palette.keyBorder.copy(alpha = 0.85f), RoundedCornerShape(corner))
    } else {
        Modifier
    }
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
                        )
                        .then(borderMod),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontFamily = BodyFontFamily,
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
                        )
                        .then(borderMod),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontFamily = BodyFontFamily,
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
    val colors = KeyDeckColors.current
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
                        shadowLight = colors.shadowLight,
                        shadowDark = colors.shadowDark,
                        cornerRadius = 17.dp,
                        shadowOffset = 2.dp,
                        blurRadius = 4.dp,
                    )
                    .border(
                        width = if (selected) 2.5.dp else 1.dp,
                        color = if (selected) colors.indigo else colors.shadowDark.copy(alpha = 0.5f),
                        shape = CircleShape,
                    )
                    .clickable { onSelect(hex) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected color",
                        tint = if (isLight) colors.indigo else Color.White,
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
    val colors = KeyDeckColors.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
            Text(
                valueLabel,
                fontFamily = BodyFontFamily,
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
                inactiveTrackColor = colors.shadowDark.copy(alpha = 0.45f),
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
    val colors = KeyDeckColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicRaised(
                surfaceColor = colors.bg,
                shadowLight = colors.shadowLight,
                shadowDark = colors.shadowDark,
                cornerRadius = 14.dp,
                shadowOffset = 3.dp,
                blurRadius = 6.dp,
                borderColor = colors.keyBorder.copy(alpha = 0.45f),
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accentFill,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.bg,
                uncheckedBorderColor = colors.keyBorder,
            ),
        )
    }
}
