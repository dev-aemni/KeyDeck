package com.example.keyboard

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.KeyboardTab
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised
import com.example.ui.theme.rgbAnimatedBorder

@Composable
fun PcKeyboardDeck(
    settings: KeyDeckSettings,
    hotkeys: List<CustomHotkey>,
    routeLabel: String,
    isOverlay: Boolean,
    onKeyDispatch: (keyCode: Int, metaState: Int, charToCommit: Char?, comboLabel: String) -> Unit,
    onHotkeyDispatch: (CustomHotkey) -> Unit,
    onChangeLayoutMode: (KeyboardLayoutMode) -> Unit,
    onToggleNumLock: () -> Unit,
    onToggleFunctionRow: () -> Unit,
    onUpdateSettings: ((KeyDeckSettings) -> KeyDeckSettings) -> Unit = {},
    onKeyHoldDispatch: ((keyCode: Int, metaState: Int, isDown: Boolean, comboLabel: String) -> Unit)? = null,
    onMouseMove: ((dx: Float, dy: Float) -> Unit)? = null,
    onMouseClick: ((isRightClick: Boolean) -> Unit)? = null,
    onOverlayDrag: ((dx: Float, dy: Float) -> Unit)? = null,
    onResizeDragDelta: ((dx: Float, dy: Float) -> Unit)? = null,
    onMinimize: (() -> Unit)? = null,
    onCloseOverlay: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = remember(settings) { settings.resolvePalette() }
    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }
    var shiftActive by remember { mutableStateOf(false) }
    var capsActive by remember { mutableStateOf(false) }
    var metaActive by remember { mutableStateOf(false) }
    var fnActive by remember { mutableStateOf(false) }

    var showResizeStrip by remember { mutableStateOf(false) }
    var showQuickSettingsPopup by remember { mutableStateOf(false) }

    val rgbActive = settings.isRgbActiveForCurrentMode()
    val rgbPhaseDeg by if (rgbActive) {
        val transition = rememberInfiniteTransition(label = "rgb_border_transition")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "rgb_phase_deg",
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val keyHeight = settings.keyHeightDp.coerceIn(26, 54).dp
    val hGap = settings.keyHorizontalGapDp.coerceIn(1, 6).dp
    val vGap = settings.keyVerticalGapDp.coerceIn(1, 7).dp
    val deckPadding = settings.deckPaddingDp.coerceIn(1, 10).dp
    val keyCorner = settings.keyCornerRadiusDp.coerceIn(4, 16).dp
    val fontScale = settings.fontScale.coerceIn(0.75f, 1.25f)

    val effectiveUpperCase = shiftActive xor capsActive

    fun currentMetaState(): Int {
        var flags = 0
        if (ctrlActive) flags = flags or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (altActive) flags = flags or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (shiftActive) flags = flags or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        if (capsActive) flags = flags or KeyEvent.META_CAPS_LOCK_ON
        if (metaActive) flags = flags or KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
        if (fnActive) flags = flags or KeyEvent.META_FUNCTION_ON
        return flags
    }

    fun formatComboLabel(baseLabel: String): String {
        val parts = mutableListOf<String>()
        if (ctrlActive) parts += "Ctrl"
        if (altActive) parts += "Alt"
        if (shiftActive) parts += "Shift"
        if (metaActive) parts += "Win"
        if (fnActive) parts += "Fn"
        parts += baseLabel
        return parts.joinToString("+")
    }

    fun clearStickyModifiersIfNeeded() {
        if (settings.stickyModifiers) {
            ctrlActive = false
            altActive = false
            shiftActive = false
            metaActive = false
        }
    }

    fun handleKeyTap(spec: KeySpec, isFromNumpad: Boolean = false) {
        when (spec.modifierType) {
            ModifierKey.CTRL -> {
                ctrlActive = !ctrlActive
                return
            }
            ModifierKey.ALT -> {
                altActive = !altActive
                return
            }
            ModifierKey.SHIFT -> {
                shiftActive = !shiftActive
                return
            }
            ModifierKey.CAPS -> {
                capsActive = !capsActive
                return
            }
            ModifierKey.META -> {
                metaActive = !metaActive
                return
            }
            ModifierKey.FN -> {
                fnActive = !fnActive
                return
            }
            null -> Unit
        }

        if (spec.id == "num_toggle_top" || spec.id == "num_toggle") {
            val nextMode = when (settings.layoutMode) {
                KeyboardLayoutMode.PC_WITH_NUMPAD -> KeyboardLayoutMode.NUMPAD_ONLY
                KeyboardLayoutMode.NUMPAD_ONLY -> KeyboardLayoutMode.PC_STANDARD
                else -> KeyboardLayoutMode.PC_WITH_NUMPAD
            }
            onChangeLayoutMode(nextMode)
            return
        }

        if (spec.id == "num_lock") {
            onToggleNumLock()
            return
        }

        val effectiveKeyCode = when {
            isFromNumpad && !settings.numLockOn && spec.fnKeyCode != null -> spec.fnKeyCode
            fnActive && spec.fnKeyCode != null -> spec.fnKeyCode
            else -> spec.keyCode
        }

        val isLetter = spec.charPrimary?.isLetter() == true
        val useShiftedChar = if (isLetter) effectiveUpperCase else shiftActive
        val effectiveChar: Char? = when {
            isFromNumpad && !settings.numLockOn -> null
            fnActive && spec.fnKeyCode != null -> null
            useShiftedChar && spec.charShift != null -> spec.charShift
            else -> spec.charPrimary
        }

        val displayLabel = when {
            isFromNumpad && !settings.numLockOn && spec.fnLabel != null -> spec.fnLabel
            fnActive && spec.fnLabel != null -> spec.fnLabel
            useShiftedChar && spec.shiftLabel != null -> spec.shiftLabel
            else -> spec.label
        }

        onKeyDispatch(
            effectiveKeyCode,
            currentMetaState(),
            effectiveChar,
            formatComboLabel(displayLabel),
        )
        clearStickyModifiersIfNeeded()
    }

    val isTightDeck = settings.keyHeightDp <= 32
    val borderCol = if (settings.showKeyBorder) palette.keyBorder.copy(alpha = 0.85f) else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.deckBg.copy(alpha = if (isOverlay) settings.overlayOpacity else 1f))
            .padding(horizontal = deckPadding, vertical = deckPadding),
        verticalArrangement = Arrangement.spacedBy(vGap),
    ) {
        // Uncluttered Top Bar: Drag Handle + Essential Mode Pills + Resize + Quick Settings Gear (+ Minimize/Close in overlay)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isOverlay && onOverlayDrag != null) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onOverlayDrag(dragAmount.x, dragAmount.y)
                            }
                        }
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 2.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (isOverlay) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag floating keyboard",
                        tint = palette.secondaryText,
                        modifier = Modifier.size(16.dp),
                    )
                }
                if (settings.showModeStrip) {
                    KeyboardLayoutMode.entries.forEach { mode ->
                        DeckModeChip(
                            label = mode.shortLabel,
                            selected = settings.layoutMode == mode,
                            palette = palette,
                            borderColor = borderCol,
                            compact = isTightDeck,
                            testTag = "deck_mode_${mode.name.lowercase()}",
                            onClick = { onChangeLayoutMode(mode) },
                        )
                    }
                } else {
                    Text(
                        text = routeLabel,
                        fontFamily = BodyFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.secondaryText,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                // Resize Toggle in Top Area
                TopActionIconChip(
                    icon = Icons.Default.OpenInFull,
                    contentDescription = "Resize keyboard",
                    active = showResizeStrip,
                    palette = palette,
                    borderColor = borderCol,
                    testTag = "deck_top_resize_toggle",
                    onClick = {
                        showResizeStrip = !showResizeStrip
                        if (showResizeStrip) showQuickSettingsPopup = false
                    },
                )
                // Quick Settings Popup Toggle
                TopActionIconChip(
                    icon = Icons.Default.Settings,
                    contentDescription = "Quick keyboard settings",
                    active = showQuickSettingsPopup,
                    palette = palette,
                    borderColor = borderCol,
                    testTag = "deck_top_settings_toggle",
                    onClick = {
                        showQuickSettingsPopup = !showQuickSettingsPopup
                        if (showQuickSettingsPopup) showResizeStrip = false
                    },
                )
                if (onMinimize != null) {
                    TopActionIconChip(
                        icon = Icons.Default.KeyboardHide,
                        contentDescription = "Minimize keyboard",
                        active = false,
                        palette = palette,
                        borderColor = borderCol,
                        testTag = "ime_minimize_button",
                        onClick = onMinimize,
                    )
                }
                if (isOverlay && onCloseOverlay != null) {
                    TopActionIconChip(
                        icon = Icons.Default.Close,
                        contentDescription = "Close floating keyboard",
                        active = true,
                        palette = palette,
                        borderColor = borderCol,
                        testTag = "overlay_close_button",
                        onClick = onCloseOverlay,
                    )
                }
            }
        }

        // Expandable Top Resize Controls
        AnimatedVisibility(visible = showResizeStrip) {
            DeckQuickResizeStrip(
                settings = settings,
                palette = palette,
                isOverlay = isOverlay,
                onUpdateSettings = onUpdateSettings,
                onResizeDragDelta = onResizeDragDelta,
            )
        }

        // Expandable Quick Feature Settings Popup
        AnimatedVisibility(visible = showQuickSettingsPopup) {
            DeckQuickSettingsPopup(
                settings = settings,
                palette = palette,
                onUpdateSettings = onUpdateSettings,
                onDismiss = { showQuickSettingsPopup = false },
            )
        }

        // Compact Shortcut Bar (capped to 4 concise favorites so top area stays clean!)
        if (settings.showHotkeyBar && hotkeys.isNotEmpty() && settings.layoutMode != KeyboardLayoutMode.GAMING) {
            val topFavorites = remember(hotkeys) { hotkeys.take(4) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 2.dp, vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                topFavorites.forEach { hotkey ->
                    val accentColor = Color(hotkey.accentHex)
                    Row(
                        modifier = Modifier
                            .neumorphicRaised(
                                surfaceColor = palette.keyBg,
                                shadowLight = palette.shadowLight,
                                shadowDark = palette.shadowDark,
                                cornerRadius = 10.dp,
                                shadowOffset = 2.dp,
                                blurRadius = 4.dp,
                                borderColor = borderCol,
                            )
                            .clickable { onHotkeyDispatch(hotkey) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("hotkey_chip_${hotkey.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                        )
                        Text(
                            text = hotkey.label,
                            color = palette.primaryText,
                            fontFamily = BodyFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            text = hotkey.comboText(),
                            color = accentColor,
                            fontFamily = BodyFontFamily,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // Optional Top Function Row (Esc, F1..F12) for PC modes
        if ((settings.showFunctionRow || fnActive) && settings.layoutMode != KeyboardLayoutMode.GAMING && settings.layoutMode != KeyboardLayoutMode.NUMPAD_ONLY) {
            val fnHeight = (keyHeight * 0.78f).coerceAtLeast(24.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(hGap),
            ) {
                PcKeyboardLayoutData.functionRow.forEach { spec ->
                    PcKeyCap(
                        spec = spec,
                        palette = palette,
                        keyHeight = fnHeight,
                        keyCorner = (keyCorner - 2.dp).coerceAtLeast(4.dp),
                        fontScale = fontScale * 0.85f,
                        upperCaseLetters = false,
                        shiftActive = false,
                        fnActive = false,
                        isModifierLit = spec.id == "num_toggle_top" && settings.layoutMode != KeyboardLayoutMode.PC_STANDARD,
                        compact = true,
                        showKeyBorder = settings.showKeyBorder,
                        rgbActive = rgbActive,
                        rgbPhaseDeg = rgbPhaseDeg,
                        modifier = Modifier.weight(spec.weight),
                        onClick = { handleKeyTap(spec) },
                    )
                }
            }
        }

        // Main Deck Body
        when (settings.layoutMode) {
            KeyboardLayoutMode.PC_WITH_NUMPAD -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(hGap + 3.dp),
                ) {
                    KeyGridBlock(
                        rows = PcKeyboardLayoutData.splitPcRows,
                        palette = palette,
                        keyHeight = keyHeight,
                        hGap = hGap,
                        vGap = vGap,
                        keyCorner = (keyCorner - 1.dp).coerceAtLeast(5.dp),
                        fontScale = fontScale * 0.92f,
                        upperCaseLetters = effectiveUpperCase,
                        ctrlActive = ctrlActive,
                        altActive = altActive,
                        shiftActive = shiftActive,
                        capsActive = capsActive,
                        metaActive = metaActive,
                        fnActive = fnActive,
                        compact = true,
                        showKeyBorder = settings.showKeyBorder,
                        rgbActive = rgbActive,
                        rgbPhaseDeg = rgbPhaseDeg,
                        modifier = Modifier.weight(0.68f),
                        onKeyTap = { handleKeyTap(it, isFromNumpad = false) },
                    )
                    NumpadBlock(
                        palette = palette,
                        keyHeight = keyHeight,
                        hGap = hGap,
                        vGap = vGap,
                        keyCorner = (keyCorner - 1.dp).coerceAtLeast(5.dp),
                        fontScale = fontScale * 0.94f,
                        numLockOn = settings.numLockOn,
                        compact = true,
                        showKeyBorder = settings.showKeyBorder,
                        rgbActive = rgbActive,
                        rgbPhaseDeg = rgbPhaseDeg,
                        modifier = Modifier.weight(0.32f),
                        onKeyTap = { handleKeyTap(it, isFromNumpad = true) },
                    )
                }
            }

            KeyboardLayoutMode.PC_STANDARD -> {
                KeyGridBlock(
                    rows = PcKeyboardLayoutData.mainRows,
                    palette = palette,
                    keyHeight = keyHeight,
                    hGap = hGap,
                    vGap = vGap,
                    keyCorner = keyCorner,
                    fontScale = fontScale,
                    upperCaseLetters = effectiveUpperCase,
                    ctrlActive = ctrlActive,
                    altActive = altActive,
                    shiftActive = shiftActive,
                    capsActive = capsActive,
                    metaActive = metaActive,
                    fnActive = fnActive,
                    compact = isTightDeck,
                    showKeyBorder = settings.showKeyBorder,
                    rgbActive = rgbActive,
                    rgbPhaseDeg = rgbPhaseDeg,
                    onKeyTap = { handleKeyTap(it, isFromNumpad = false) },
                )
            }

            KeyboardLayoutMode.NUMPAD_ONLY -> {
                NumpadBlock(
                    palette = palette,
                    keyHeight = (keyHeight + 5.dp),
                    hGap = hGap + 1.dp,
                    vGap = vGap + 1.dp,
                    keyCorner = keyCorner,
                    fontScale = fontScale * 1.08f,
                    numLockOn = settings.numLockOn,
                    compact = false,
                    showKeyBorder = settings.showKeyBorder,
                    rgbActive = rgbActive,
                    rgbPhaseDeg = rgbPhaseDeg,
                    modifier = Modifier.fillMaxWidth(),
                    onKeyTap = { handleKeyTap(it, isFromNumpad = true) },
                )
            }

            KeyboardLayoutMode.GAMING -> {
                val gameKeyHeight = (keyHeight + 2.dp).coerceAtLeast(36.dp)
                val totalPadHeight = gameKeyHeight * 4 + vGap * 3
                val padWeight = settings.touchpadWidthFraction.coerceIn(0.30f, 0.55f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gaming_mode_deck"),
                    horizontalArrangement = Arrangement.spacedBy(hGap + 4.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    GamingKeysBlock(
                        rows = PcKeyboardLayoutData.gamingLeftRows,
                        palette = palette,
                        keyHeight = gameKeyHeight,
                        hGap = hGap,
                        vGap = vGap,
                        keyCorner = keyCorner,
                        fontScale = fontScale,
                        showKeyBorder = settings.showKeyBorder,
                        rgbActive = rgbActive,
                        rgbPhaseDeg = rgbPhaseDeg,
                        modifier = Modifier.weight(1f - padWeight),
                        onKeyHold = { spec, isDown ->
                            if (onKeyHoldDispatch != null) {
                                onKeyHoldDispatch(spec.keyCode, 0, isDown, spec.label)
                            } else if (isDown) {
                                onKeyDispatch(spec.keyCode, 0, spec.charPrimary, spec.label)
                            }
                        },
                    )
                    GamingMouseTouchpad(
                        palette = palette,
                        settings = settings,
                        rgbPhaseDeg = rgbPhaseDeg,
                        rgbEnabled = rgbActive,
                        totalHeight = totalPadHeight,
                        cornerRadius = keyCorner,
                        modifier = Modifier.weight(padWeight),
                        onMouseMove = { dx, dy -> onMouseMove?.invoke(dx, dy) },
                        onMouseClick = { isRight -> onMouseClick?.invoke(isRight) },
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyGridBlock(
    rows: List<List<KeySpec>>,
    palette: ResolvedKeyboardPalette,
    keyHeight: Dp,
    hGap: Dp,
    vGap: Dp,
    keyCorner: Dp,
    fontScale: Float,
    upperCaseLetters: Boolean,
    ctrlActive: Boolean,
    altActive: Boolean,
    shiftActive: Boolean,
    capsActive: Boolean,
    metaActive: Boolean,
    fnActive: Boolean,
    compact: Boolean,
    showKeyBorder: Boolean,
    rgbActive: Boolean,
    rgbPhaseDeg: Float,
    modifier: Modifier = Modifier,
    onKeyTap: (KeySpec) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(vGap),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(hGap),
            ) {
                row.forEach { spec ->
                    val isLit = when (spec.modifierType) {
                        ModifierKey.CTRL -> ctrlActive
                        ModifierKey.ALT -> altActive
                        ModifierKey.SHIFT -> shiftActive
                        ModifierKey.CAPS -> capsActive
                        ModifierKey.META -> metaActive
                        ModifierKey.FN -> fnActive
                        null -> false
                    }
                    PcKeyCap(
                        spec = spec,
                        palette = palette,
                        keyHeight = keyHeight,
                        keyCorner = keyCorner,
                        fontScale = fontScale,
                        upperCaseLetters = upperCaseLetters,
                        shiftActive = shiftActive,
                        fnActive = fnActive,
                        isModifierLit = isLit,
                        compact = compact,
                        showKeyBorder = showKeyBorder,
                        rgbActive = rgbActive,
                        rgbPhaseDeg = rgbPhaseDeg,
                        modifier = Modifier.weight(spec.weight),
                        onClick = { onKeyTap(spec) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GamingKeysBlock(
    rows: List<List<KeySpec>>,
    palette: ResolvedKeyboardPalette,
    keyHeight: Dp,
    hGap: Dp,
    vGap: Dp,
    keyCorner: Dp,
    fontScale: Float,
    showKeyBorder: Boolean,
    rgbActive: Boolean,
    rgbPhaseDeg: Float,
    modifier: Modifier = Modifier,
    onKeyHold: (KeySpec, Boolean) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(vGap),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(hGap),
            ) {
                row.forEach { spec ->
                    GamingHoldKeyCap(
                        spec = spec,
                        palette = palette,
                        keyHeight = keyHeight,
                        keyCorner = keyCorner,
                        fontScale = fontScale,
                        showKeyBorder = showKeyBorder,
                        rgbActive = rgbActive,
                        rgbPhaseDeg = rgbPhaseDeg,
                        modifier = Modifier.weight(spec.weight),
                        onHoldChanged = { isDown -> onKeyHold(spec, isDown) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GamingHoldKeyCap(
    spec: KeySpec,
    palette: ResolvedKeyboardPalette,
    keyHeight: Dp,
    keyCorner: Dp,
    fontScale: Float,
    showKeyBorder: Boolean,
    rgbActive: Boolean,
    rgbPhaseDeg: Float,
    modifier: Modifier = Modifier,
    onHoldChanged: (Boolean) -> Unit,
) {
    var held by remember { mutableStateOf(false) }
    val pressOffsetY by animateDpAsState(
        targetValue = if (held) 1.5.dp else 0.dp,
        animationSpec = spring(stiffness = 950f),
        label = "gaming_key_offset",
    )

    val borderCol = if (showKeyBorder) palette.keyBorder.copy(alpha = 0.85f) else null
    val keyBg = when {
        held -> palette.activeFill
        spec.isAccent -> palette.activeFill.copy(alpha = 0.75f)
        spec.isModifier -> palette.modifierBg
        else -> palette.keyBg
    }
    val textColor = when {
        held || spec.isAccent -> palette.accentBg
        else -> palette.primaryText
    }

    Box(
        modifier = modifier
            .height(keyHeight)
            .offset(y = pressOffsetY)
            .then(
                if (held) {
                    Modifier.neumorphicInset(
                        surfaceColor = keyBg,
                        shadowLight = palette.shadowLight,
                        shadowDark = palette.shadowDark,
                        cornerRadius = keyCorner,
                        depth = 3.dp,
                        borderColor = borderCol,
                    )
                } else {
                    Modifier.neumorphicRaised(
                        surfaceColor = keyBg,
                        shadowLight = palette.shadowLight,
                        shadowDark = palette.shadowDark,
                        cornerRadius = keyCorner,
                        shadowOffset = 3.dp,
                        blurRadius = 6.dp,
                        borderColor = borderCol,
                    )
                },
            )
            .then(
                if (rgbActive) {
                    Modifier.rgbAnimatedBorder(
                        phaseDegrees = rgbPhaseDeg,
                        cornerRadius = keyCorner,
                        strokeWidth = if (held || spec.isAccent) 1.8.dp else 1.3.dp,
                        alpha = if (held) 0.98f else 0.80f,
                    )
                } else {
                    Modifier
                },
            )
            .pointerInput(spec.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    held = true
                    onHoldChanged(true)
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.all { !it.pressed }) {
                                break
                            }
                        }
                    } finally {
                        held = false
                        onHoldChanged(false)
                    }
                }
            }
            .testTag("key_${spec.id}"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = spec.label,
            color = textColor,
            fontFamily = BodyFontFamily,
            fontSize = (if (spec.label.length > 2) 10f else 13f * fontScale).coerceIn(8f, 16f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun NumpadBlock(
    palette: ResolvedKeyboardPalette,
    keyHeight: Dp,
    hGap: Dp,
    vGap: Dp,
    keyCorner: Dp,
    fontScale: Float,
    numLockOn: Boolean,
    compact: Boolean,
    showKeyBorder: Boolean,
    rgbActive: Boolean,
    rgbPhaseDeg: Float,
    modifier: Modifier = Modifier,
    onKeyTap: (KeySpec) -> Unit,
) {
    val borderCol = if (showKeyBorder) palette.keyBorder.copy(alpha = 0.72f) else null
    Column(
        modifier = modifier
            .neumorphicInset(
                surfaceColor = palette.deckBg,
                shadowLight = palette.shadowLight,
                shadowDark = palette.shadowDark,
                cornerRadius = keyCorner + 2.dp,
                depth = 2.5.dp,
                borderColor = borderCol,
            )
            .padding(2.5.dp),
        verticalArrangement = Arrangement.spacedBy(vGap),
    ) {
        PcKeyboardLayoutData.numpadRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(hGap),
            ) {
                row.forEach { spec ->
                    val isNumLockKey = spec.id == "num_lock"
                    val showNav = !numLockOn && spec.fnLabel != null
                    val displaySpec = if (showNav) {
                        spec.copy(label = spec.fnLabel ?: spec.label, shiftLabel = spec.label)
                    } else {
                        spec
                    }
                    PcKeyCap(
                        spec = displaySpec,
                        palette = palette,
                        keyHeight = keyHeight,
                        keyCorner = keyCorner,
                        fontScale = fontScale,
                        upperCaseLetters = false,
                        shiftActive = false,
                        fnActive = false,
                        isModifierLit = isNumLockKey && numLockOn,
                        compact = compact,
                        showKeyBorder = showKeyBorder,
                        rgbActive = rgbActive,
                        rgbPhaseDeg = rgbPhaseDeg,
                        modifier = Modifier.weight(spec.weight),
                        onClick = { onKeyTap(spec) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PcKeyCap(
    spec: KeySpec,
    palette: ResolvedKeyboardPalette,
    keyHeight: Dp,
    keyCorner: Dp,
    fontScale: Float,
    upperCaseLetters: Boolean,
    shiftActive: Boolean,
    fnActive: Boolean,
    isModifierLit: Boolean,
    compact: Boolean,
    showKeyBorder: Boolean,
    rgbActive: Boolean,
    rgbPhaseDeg: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isEngaged = isPressed || isModifierLit

    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed) 1.5.dp else 0.dp,
        animationSpec = spring(stiffness = 900f),
        label = "key_y_offset",
    )

    val isLetter = spec.charPrimary?.isLetter() == true
    val primaryText = when {
        fnActive && spec.fnLabel != null -> spec.fnLabel
        isLetter && upperCaseLetters && spec.charShift != null -> spec.charShift.toString()
        !isLetter && shiftActive && spec.charShift != null -> spec.charShift.toString()
        else -> spec.label
    }

    val cornerText = when {
        keyHeight < 29.dp -> null
        fnActive && spec.fnLabel != null -> spec.label
        !shiftActive && spec.shiftLabel != null && !isLetter -> spec.shiftLabel
        else -> null
    }

    val keyBg = when {
        isEngaged -> palette.activeFill
        spec.isAccent -> palette.accentBg
        spec.isModifier -> palette.modifierBg
        else -> palette.keyBg
    }

    val textColor = when {
        spec.isAccent && !isEngaged -> Color.White
        isEngaged -> palette.activeIndicator
        else -> palette.primaryText
    }

    val borderCol = if (showKeyBorder) palette.keyBorder.copy(alpha = 0.85f) else null
    val shadowDepth = if (compact) 2.dp else 3.dp
    val blurRadius = if (compact) 4.dp else 6.dp
    val iconSize = (keyHeight * 0.38f).coerceIn(11.dp, 17.dp)

    val neumorphicModifier = if (isEngaged) {
        Modifier.neumorphicInset(
            surfaceColor = keyBg,
            shadowLight = palette.shadowLight,
            shadowDark = palette.shadowDark,
            cornerRadius = keyCorner,
            depth = 2.5.dp,
            borderColor = borderCol,
        )
    } else {
        Modifier.neumorphicRaised(
            surfaceColor = keyBg,
            shadowLight = palette.shadowLight,
            shadowDark = palette.shadowDark,
            cornerRadius = keyCorner,
            shadowOffset = shadowDepth,
            blurRadius = blurRadius,
            borderColor = borderCol,
        )
    }

    Box(
        modifier = modifier
            .height(keyHeight)
            .offset(y = pressOffsetY)
            .then(neumorphicModifier)
            .then(
                if (rgbActive) {
                    Modifier.rgbAnimatedBorder(
                        phaseDegrees = rgbPhaseDeg,
                        cornerRadius = keyCorner,
                        strokeWidth = if (isEngaged) 1.6.dp else 1.15.dp,
                        alpha = if (isEngaged) 0.95f else 0.70f,
                    )
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = if (compact) 1.dp else 2.dp, vertical = 1.dp)
            .testTag("key_${spec.id}"),
    ) {
        if (cornerText != null) {
            Text(
                text = cornerText,
                color = palette.secondaryText.copy(alpha = 0.80f),
                fontFamily = BodyFontFamily,
                fontSize = (if (compact) 7f else 8f * fontScale.coerceAtMost(1.05f)).sp,
                lineHeight = 9.sp,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 3.dp, top = 1.dp),
            )
        }

        if (isModifierLit) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(3.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(palette.activeIndicator),
            )
        }

        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center,
        ) {
            when (spec.iconType) {
                KeyIconType.BACKSPACE -> Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = textColor,
                    modifier = Modifier.size(iconSize),
                )
                KeyIconType.TAB -> Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardTab,
                    contentDescription = "Tab",
                    tint = textColor,
                    modifier = Modifier.size(iconSize),
                )
                KeyIconType.ENTER -> Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                    contentDescription = "Enter",
                    tint = textColor,
                    modifier = Modifier.size(iconSize),
                )
                KeyIconType.SHIFT -> Icon(
                    imageVector = Icons.Default.Upload,
                    contentDescription = "Shift",
                    tint = textColor,
                    modifier = Modifier.size(iconSize),
                )
                KeyIconType.ARROW_UP -> Icon(
                    imageVector = Icons.Default.ArrowDropUp,
                    contentDescription = "Arrow Up",
                    tint = textColor,
                    modifier = Modifier.size((iconSize + 3.dp).coerceAtMost(20.dp)),
                )
                KeyIconType.ARROW_DOWN -> Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Arrow Down",
                    tint = textColor,
                    modifier = Modifier.size((iconSize + 3.dp).coerceAtMost(20.dp)),
                )
                KeyIconType.ARROW_LEFT -> Icon(
                    imageVector = Icons.Default.ArrowLeft,
                    contentDescription = "Arrow Left",
                    tint = textColor,
                    modifier = Modifier.size((iconSize + 3.dp).coerceAtMost(20.dp)),
                )
                KeyIconType.ARROW_RIGHT -> Icon(
                    imageVector = Icons.Default.ArrowRight,
                    contentDescription = "Arrow Right",
                    tint = textColor,
                    modifier = Modifier.size((iconSize + 3.dp).coerceAtMost(20.dp)),
                )
                KeyIconType.MIC_OR_NUM -> Icon(
                    imageVector = Icons.Default.Dialpad,
                    contentDescription = "Toggle Numpad",
                    tint = palette.activeIndicator,
                    modifier = Modifier.size(iconSize),
                )
                KeyIconType.META_WIN -> MetaDiamondGlyph(
                    color = textColor,
                    size = (iconSize - 2.dp).coerceAtLeast(9.dp),
                )
                KeyIconType.SPACE -> SpaceBarGlyph(
                    color = textColor,
                    modifier = Modifier
                        .width(if (compact) 16.dp else 24.dp)
                        .height(6.dp),
                )
                KeyIconType.LAYER_CIRCLE -> Canvas(modifier = Modifier.size((iconSize - 3.dp).coerceAtLeast(8.dp))) {
                    drawCircle(
                        color = textColor,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
                null -> {
                    val baseSp = when {
                        compact && primaryText.length > 3 -> 8.2f
                        compact && primaryText.length > 1 -> 9.4f
                        compact -> 11.4f
                        primaryText.length > 3 -> 9.2f
                        primaryText.length > 1 -> 10.6f
                        else -> 13.2f
                    }
                    Text(
                        text = primaryText,
                        color = textColor,
                        fontFamily = BodyFontFamily,
                        fontSize = (baseSp * fontScale).coerceIn(8f, 17f).sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaDiamondGlyph(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val gap = w * 0.08f
        val r = w * 0.20f

        fun diamond(dx: Float, dy: Float) {
            val path = Path().apply {
                moveTo(dx, dy - r)
                lineTo(dx + r, dy)
                lineTo(dx, dy + r)
                lineTo(dx - r, dy)
                close()
            }
            drawPath(path, color = color)
        }
        diamond(cx, cy - r - gap)
        diamond(cx, cy + r + gap)
        diamond(cx - r - gap, cy)
        diamond(cx + r + gap, cy)
    }
}

@Composable
private fun SpaceBarGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(0f, h)
            lineTo(w, h)
            lineTo(w, 0f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun DeckModeChip(
    label: String,
    selected: Boolean,
    palette: ResolvedKeyboardPalette,
    borderColor: Color?,
    compact: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val modifier = if (selected) {
        Modifier.neumorphicInset(
            surfaceColor = palette.activeFill,
            shadowLight = palette.shadowLight,
            shadowDark = palette.shadowDark,
            cornerRadius = 9.dp,
            depth = 2.dp,
            borderColor = borderColor,
        )
    } else {
        Modifier.neumorphicRaised(
            surfaceColor = palette.keyBg,
            shadowLight = palette.shadowLight,
            shadowDark = palette.shadowDark,
            cornerRadius = 9.dp,
            shadowOffset = 2.dp,
            blurRadius = 4.dp,
            borderColor = borderColor,
        )
    }
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (compact) 7.dp else 9.dp,
                vertical = if (compact) 3.5.dp else 4.5.dp,
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) palette.activeIndicator else palette.secondaryText,
            fontFamily = BodyFontFamily,
            fontSize = if (compact) 9.5.sp else 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TopActionIconChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    active: Boolean,
    palette: ResolvedKeyboardPalette,
    borderColor: Color?,
    testTag: String,
    onClick: () -> Unit,
) {
    val mod = if (active) {
        Modifier.neumorphicInset(
            surfaceColor = palette.activeFill,
            shadowLight = palette.shadowLight,
            shadowDark = palette.shadowDark,
            cornerRadius = 9.dp,
            depth = 2.dp,
            borderColor = borderColor,
        )
    } else {
        Modifier.neumorphicRaised(
            surfaceColor = palette.keyBg,
            shadowLight = palette.shadowLight,
            shadowDark = palette.shadowDark,
            cornerRadius = 9.dp,
            shadowOffset = 2.dp,
            blurRadius = 4.dp,
            borderColor = borderColor,
        )
    }
    Box(
        modifier = mod
            .size(26.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) palette.accentBg else palette.primaryText,
            modifier = Modifier.size(14.dp),
        )
    }
}
