package com.example.ui.screens

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.awaitCancellation
import com.example.keyboard.CustomHotkey
import com.example.keyboard.DispatchedKeyRecord
import com.example.keyboard.KeyDeckSettings
import com.example.keyboard.KeyDispatcher
import com.example.keyboard.KeyboardDockAlignment
import com.example.keyboard.KeyboardLayoutMode
import com.example.keyboard.PcKeyboardDeck
import com.example.ui.SystemReadinessState
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.NeuActiveFill
import com.example.ui.theme.NeuAmber
import com.example.ui.theme.NeuBg
import com.example.ui.theme.NeuEmerald
import com.example.ui.theme.NeuIndigo
import com.example.ui.theme.NeuShadowDark
import com.example.ui.theme.NeuShadowLight
import com.example.ui.theme.NeuTextPrimary
import com.example.ui.theme.NeuTextSecondary
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised

@Composable
fun DeckHomeScreen(
    settings: KeyDeckSettings,
    hotkeys: List<CustomHotkey>,
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
    onOpenLayoutSheet: () -> Unit,
    onOpenSettingsSheet: () -> Unit,
    onChangeLayoutMode: (KeyboardLayoutMode) -> Unit,
    onToggleNumLock: () -> Unit,
    onToggleFunctionRow: () -> Unit,
    onUpdateSettings: ((KeyDeckSettings) -> KeyDeckSettings) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var testText by remember { mutableStateOf("") }
    var showInlinePreview by remember { mutableStateOf(true) }
    var inlineMouseInfo by remember { mutableStateOf<String?>(null) }

    DisposableEffect(showInlinePreview) {
        if (showInlinePreview) {
            val target = object : KeyDispatcher.DirectKeyTarget {
                override fun onDirectKeyEvent(keyCode: Int, metaState: Int, charToCommit: Char?): Boolean {
                    val isCtrl = (metaState and KeyEvent.META_CTRL_ON) != 0
                    val isAlt = (metaState and KeyEvent.META_ALT_ON) != 0
                    val isMeta = (metaState and KeyEvent.META_META_ON) != 0

                    if (isCtrl && keyCode == KeyEvent.KEYCODE_A) {
                        return true
                    }
                    if (isCtrl && keyCode == KeyEvent.KEYCODE_Z) {
                        if (testText.isNotEmpty()) testText = testText.dropLast(1)
                        return true
                    }
                    if (!isCtrl && !isAlt && !isMeta) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DEL -> {
                                if (testText.isNotEmpty()) testText = testText.dropLast(1)
                                return true
                            }
                            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                testText += "\n"
                                return true
                            }
                            KeyEvent.KEYCODE_TAB -> {
                                testText += "    "
                                return true
                            }
                            else -> if (charToCommit != null) {
                                testText += charToCommit
                                return true
                            }
                        }
                    }
                    return true
                }

                override fun onDirectKeyHold(keyCode: Int, metaState: Int, isDown: Boolean): Boolean {
                    inlineMouseInfo = if (isDown) "Holding ${KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")}" else null
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
            .background(NeuBg)
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
                                surfaceColor = NeuBg,
                                shadowLight = NeuShadowLight,
                                shadowDark = NeuShadowDark,
                                cornerRadius = 14.dp,
                                shadowOffset = 4.dp,
                                blurRadius = 8.dp,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "KeyDeck icon",
                            tint = NeuIndigo,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Soft UI Keyboard",
                            style = MaterialTheme.typography.titleLarge,
                            color = NeuTextPrimary,
                        )
                        Text(
                            text = "PC Deck • Numpad • Gaming",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeuTextSecondary,
                        )
                    }
                }

                // Neumorphic Status Pill
                Box(
                    modifier = Modifier
                        .then(
                            if (isOverlayRunning) {
                                Modifier.neumorphicInset(
                                    surfaceColor = NeuActiveFill,
                                    shadowLight = NeuShadowLight,
                                    shadowDark = NeuShadowDark,
                                    cornerRadius = 20.dp,
                                    depth = 3.dp,
                                )
                            } else {
                                Modifier.neumorphicRaised(
                                    surfaceColor = NeuBg,
                                    shadowLight = NeuShadowLight,
                                    shadowDark = NeuShadowDark,
                                    cornerRadius = 20.dp,
                                    shadowOffset = 3.dp,
                                    blurRadius = 6.dp,
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
                                .background(if (isOverlayRunning) NeuIndigo else NeuTextSecondary),
                        )
                        Text(
                            text = if (isOverlayRunning) "ACTIVE" else "OFF",
                            fontFamily = BodyFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverlayRunning) NeuIndigo else NeuTextSecondary,
                        )
                    }
                }
            }

            // Soft Neumorphic Hero Activation Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicRaised(
                        surfaceColor = NeuBg,
                        shadowLight = NeuShadowLight,
                        shadowDark = NeuShadowDark,
                        cornerRadius = 26.dp,
                        shadowOffset = 7.dp,
                        blurRadius = 14.dp,
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
                                color = if (isOverlayRunning) NeuIndigo else NeuTextPrimary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isOverlayRunning) {
                                    "Tap the floating icon anywhere — even on pages without text inputs. Stops if KeyDeck tab is closed."
                                } else {
                                    "Pops up a floating icon on any screen to open the PC deck at any time."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = NeuTextSecondary,
                            )
                        }

                        Switch(
                            checked = isOverlayRunning,
                            onCheckedChange = onToggleOverlay,
                            modifier = Modifier.testTag("hero_floating_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeuIndigo,
                                uncheckedThumbColor = NeuTextSecondary,
                                uncheckedTrackColor = NeuShadowDark.copy(alpha = 0.45f),
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
                                            surfaceColor = NeuActiveFill,
                                            shadowLight = NeuShadowLight,
                                            shadowDark = NeuShadowDark,
                                            cornerRadius = 16.dp,
                                            depth = 3.dp,
                                        )
                                    } else {
                                        Modifier.neumorphicRaised(
                                            surfaceColor = NeuBg,
                                            shadowLight = NeuShadowLight,
                                            shadowDark = NeuShadowDark,
                                            cornerRadius = 16.dp,
                                            shadowOffset = 4.dp,
                                            blurRadius = 8.dp,
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
                                    color = NeuIndigo,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = NeuIndigo,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }

                        // Select IME pill
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .neumorphicRaised(
                                    surfaceColor = NeuBg,
                                    shadowLight = NeuShadowLight,
                                    shadowDark = NeuShadowDark,
                                    cornerRadius = 16.dp,
                                    shadowOffset = 4.dp,
                                    blurRadius = 8.dp,
                                )
                                .clickable(onClick = onOpenImeSettingsOrPicker)
                                .padding(horizontal = 16.dp)
                                .testTag("hero_select_ime_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (readiness.isImeSelected) "IME Active" else "Select IME",
                                color = if (readiness.isImeSelected) NeuEmerald else NeuTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            // Permission & Accessibility Readiness Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NeuReadinessChip(
                    title = "Overlay",
                    ready = readiness.canDrawOverlays,
                    icon = Icons.Default.Layers,
                    testTag = "readiness_overlay",
                    modifier = Modifier.weight(1f),
                    onClick = onRequestOverlayPermission,
                )
                NeuReadinessChip(
                    title = "Accessibility",
                    ready = readiness.isAccessibilityEnabled,
                    icon = Icons.Default.AccessibilityNew,
                    testTag = "readiness_accessibility",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAccessibilitySettings,
                )
                NeuReadinessChip(
                    title = "System IME",
                    ready = readiness.isImeEnabled,
                    icon = Icons.Default.Keyboard,
                    testTag = "readiness_ime",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenImeSettingsOrPicker,
                )
            }

            // Quick Actions 2x2 Neumorphic Cards
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                color = NeuTextPrimary,
            )

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    NeuQuickActionCard(
                        icon = Icons.Default.Bolt,
                        iconTint = NeuIndigo,
                        title = "Hotkeys",
                        subtitle = "${hotkeys.size} shortcuts",
                        testTag = "quick_action_hotkeys",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenHotkeysTab,
                    )
                    NeuQuickActionCard(
                        icon = Icons.Default.Palette,
                        iconTint = Color(0xFFD97706),
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
                        iconTint = NeuEmerald,
                        title = "Size & Layout",
                        subtitle = "${settings.keyHeightDp}dp • ${(settings.keyboardWidthFraction * 100).toInt()}%",
                        testTag = "quick_action_layout",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenLayoutSheet,
                    )
                    NeuQuickActionCard(
                        icon = Icons.Default.Settings,
                        iconTint = Color(0xFFE11D48),
                        title = "Settings",
                        subtitle = "Bubble ${settings.bubbleSizeDp}dp",
                        testTag = "quick_action_settings",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSettingsSheet,
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
                    color = NeuTextPrimary,
                )
                Box(
                    modifier = Modifier
                        .neumorphicRaised(
                            surfaceColor = NeuBg,
                            shadowLight = NeuShadowLight,
                            shadowDark = NeuShadowDark,
                            cornerRadius = 12.dp,
                            shadowOffset = 3.dp,
                            blurRadius = 6.dp,
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
                            tint = NeuIndigo,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = if (showInlinePreview) "Hide Deck" else "Show Deck",
                            fontSize = 12.sp,
                            color = NeuTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // Concave Inset Text Input Well (suppresses system/IME popup when KeyDeck is active)
            @OptIn(ExperimentalComposeUiApi::class)
            val suppressTestWellIme = showInlinePreview || isOverlayRunning || settings.suppressAutoImeOnInput
            @OptIn(ExperimentalComposeUiApi::class)
            val noSoftKeyboardInterceptor = remember {
                PlatformTextInputInterceptor { _, _ ->
                    awaitCancellation()
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicInset(
                        surfaceColor = NeuBg,
                        shadowLight = NeuShadowLight,
                        shadowDark = NeuShadowDark,
                        cornerRadius = 16.dp,
                        depth = 4.dp,
                    )
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
                val inputContent: @Composable () -> Unit = {
                    BasicTextField(
                        value = testText,
                        onValueChange = { testText = it },
                        textStyle = TextStyle(
                            fontFamily = BodyFontFamily,
                            fontSize = 15.sp,
                            color = NeuTextPrimary,
                        ),
                        cursorBrush = SolidColor(NeuIndigo),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("try_keyboard_input"),
                        decorationBox = { innerTextField ->
                            if (testText.isEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = NeuIndigo,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = "Tap here to test typing...",
                                        color = NeuTextSecondary,
                                        fontSize = 14.sp,
                                    )
                                }
                            }
                            innerTextField()
                        },
                    )
                }
                if (suppressTestWellIme) {
                    @OptIn(ExperimentalComposeUiApi::class)
                    InterceptPlatformTextInput(interceptor = noSoftKeyboardInterceptor) {
                        inputContent()
                    }
                } else {
                    inputContent()
                }
            }

            // Live Dispatched Key & Gaming Mouse Telemetry Chip
            if (lastDispatchedKey != null || inlineMouseInfo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphicInset(
                            surfaceColor = NeuActiveFill,
                            shadowLight = NeuShadowLight,
                            shadowDark = NeuShadowDark,
                            cornerRadius = 12.dp,
                            depth = 3.dp,
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
                        color = NeuIndigo,
                    )
                    Text(
                        text = lastDispatchedKey?.detail ?: "Touchpad Ready",
                        fontFamily = BodyFontFamily,
                        fontSize = 11.sp,
                        color = NeuTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Extruded Neumorphic Keyboard Frame
        AnimatedVisibility(visible = showInlinePreview) {
            val palette = settings.resolvePalette()
            val dockAlign = when (settings.dockAlignment) {
                KeyboardDockAlignment.LEFT -> Alignment.CenterStart
                KeyboardDockAlignment.CENTER -> Alignment.Center
                KeyboardDockAlignment.RIGHT -> Alignment.CenterEnd
            }
            val outerCorner = (settings.keyCornerRadiusDp + 8).coerceIn(12, 24).dp
            val borderCol = if (settings.showKeyBorder) palette.keyBorder.copy(alpha = 0.85f) else null
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 14.dp, start = 6.dp, end = 6.dp),
                contentAlignment = dockAlign,
            ) {
                PcKeyboardDeck(
                    settings = settings,
                    hotkeys = hotkeys,
                    routeLabel = "In-App",
                    isOverlay = false,
                    onKeyDispatch = { keyCode, metaState, charToCommit, comboLabel ->
                        KeyDispatcher.dispatchKey(
                            context = context,
                            keyCode = keyCode,
                            metaState = metaState,
                            charToCommit = charToCommit,
                            comboLabel = comboLabel,
                            hapticEnabled = settings.hapticFeedback,
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
                    onHotkeyDispatch = { hotkey ->
                        KeyDispatcher.dispatchHotkey(
                            context = context,
                            hotkey = hotkey,
                            hapticEnabled = settings.hapticFeedback,
                        )
                    },
                    onChangeLayoutMode = onChangeLayoutMode,
                    onToggleNumLock = onToggleNumLock,
                    onToggleFunctionRow = onToggleFunctionRow,
                    onUpdateSettings = onUpdateSettings,
                    modifier = Modifier
                        .fillMaxWidth(settings.keyboardWidthFraction.coerceIn(0.60f, 1.0f))
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
    Box(
        modifier = modifier
            .height(108.dp)
            .neumorphicRaised(
                surfaceColor = NeuBg,
                shadowLight = NeuShadowLight,
                shadowDark = NeuShadowDark,
                cornerRadius = 20.dp,
                shadowOffset = 5.dp,
                blurRadius = 10.dp,
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
                        surfaceColor = NeuBg,
                        shadowLight = NeuShadowLight,
                        shadowDark = NeuShadowDark,
                        cornerRadius = 12.dp,
                        depth = 3.dp,
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
                color = NeuTextPrimary,
                maxLines = 1,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = NeuTextSecondary,
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
    onClick: () -> Unit,
) {
    val mod = if (ready) {
        Modifier.neumorphicInset(
            surfaceColor = NeuActiveFill,
            shadowLight = NeuShadowLight,
            shadowDark = NeuShadowDark,
            cornerRadius = 14.dp,
            depth = 3.dp,
        )
    } else {
        Modifier.neumorphicRaised(
            surfaceColor = NeuBg,
            shadowLight = NeuShadowLight,
            shadowDark = NeuShadowDark,
            cornerRadius = 14.dp,
            shadowOffset = 3.dp,
            blurRadius = 6.dp,
        )
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
                tint = if (ready) NeuIndigo else NeuAmber,
                modifier = Modifier.size(16.dp),
            )
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeuTextPrimary,
                    maxLines = 1,
                )
                Text(
                    text = if (ready) "Ready" else "Setup",
                    fontSize = 10.sp,
                    color = if (ready) NeuIndigo else NeuAmber,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}
