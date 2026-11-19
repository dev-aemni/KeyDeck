package com.example.keyboard

import android.view.KeyEvent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.KeyboardTab
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.KeyboardHide
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised

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
    onMinimize: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = remember(settings) { settings.resolvePalette() }
    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }
    var shiftActive by remember { mutableStateOf(false) }
    var capsActive by remember { mutableStateOf(false) }
    var metaActive by remember { mutableStateOf(false) }
    var fnActive by remember { mutableStateOf(false) }

    val keyHeight = settings.keyHeightDp.coerceIn(24, 54).dp
    val hGap = settings.keyHorizontalGapDp.coerceIn(1, 6).dp
    val vGap = settings.keyVerticalGapDp.coerceIn(1, 7).dp
    val deckPadding = settings.deckPaddingDp.coerceIn(1, 10).dp
    val keyCorner = settings.keyCornerRadiusDp.coerceIn(4, 16).dp
    val fontScale = settings.fontScale.coerceIn(0.70f, 1.25f)

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
                KeyboardLayoutMode.PC_STANDARD -> KeyboardLayoutMode.PC_WITH_NUMPAD
                KeyboardLayoutMode.PC_WITH_NUMPAD -> KeyboardLayoutMode.NUMPAD_ONLY
                KeyboardLayoutMode.NUMPAD_ONLY -> KeyboardLayoutMode.PC_STANDARD
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

    val isTightDeck = settings.keyHeightDp <= 31

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.deckBg.copy(alpha = if (isOverlay) settings.overlayOpacity else 1f))
            .padding(horizontal = deckPadding, vertical = deckPadding),
        verticalArrangement = Arrangement.spacedBy(vGap),
    ) {
        // Soft Neumorphic Mode Strip (can be hidden for extra-slim deck height)
        if (settings.showModeStrip) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DeckModeChip(
                        label = "PC",
                        selected = settings.layoutMode == KeyboardLayoutMode.PC_STANDARD,
                        palette = palette,
                        compact = isTightDeck,
                        testTag = "deck_mode_pc",
                        onClick = { onChangeLayoutMode(KeyboardLayoutMode.PC_STANDARD) },
                    )
                    DeckModeChip(
                        label = "PC+NUM",
                        selected = settings.layoutMode == KeyboardLayoutMode.PC_WITH_NUMPAD,
                        palette = palette,
                        compact = isTightDeck,
                        testTag = "deck_mode_split",
                        onClick = { onChangeLayoutMode(KeyboardLayoutMode.PC_WITH_NUMPAD) },
                    )
                    DeckModeChip(
                        label = "NUM",
                        selected = settings.layoutMode == KeyboardLayoutMode.NUMPAD_ONLY,
                        palette = palette,
                        compact = isTightDeck,
                        testTag = "deck_mode_numpad",
                        onClick = { onChangeLayoutMode(KeyboardLayoutMode.NUMPAD_ONLY) },
                    )
                    DeckModeChip(
                        label = "F1-12",
                        selected = settings.showFunctionRow || fnActive,
                        palette = palette,
                        compact = isTightDeck,
                        testTag = "deck_mode_fn_row",
                        onClick = onToggleFunctionRow,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .neumorphicInset(
                                surfaceColor = palette.activeFill,
                                shadowLight = palette.shadowLight,
                                shadowDark = palette.shadowDark,
                                cornerRadius = 10.dp,
                                depth = 2.dp,
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = routeLabel,
                            color = palette.accentBg,
                            fontFamily = MonoFontFamily,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (onMinimize != null && !isOverlay) {
                        Box(
                            modifier = Modifier
                                .neumorphicRaised(
                                    surfaceColor = palette.keyBg,
                                    shadowLight = palette.shadowLight,
                                    shadowDark = palette.shadowDark,
                                    cornerRadius = 10.dp,
                                    shadowOffset = 2.dp,
                                    blurRadius = 4.dp,
                                )
                                .clickable(onClick = onMinimize)
                                .padding(4.dp)
                                .testTag("ime_minimize_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardHide,
                                contentDescription = "Hide keyboard",
                                tint = palette.secondaryText,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }

        // Soft Neumorphic Customizable Hotkey Shortcuts Strip
        if (settings.showHotkeyBar && hotkeys.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isTightDeck) 5.dp else 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                hotkeys.forEach { hotkey ->
                    val accentColor = Color(hotkey.accentHex)
                    Row(
                        modifier = Modifier
                            .neumorphicRaised(
                                surfaceColor = palette.keyBg,
                                shadowLight = palette.shadowLight,
                                shadowDark = palette.shadowDark,
                                cornerRadius = 12.dp,
                                shadowOffset = 2.dp,
                                blurRadius = 4.dp,
                            )
                            .clickable { onHotkeyDispatch(hotkey) }
                            .padding(
                                horizontal = if (isTightDeck) 7.dp else 9.dp,
                                vertical = if (isTightDeck) 3.5.dp else 5.dp,
                            )
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
                            fontSize = (if (isTightDeck) 9.5f else 10.5f * fontScale.coerceAtMost(1.06f)).sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            text = hotkey.comboText(),
                            color = accentColor,
                            fontFamily = MonoFontFamily,
                            fontSize = (if (isTightDeck) 8.5f else 9.5f).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // Top Function Row matching HTML Soft UI: esc, f1..f12, num
        if (settings.showFunctionRow || fnActive) {
            val fnHeight = (keyHeight * 0.80f).coerceAtLeast(22.dp)
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
                        modifier = Modifier.weight(spec.weight),
                        onClick = { handleKeyTap(spec) },
                    )
                }
            }
        }

        // Main Keyboard Deck Area (PC Standard / Split PC+Numpad / Dedicated Numpad)
        when (settings.layoutMode) {
            KeyboardLayoutMode.PC_STANDARD -> {
                PcMainBlock(
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
                    onKeyTap = { handleKeyTap(it, isFromNumpad = false) },
                )
            }

            KeyboardLayoutMode.PC_WITH_NUMPAD -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(hGap + 2.dp),
                ) {
                    PcMainBlock(
                        palette = palette,
                        keyHeight = keyHeight,
                        hGap = (hGap - 0.5.dp).coerceAtLeast(1.dp),
                        vGap = vGap,
                        keyCorner = (keyCorner - 2.dp).coerceAtLeast(4.dp),
                        fontScale = fontScale * 0.84f,
                        upperCaseLetters = effectiveUpperCase,
                        ctrlActive = ctrlActive,
                        altActive = altActive,
                        shiftActive = shiftActive,
                        capsActive = capsActive,
                        metaActive = metaActive,
                        fnActive = fnActive,
                        compact = true,
                        modifier = Modifier.weight(0.73f),
                        onKeyTap = { handleKeyTap(it, isFromNumpad = false) },
                    )
                    NumpadBlock(
                        palette = palette,
                        keyHeight = keyHeight,
                        hGap = (hGap - 0.5.dp).coerceAtLeast(1.dp),
                        vGap = vGap,
                        keyCorner = (keyCorner - 2.dp).coerceAtLeast(4.dp),
                        fontScale = fontScale * 0.88f,
                        numLockOn = settings.numLockOn,
                        compact = true,
                        modifier = Modifier.weight(0.27f),
                        onKeyTap = { handleKeyTap(it, isFromNumpad = true) },
                    )
                }
            }

            KeyboardLayoutMode.NUMPAD_ONLY -> {
                NumpadBlock(
                    palette = palette,
                    keyHeight = (keyHeight + 4.dp),
                    hGap = hGap + 1.dp,
                    vGap = vGap + 1.dp,
                    keyCorner = keyCorner,
                    fontScale = fontScale * 1.05f,
                    numLockOn = settings.numLockOn,
                    compact = false,
                    modifier = Modifier.fillMaxWidth(),
                    onKeyTap = { handleKeyTap(it, isFromNumpad = true) },
                )
            }
        }
    }
}

