package com.example.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyboard.AppThemeMode
import com.example.keyboard.CustomHotkey
import com.example.keyboard.DispatchedKeyRecord
import com.example.keyboard.KeyDeckSettings
import com.example.keyboard.KeyDispatcher
import com.example.keyboard.KeyboardDockAlignment
import com.example.keyboard.KeyboardLayoutMode
import com.example.keyboard.KeyboardWallpaper
import com.example.keyboard.KeyboardWallpaperBackdrop
import com.example.keyboard.PcKeyboardDeck
import com.example.keyboard.WallpaperSourceType
import com.example.ui.SystemReadinessState
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.KeyDeckColors
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised

@Composable
fun DeckHomeScreen(
    settings: KeyDeckSettings,
    hotkeys: List<CustomHotkey>,
    activeWallpaper: KeyboardWallpaper? = null,
    readiness: SystemReadinessState,
    isOverlayRunning: Boolean,
    isKeyboardExpanded: Boolean,
    lastDispatchedKey: DispatchedKeyRecord?,
    onToggleOverlay: (Boolean) -> Unit,
    onOpenFloatingDeck: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenImeSettingsOrPicker: () -> Unit,
    onOpenHotkeysTab: () -> Unit,
    onOpenThemesSheet: () -> Unit,
    onOpenWallpapersSheet: () -> Unit = onOpenThemesSheet,
    onOpenLayoutSheet: () -> Unit,
    onOpenSettingsSheet: () -> Unit,
    onChangeLayoutMode: (KeyboardLayoutMode) -> Unit,
    onToggleNumLock: () -> Unit,
    onToggleFunctionRow: () -> Unit,
    onUpdateSettings: ((KeyDeckSettings) -> KeyDeckSettings) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = KeyDeckColors.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val testInputFocusRequester = remember { FocusRequester() }
    var testFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var isTestInputFocused by remember { mutableStateOf(false) }
    var showInlinePreview by remember { mutableStateOf(true) }
    var measuredDeckHeightPx by remember { mutableIntStateOf(0) }
    var inlineMouseInfo by remember { mutableStateOf<String?>(null) }

    fun replaceSelectionWith(insertion: String) {
        val cur = testFieldValue
        val text = cur.text
        val start = cur.selection.min.coerceIn(0, text.length)
        val end = cur.selection.max.coerceIn(0, text.length)
        val newText = text.substring(0, start) + insertion + text.substring(end)
        val newCursor = start + insertion.length
        testFieldValue = TextFieldValue(newText, TextRange(newCursor))
    }

    DisposableEffect(Unit) {
        run {
            val target = object : KeyDispatcher.DirectKeyTarget {
                override fun onDirectKeyEvent(keyCode: Int, metaState: Int, charToCommit: Char?): Boolean {
                    val isCtrl = (metaState and KeyEvent.META_CTRL_ON) != 0
                    val isAlt = (metaState and KeyEvent.META_ALT_ON) != 0
                    val isMeta = (metaState and KeyEvent.META_META_ON) != 0

                    if (!isTestInputFocused && showInlinePreview) {
                        runCatching { testInputFocusRequester.requestFocus() }
                    }

                    if (isCtrl && !isAlt && !isMeta) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_A -> {
                                testFieldValue = testFieldValue.copy(selection = TextRange(0, testFieldValue.text.length))
                                return true
                            }
                            KeyEvent.KEYCODE_Z -> {
                                val text = testFieldValue.text
                                if (text.isNotEmpty()) {
                                    val trimmed = text.dropLast(1)
                                    testFieldValue = TextFieldValue(trimmed, TextRange(trimmed.length))
                                }
                                return true
                            }
                        }
                    }
                    val isShift = (metaState and KeyEvent.META_SHIFT_ON) != 0
                    if (!isCtrl && !isAlt && !isMeta) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DEL -> {
                                val cur = testFieldValue
                                val text = cur.text
                                val start = cur.selection.min.coerceIn(0, text.length)
                                val end = cur.selection.max.coerceIn(0, text.length)
                                if (start != end) {
                                    val newText = text.substring(0, start) + text.substring(end)
                                    testFieldValue = TextFieldValue(newText, TextRange(start))
                                } else if (start > 0) {
                                    val newText = text.substring(0, start - 1) + text.substring(start)
                                    testFieldValue = TextFieldValue(newText, TextRange(start - 1))
                                }
                                return true
                            }
                            KeyEvent.KEYCODE_FORWARD_DEL -> {
                                val cur = testFieldValue
                                val text = cur.text
                                val start = cur.selection.min.coerceIn(0, text.length)
                                val end = cur.selection.max.coerceIn(0, text.length)
                                if (start != end) {
                                    val newText = text.substring(0, start) + text.substring(end)
                                    testFieldValue = TextFieldValue(newText, TextRange(start))
                                } else if (end < text.length) {
                                    val newText = text.substring(0, end) + text.substring(end + 1)
                                    testFieldValue = TextFieldValue(newText, TextRange(start))
                                }
                                return true
                            }
                            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                replaceSelectionWith("\n")
                                return true
                            }
                            KeyEvent.KEYCODE_TAB -> {
                                replaceSelectionWith("    ")
                                return true
                            }
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                val pos = (testFieldValue.selection.end - 1).coerceAtLeast(0)
                                val newSel = if (isShift) TextRange(testFieldValue.selection.start, pos) else TextRange(pos)
                                testFieldValue = testFieldValue.copy(selection = newSel)
                                return true
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                val pos = (testFieldValue.selection.end + 1).coerceAtMost(testFieldValue.text.length)
                                val newSel = if (isShift) TextRange(testFieldValue.selection.start, pos) else TextRange(pos)
                                testFieldValue = testFieldValue.copy(selection = newSel)
                                return true
                            }
                            KeyEvent.KEYCODE_MOVE_HOME -> {
                                val newSel = if (isShift) TextRange(testFieldValue.selection.start, 0) else TextRange(0)
                                testFieldValue = testFieldValue.copy(selection = newSel)
                                return true
                            }
                            KeyEvent.KEYCODE_MOVE_END -> {
                                val endPos = testFieldValue.text.length
                                val newSel = if (isShift) TextRange(testFieldValue.selection.start, endPos) else TextRange(endPos)
                                testFieldValue = testFieldValue.copy(selection = newSel)
                                return true
                            }
                            else -> if (charToCommit != null) {
                                replaceSelectionWith(charToCommit.toString())
                                return true
                            }
                        }
                    }
                    return false
                }

                override fun onDirectKeyHold(keyCode: Int, metaState: Int, isDown: Boolean): Boolean {
                    inlineMouseInfo = if (isDown) "Holding ${KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")}" else null
                    return true
                }

                override fun getSurroundingChars(beforeLen: Int, afterLen: Int): Pair<String, String>? {
                    val cur = testFieldValue
                    if (!cur.selection.collapsed) return "" to ""
                    val cursor = cur.selection.start.coerceIn(0, cur.text.length)
                    val before = cur.text.substring(0, cursor).takeLast(beforeLen)
                    val after = cur.text.substring(cursor).take(afterLen)
                    return before to after
                }

                override fun replaceTailText(expectedTail: String, replacement: String): Boolean {
                    if (!isTestInputFocused && showInlinePreview) {
                        runCatching { testInputFocusRequester.requestFocus() }
                    }
                    val cur = testFieldValue
                    if (!cur.selection.collapsed) return false
                    val cursor = cur.selection.start.coerceIn(0, cur.text.length)
                    val before = cur.text.substring(0, cursor)
                    if (expectedTail.isNotEmpty() && !before.endsWith(expectedTail)) {
                        return false
                    }
                    val startReplace = cursor - expectedTail.length
                    val newText = cur.text.substring(0, startReplace) + replacement + cur.text.substring(cursor)
                    val newCursor = startReplace + replacement.length
                    testFieldValue = TextFieldValue(newText, TextRange(newCursor))
                    return true
                }
            }
            val mouseTarget = object : KeyDispatcher.DirectMouseTarget {
                override fun onMouseDelta(dx: Float, dy: Float) {
                    inlineMouseInfo = "Look Δ(${dx.toInt()}, ${dy.toInt()})"
                }

                override fun onMouseClick(isRightClick: Boolean) {
                    inlineMouseInfo = if (isRightClick) "Right Click (Use)" else "Left Click (Attack)"
                }
            }
            KeyDispatcher.setDirectTarget(target)
            KeyDispatcher.setDirectMouseTarget(mouseTarget)
        }
        onDispose {
            KeyDispatcher.setDirectTarget(null)
            KeyDispatcher.setDirectMouseTarget(null)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Soft UI Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .neumorphicRaised(
                                surfaceColor = colors.bg,
                                shadowLight = colors.shadowLight,
                                shadowDark = colors.shadowDark,
                                cornerRadius = 14.dp,
                                shadowOffset = 4.dp,
                                blurRadius = 8.dp,
                                borderColor = colors.keyBorder.copy(alpha = 0.45f),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "KeyDeck icon",
                            tint = colors.indigo,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Soft UI Keyboard",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "PC Deck • Numpad • Gaming",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Quick Dark Mode Switcher Pill
                    val themeIcon = when (settings.appThemeMode) {
                        AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                        AppThemeMode.LIGHT -> Icons.Default.LightMode
                        AppThemeMode.DARK -> Icons.Default.DarkMode
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .then(
                                if (colors.isDark) {
                                    Modifier.neumorphicInset(
                                        surfaceColor = colors.activeFill,
                                        shadowLight = colors.shadowLight,
                                        shadowDark = colors.shadowDark,
                                        cornerRadius = 18.dp,
                                        depth = 3.dp,
                                        borderColor = colors.keyBorder.copy(alpha = 0.55f),
                                    )
                                } else {
                                    Modifier.neumorphicRaised(
                                        surfaceColor = colors.bg,
                                        shadowLight = colors.shadowLight,
                                        shadowDark = colors.shadowDark,
                                        cornerRadius = 18.dp,
                                        shadowOffset = 3.dp,
                                        blurRadius = 6.dp,
                                        borderColor = colors.keyBorder.copy(alpha = 0.45f),
                                    )
                                },
                            )
                            .clickable {
                                val next = when (settings.appThemeMode) {
                                    AppThemeMode.SYSTEM -> if (colors.isDark) AppThemeMode.LIGHT else AppThemeMode.DARK
                                    AppThemeMode.DARK -> AppThemeMode.LIGHT
                                    AppThemeMode.LIGHT -> AppThemeMode.DARK
                                }
                                onUpdateSettings { it.copy(appThemeMode = next) }
                            }
                            .testTag("header_dark_mode_toggle"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = themeIcon,
                            contentDescription = "Theme: ${settings.appThemeMode.label}",
                            tint = if (colors.isDark) colors.amber else colors.indigo,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    // Neumorphic Status Pill
                    Box(
                        modifier = Modifier
                            .then(
                                if (isOverlayRunning) {
                                    Modifier.neumorphicInset(
                                        surfaceColor = colors.activeFill,
                                        shadowLight = colors.shadowLight,
                                        shadowDark = colors.shadowDark,
                                        cornerRadius = 20.dp,
                                        depth = 3.dp,
                                        borderColor = colors.keyBorder.copy(alpha = 0.45f),
                                    )
                                } else {
                                    Modifier.neumorphicRaised(
                                        surfaceColor = colors.bg,
                                        shadowLight = colors.shadowLight,
                                        shadowDark = colors.shadowDark,
                                        cornerRadius = 20.dp,
                                        shadowOffset = 3.dp,
                                        blurRadius = 6.dp,
                                        borderColor = colors.keyBorder.copy(alpha = 0.45f),
                                    )
                                },
                            )
                            .clickable { onToggleOverlay(!isOverlayRunning) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("header_status_pill"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOverlayRunning) colors.indigo else colors.textSecondary),
                            )
                            Text(
                                text = if (isOverlayRunning) "ACTIVE" else "OFF",
                                fontFamily = BodyFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverlayRunning) colors.indigo else colors.textSecondary,
                            )
                        }
                    }
                }
            }

            // Soft Neumorphic Hero Activation Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicRaised(
                        surfaceColor = colors.bg,
                        shadowLight = colors.shadowLight,
                        shadowDark = colors.shadowDark,
                        cornerRadius = 26.dp,
                        shadowOffset = 7.dp,
                        blurRadius = 14.dp,
                        borderColor = colors.keyBorder.copy(alpha = 0.45f),
                    )
                    .padding(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            Text(
                                text = if (isOverlayRunning) "Floating Keyboard Active" else "Turn On PC Keyboard",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isOverlayRunning) colors.indigo else colors.textPrimary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isOverlayRunning) {
                                    "Tap the floating icon anywhere — stays active even if KeyDeck app tab is closed."
                                } else {
                                    "Pops up a floating icon on any screen to open the PC deck at any time."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                            )
                        }

                        Switch(
                            checked = isOverlayRunning,
                            onCheckedChange = onToggleOverlay,
                            modifier = Modifier.testTag("hero_floating_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.accentFill,
                                uncheckedThumbColor = colors.textSecondary,
                                uncheckedTrackColor = colors.shadowDark.copy(alpha = 0.55f),
                                uncheckedBorderColor = Color.Transparent,
                            ),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Extruded primary action pill
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .then(
                                    if (isOverlayRunning) {
                                        Modifier.neumorphicInset(
                                            surfaceColor = colors.activeFill,
                                            shadowLight = colors.shadowLight,
                                            shadowDark = colors.shadowDark,
                                            cornerRadius = 16.dp,
                                            depth = 3.dp,
                                            borderColor = colors.keyBorder.copy(alpha = 0.45f),
                                        )
                                    } else {
                                        Modifier.neumorphicRaised(
                                            surfaceColor = colors.bg,
                                            shadowLight = colors.shadowLight,
                                            shadowDark = colors.shadowDark,
                                            cornerRadius = 16.dp,
                                            shadowOffset = 4.dp,
                                            blurRadius = 8.dp,
                                            borderColor = colors.keyBorder.copy(alpha = 0.45f),
                                        )
                                    },
                                )
                                .clickable {
                                    if (isOverlayRunning) onOpenFloatingDeck() else onToggleOverlay(true)
                                }
                                .padding(horizontal = 18.dp)
                                .testTag("hero_primary_action_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = when {
                                        !readiness.canDrawOverlays -> "Grant Overlay"
                                        isOverlayRunning && isKeyboardExpanded -> "Hide Overlay"
                                        isOverlayRunning -> "Open Keyboard"
                                        else -> "Turn On"
                                    },
                                    color = colors.indigo,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = colors.indigo,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }

                        // Primary IME Enable / Select pill
                        val imePillMod = if (readiness.isImeSelected) {
                            Modifier.neumorphicInset(
                                surfaceColor = colors.activeFill,
                                shadowLight = colors.shadowLight,
                                shadowDark = colors.shadowDark,
                                cornerRadius = 16.dp,
                                depth = 3.dp,
                                borderColor = colors.keyBorder.copy(alpha = 0.45f),
                            )
                        } else {
                            Modifier.neumorphicRaised(
                                surfaceColor = colors.bg,
                                shadowLight = colors.shadowLight,
                                shadowDark = colors.shadowDark,
                                cornerRadius = 16.dp,
                                shadowOffset = 4.dp,
                                blurRadius = 8.dp,
                                borderColor = colors.keyBorder.copy(alpha = 0.45f),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .then(imePillMod)
                                .clickable(onClick = onOpenImeSettingsOrPicker)
                                .padding(horizontal = 16.dp)
                                .testTag("hero_select_ime_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = when {
                                    !readiness.isImeEnabled -> "Enable IME"
                                    !readiness.isImeSelected -> "Select IME"
                                    else -> "KeyDeck IME ✓"
                                },
                                color = when {
                                    readiness.isImeSelected -> colors.emerald
                                    readiness.isImeEnabled -> colors.indigo
                                    else -> colors.textPrimary
                                },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            )
                        }
                    }

                    val floatComboText = remember(hotkeys) {
                        hotkeys.firstOrNull {
                            it.actionType == com.example.keyboard.HotkeyActionType.TOGGLE_FLOATING
                        }?.comboText() ?: "Ctrl+Alt+F"
                    }
                    // IME-First Status & Quick Shortcut Banner (Accessibility strictly optional)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphicInset(
                                surfaceColor = colors.activeFill.copy(alpha = 0.72f),
                                shadowLight = colors.shadowLight,
                                shadowDark = colors.shadowDark,
                                cornerRadius = 14.dp,
                                depth = 2.dp,
                                borderColor = colors.keyBorder.copy(alpha = 0.35f),
                            )
                            .clickable(
                                onClick = if (readiness.isImeSelected) onOpenHotkeysTab else onOpenImeSettingsOrPicker,
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("hero_ime_setup_banner"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = if (readiness.isImeSelected) Icons.Default.CheckCircle else Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = when {
                                readiness.isImeSelected -> colors.emerald
                                readiness.isImeEnabled -> colors.indigo
                                else -> colors.amber
                            },
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = when {
                                !readiness.isImeEnabled -> "Step 1: Enable KeyDeck IME to type anywhere (Accessibility optional)."
                                !readiness.isImeSelected -> "KeyDeck IME enabled! Step 2: Select KeyDeck as your active keyboard."
                                else -> "KeyDeck IME active. Press $floatComboText on IME anytime to pop open Floating Keyboard."
                            },
                            fontFamily = BodyFontFamily,
                            fontSize = 11.5.sp,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = when {
                                !readiness.isImeEnabled -> "Enable"
                                !readiness.isImeSelected -> "Select"
                                else -> "Edit"
                            },
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.indigo,
                        )
                    }
                }
            }

            // Permission & IME Readiness Pills (IME First, Accessibility Optional)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NeuReadinessChip(
                    title = "Overlay",
                    ready = readiness.canDrawOverlays,
                    statusText = if (readiness.canDrawOverlays) "Ready" else "Grant",
                    icon = Icons.Default.Layers,
                    testTag = "readiness_overlay",
                    modifier = Modifier.weight(1f),
                    onClick = onRequestOverlayPermission,
                )
                NeuReadinessChip(
                    title = "KeyDeck IME",
                    ready = readiness.isImeSelected,
                    statusText = when {
                        readiness.isImeSelected -> "Selected ✓"
                        readiness.isImeEnabled -> "Enabled (Select)"
                        else -> "Enable"
                    },
                    icon = Icons.Default.Keyboard,
                    testTag = "readiness_ime",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenImeSettingsOrPicker,
                )
                NeuReadinessChip(
                    title = "Accessibility",
                    ready = readiness.isAccessibilityEnabled,
                    statusText = if (readiness.isAccessibilityEnabled) "On" else "Optional",
                    isOptional = true,
                    icon = Icons.Default.AccessibilityNew,
                    testTag = "readiness_accessibility",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAccessibilitySettings,
                )
            }

            // Quick Actions 2x2 Neumorphic Cards
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
            )

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    NeuQuickActionCard(
                        icon = Icons.Default.Bolt,
                        iconTint = colors.indigo,
                        title = "Hotkeys",
                        subtitle = "${hotkeys.size} shortcuts",
                        testTag = "quick_action_hotkeys",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenHotkeysTab,
                    )
                    NeuQuickActionCard(
                        icon = Icons.Default.Palette,
                        iconTint = colors.amber,
                        title = "Themes",
                        subtitle = settings.themeId.displayName,
                        testTag = "quick_action_themes",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenThemesSheet,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    NeuQuickActionCard(
                        icon = Icons.Default.DashboardCustomize,
                        iconTint = colors.emerald,
                        title = "Size & Layout",
                        subtitle = "${settings.keyHeightDp}dp • ${(settings.keyboardWidthFraction * 100).toInt()}%",
                        testTag = "quick_action_layout",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenLayoutSheet,
                    )
                    NeuQuickActionCard(
                        icon = Icons.Default.Settings,
                        iconTint = colors.coral,
                        title = "Settings",
                        subtitle = "${settings.appThemeMode.label} • ${settings.bubbleSizeDp}dp",
                        testTag = "quick_action_settings",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSettingsSheet,
                    )
                }

                // Keyboard Wallpaper Studio Quick Access Card
                val homePalette = settings.resolvePalette(colors.isDark)
                val hasActiveWp = activeWallpaper != null && activeWallpaper.sourceType != WallpaperSourceType.NONE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphicRaised(
                            surfaceColor = colors.bg,
                            shadowLight = colors.shadowLight,
                            shadowDark = colors.shadowDark,
                            cornerRadius = 18.dp,
                            shadowOffset = 5.dp,
                            blurRadius = 10.dp,
                            borderColor = colors.keyBorder.copy(alpha = 0.45f),
                        )
                        .clickable(onClick = onOpenWallpapersSheet)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("quick_action_wallpapers"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 52.dp, height = 38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(homePalette.deckBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (hasActiveWp) {
                                KeyboardWallpaperBackdrop(
                                    wallpaper = activeWallpaper,
                                    palette = homePalette,
                                    modifier = Modifier.matchParentSize(),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Wallpaper,
                                    contentDescription = "Keyboard Wallpaper Studio",
                                    tint = colors.indigo,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Keyboard Wallpaper Studio",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = if (hasActiveWp) {
                                    "${activeWallpaper!!.name} • ${(activeWallpaper.lightness * 100).toInt()}% Light • Tap to edit"
                                } else {
                                    "Pick gallery image, 90% dark stealth, patterns & filters"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = colors.indigo,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Try Your Keyboard Section with Recessed Neumorphic Well
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Try Your Keyboard",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                )
                Box(
                    modifier = Modifier
                        .neumorphicRaised(
                            surfaceColor = colors.bg,
                            shadowLight = colors.shadowLight,
                            shadowDark = colors.shadowDark,
                            cornerRadius = 12.dp,
                            shadowOffset = 3.dp,
                            blurRadius = 6.dp,
                            borderColor = colors.keyBorder.copy(alpha = 0.45f),
                        )
                        .clickable { showInlinePreview = !showInlinePreview }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("toggle_inline_preview"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = if (showInlinePreview) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = colors.indigo,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = if (showInlinePreview) "Hide Deck" else "Show Deck",
                            fontSize = 12.sp,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // Concave Inset Text Input Well — always sends real Android IME / keyboard focus signal on tap
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicInset(
                        surfaceColor = colors.bg,
                        shadowLight = colors.shadowLight,
                        shadowDark = colors.shadowDark,
                        cornerRadius = 16.dp,
                        depth = 4.dp,
                        borderColor = if (isTestInputFocused) colors.indigo.copy(alpha = 0.75f) else colors.keyBorder.copy(alpha = 0.45f),
                    )
                    .clickable {
                        testInputFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        BasicTextField(
                            value = testFieldValue,
                            onValueChange = { testFieldValue = it },
                            textStyle = TextStyle(
                                fontFamily = BodyFontFamily,
                                fontSize = 15.sp,
                                color = colors.textPrimary,
                            ),
                            cursorBrush = SolidColor(colors.indigo),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(testInputFocusRequester)
                                .onFocusChanged { isTestInputFocused = it.isFocused }
                                .testTag("try_keyboard_input"),
                            decorationBox = { innerTextField ->
                                if (testFieldValue.text.isEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EditNote,
                                            contentDescription = null,
                                            tint = colors.indigo,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            text = "Tap here to open keyboard & test typing...",
                                            color = colors.textSecondary,
                                            fontSize = 14.sp,
                                        )
                                    }
                                }
                                innerTextField()
                            },
                        )
                    }
                    if (testFieldValue.text.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable {
                                    testFieldValue = TextFieldValue("")
                                    com.example.data.KeyDeckRuntimeState.notifyInputSessionReset()
                                }
                                .testTag("try_keyboard_clear"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear test text",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            // Live Dispatched Key & Gaming Mouse Telemetry Chip
            if (lastDispatchedKey != null || inlineMouseInfo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphicInset(
                            surfaceColor = colors.activeFill,
                            shadowLight = colors.shadowLight,
                            shadowDark = colors.shadowDark,
                            cornerRadius = 12.dp,
                            depth = 3.dp,
                            borderColor = colors.keyBorder.copy(alpha = 0.45f),
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = inlineMouseInfo ?: "Key: ${lastDispatchedKey?.comboLabel ?: ""}",
                        fontFamily = BodyFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.indigo,
                    )
                    Text(
                        text = lastDispatchedKey?.detail ?: "Touchpad Ready",
                        fontFamily = BodyFontFamily,
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Extruded Neumorphic Keyboard Frame (or exact-height blank space when hidden so Floating Keyboard fits cleanly below input)
        val palette = settings.resolvePalette(colors.isDark)
        val dockAlign = when (settings.dockAlignment) {
            KeyboardDockAlignment.LEFT -> Alignment.CenterStart
            KeyboardDockAlignment.CENTER -> Alignment.Center
            KeyboardDockAlignment.RIGHT -> Alignment.CenterEnd
        }
        val outerCorner = (settings.keyCornerRadiusDp + 8).coerceIn(12, 24).dp
        val borderCol = if (settings.isKeyBorderVisible()) palette.keyBorder.copy(alpha = 0.85f) else null

        if (showInlinePreview) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 14.dp, start = 6.dp, end = 6.dp),
                contentAlignment = dockAlign,
            ) {
                PcKeyboardDeck(
                    settings = settings,
                    routeLabel = "In-App",
                    isOverlay = false,
                    hotkeys = hotkeys,
                    activeWallpaper = activeWallpaper,
                    onHotkeyDispatch = { hk ->
                        KeyDispatcher.dispatchHotkey(
                            context = context,
                            hotkey = hk,
                            hapticEnabled = settings.hapticFeedback,
                        )
                    },
                    onKeyDispatch = { keyCode, metaState, charToCommit, comboLabel ->
                        KeyDispatcher.dispatchKey(
                            context = context,
                            keyCode = keyCode,
                            metaState = metaState,
                            charToCommit = charToCommit,
                            comboLabel = comboLabel,
                            hapticEnabled = settings.hapticFeedback,
                            hotkeys = hotkeys,
                        )
                    },
                    onRepeatKeyDispatch = { keyCode, metaState, charToCommit, comboLabel ->
                        KeyDispatcher.dispatchKey(
                            context = context,
                            keyCode = keyCode,
                            metaState = metaState,
                            charToCommit = charToCommit,
                            comboLabel = comboLabel,
                            hapticEnabled = false,
                            hotkeys = hotkeys,
                        )
                    },
                    onKeyHoldDispatch = { keyCode, metaState, isDown, comboLabel ->
                        KeyDispatcher.dispatchKeyHold(
                            context = context,
                            keyCode = keyCode,
                            metaState = metaState,
                            isDown = isDown,
                            comboLabel = comboLabel,
                            hapticEnabled = settings.hapticFeedback,
                        )
                    },
                    onMouseMove = { dx, dy ->
                        KeyDispatcher.dispatchMouseMove(
                            dx = dx,
                            dy = dy,
                            sensitivity = settings.touchpadSensitivity,
                            mcpeLookMode = settings.gamingMcpeLookMode,
                        )
                    },
                    onMouseClick = { isRight ->
                        KeyDispatcher.dispatchMouseClick(
                            context = context,
                            isRightClick = isRight,
                            mcpeLookMode = settings.gamingMcpeLookMode,
                            hapticEnabled = settings.hapticFeedback,
                        )
                    },
                    onChangeLayoutMode = onChangeLayoutMode,
                    onToggleNumLock = onToggleNumLock,
                    onToggleFunctionRow = onToggleFunctionRow,
                    onUpdateSettings = onUpdateSettings,
                    modifier = Modifier
                        .fillMaxWidth(settings.keyboardWidthFraction.coerceIn(0.60f, 1.0f))
                        .onSizeChanged { size ->
                            if (size.height > 0) {
                                measuredDeckHeightPx = size.height
                            }
                        }
                        .neumorphicRaised(
                            surfaceColor = palette.deckBg,
                            shadowLight = palette.shadowLight,
                            shadowDark = palette.shadowDark,
                            cornerRadius = outerCorner,
                            shadowOffset = 5.dp,
                            blurRadius = 10.dp,
                            borderColor = borderCol,
                        ),
                )
            }
        } else {
            val suggestionBarExtra = if (settings.layoutMode == KeyboardLayoutMode.SIMPLE && settings.simpleSuggestionsEnabled) 36 else 0
            val fallbackHeightDp = (settings.keyHeightDp * 5 + settings.keyVerticalGapDp * 4 + settings.deckPaddingDp * 2 + 42 + suggestionBarExtra).dp
            val exactReservedHeightDp = if (measuredDeckHeightPx > 0) {
                with(density) { measuredDeckHeightPx.toDp() }
            } else {
                fallbackHeightDp
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 14.dp, start = 6.dp, end = 6.dp),
                contentAlignment = dockAlign,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(settings.keyboardWidthFraction.coerceIn(0.60f, 1.0f))
                        .height(exactReservedHeightDp)
                        .clip(RoundedCornerShape(outerCorner))
                        .testTag("hidden_deck_placeholder"),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val r = outerCorner.toPx()
                        drawRoundRect(
                            color = colors.shadowDark.copy(alpha = 0.42f),
                            cornerRadius = CornerRadius(r, r),
                            style = Stroke(
                                width = 1.4.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f),
                            ),
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = colors.textSecondary.copy(alpha = 0.55f),
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = "Deck Hidden • Space Reserved for Floating Keyboard",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary.copy(alpha = 0.72f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NeuQuickActionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = KeyDeckColors.current
    Box(
        modifier = modifier
            .height(108.dp)
            .neumorphicRaised(
                surfaceColor = colors.bg,
                shadowLight = colors.shadowLight,
                shadowDark = colors.shadowDark,
                cornerRadius = 20.dp,
                shadowOffset = 5.dp,
                blurRadius = 10.dp,
                borderColor = colors.keyBorder.copy(alpha = 0.45f),
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .neumorphicInset(
                        surfaceColor = colors.bg,
                        shadowLight = colors.shadowLight,
                        shadowDark = colors.shadowDark,
                        cornerRadius = 12.dp,
                        depth = 3.dp,
                        borderColor = colors.keyBorder.copy(alpha = 0.45f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NeuReadinessChip(
    title: String,
    ready: Boolean,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier,
    statusText: String? = null,
    isOptional: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = KeyDeckColors.current
    val mod = if (ready) {
        Modifier.neumorphicInset(
            surfaceColor = colors.activeFill,
            shadowLight = colors.shadowLight,
            shadowDark = colors.shadowDark,
            cornerRadius = 14.dp,
            depth = 3.dp,
            borderColor = colors.keyBorder.copy(alpha = 0.45f),
        )
    } else {
        Modifier.neumorphicRaised(
            surfaceColor = colors.bg,
            shadowLight = colors.shadowLight,
            shadowDark = colors.shadowDark,
            cornerRadius = 14.dp,
            shadowOffset = 3.dp,
            blurRadius = 6.dp,
            borderColor = colors.keyBorder.copy(alpha = 0.45f),
        )
    }
    val statusColor = when {
        ready -> colors.indigo
        isOptional -> colors.textSecondary
        else -> colors.amber
    }
    Box(
        modifier = modifier
            .height(54.dp)
            .then(mod)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (ready) Icons.Default.CheckCircle else icon,
                contentDescription = title,
                tint = statusColor,
                modifier = Modifier.size(16.dp),
            )
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = statusText ?: if (ready) "Ready" else "Setup",
                    fontSize = 10.sp,
                    color = statusColor,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