@Composable
private fun PcMainBlock(
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
    modifier: Modifier = Modifier,
    onKeyTap: (KeySpec) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(vGap),
    ) {
        PcKeyboardLayoutData.mainRows.forEach { row ->
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
                        modifier = Modifier.weight(spec.weight),
                        onClick = { onKeyTap(spec) },
                    )
                }
            }
        }
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
    modifier: Modifier = Modifier,
    onKeyTap: (KeySpec) -> Unit,
) {
    Column(
        modifier = modifier
            .neumorphicInset(
                surfaceColor = palette.deckBg,
                shadowLight = palette.shadowLight,
                shadowDark = palette.shadowDark,
                cornerRadius = keyCorner + 3.dp,
                depth = 3.dp,
            )
            .padding(3.dp),
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
        keyHeight < 28.dp -> null
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

    val shadowDepth = if (compact) 2.5.dp else 3.5.dp
    val blurRadius = if (compact) 5.dp else 7.dp
    val iconSize = (keyHeight * 0.40f).coerceIn(10.dp, 17.dp)

    val neumorphicModifier = if (isEngaged) {
        Modifier.neumorphicInset(
            surfaceColor = keyBg,
            shadowLight = palette.shadowLight,
            shadowDark = palette.shadowDark,
            cornerRadius = keyCorner,
            depth = 3.dp,
        )
    } else {
        Modifier.neumorphicRaised(
            surfaceColor = keyBg,
            shadowLight = palette.shadowLight,
            shadowDark = palette.shadowDark,
            cornerRadius = keyCorner,
            shadowOffset = shadowDepth,
            blurRadius = blurRadius,
        )
    }

    Box(
        modifier = modifier
            .height(keyHeight)
            .offset(y = pressOffsetY)
            .then(neumorphicModifier)
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
                color = palette.secondaryText.copy(alpha = 0.78f),
                fontFamily = MonoFontFamily,
                fontSize = (if (compact) 6.5f else 7.5f * fontScale.coerceAtMost(1.05f)).sp,
                lineHeight = 8.sp,
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
                        compact && primaryText.length > 3 -> 7.2f
                        compact && primaryText.length > 1 -> 8.5f
                        compact -> 10.5f
                        primaryText.length > 3 -> 8.5f
                        primaryText.length > 1 -> 9.8f
                        else -> 12.8f
                    }
                    Text(
                        text = primaryText,
                        color = textColor,
                        fontFamily = BodyFontFamily,
                        fontSize = (baseSp * fontScale).coerceIn(7f, 16f).sp,
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
    compact: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val modifier = if (selected) {
        Modifier.neumorphicInset(
            surfaceColor = palette.activeFill,
            shadowLight = palette.shadowLight,
            shadowDark = palette.shadowDark,
            cornerRadius = 10.dp,
            depth = 2.dp,
        )
    } else {
        Modifier.neumorphicRaised(
            surfaceColor = palette.keyBg,
            shadowLight = palette.shadowLight,
            shadowDark = palette.shadowDark,
            cornerRadius = 10.dp,
            shadowOffset = 2.dp,
            blurRadius = 4.dp,
        )
    }
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (compact) 7.dp else 9.dp,
                vertical = if (compact) 3.dp else 4.dp,
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) palette.activeIndicator else palette.secondaryText,
            fontFamily = MonoFontFamily,
            fontSize = if (compact) 8.5.sp else 9.5.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
