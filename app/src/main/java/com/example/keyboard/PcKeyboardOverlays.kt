package com.example.keyboard

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicKeyCap
import com.example.ui.theme.neumorphicRaised
import com.example.ui.theme.rgbAnimatedBorder
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun GamingMouseTouchpad(
    palette: ResolvedKeyboardPalette,
    settings: KeyDeckSettings,
    rgbPhaseDeg: Float,
    rgbEnabled: Boolean,
    keyOpacity: Float = 1f,
    totalHeight: Dp,
    cornerRadius: Dp,
    onMouseMove: (dx: Float, dy: Float) -> Unit,
    onMouseClick: (isRightClick: Boolean) -> Unit,
    onMouseEnd: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val currentOnMouseMove by rememberUpdatedState(onMouseMove)
    val currentOnMouseClick by rememberUpdatedState(onMouseClick)
    val currentOnMouseEnd by rememberUpdatedState(onMouseEnd)

    var fingerActive by remember { mutableStateOf(false) }
    var isDragged by remember { mutableStateOf(false) }
    var analogMoved by remember { mutableStateOf(false) }
    var downAtMs by remember { mutableLongStateOf(0L) }
    var rawOffsetX by remember { mutableFloatStateOf(0f) }
    var rawOffsetY by remember { mutableFloatStateOf(0f) }

    val borderVisible = settings.isKeyBorderVisible()
    val borderCol = if (borderVisible && !rgbEnabled) palette.keyBorder.copy(alpha = 0.85f) else null

    // Continuous 60Hz analog stick velocity when dragged or held outside deadzone
    LaunchedEffect(fingerActive) {
        if (!fingerActive) return@LaunchedEffect
        while (fingerActive) {
            delay(16L)
            val heldPastTapMs = (SystemClock.uptimeMillis() - downAtMs) > 110L
            if (isDragged || heldPastTapMs) {
                val stick = MouseJoystickMath.computeStickOutput(rawOffsetX, rawOffsetY)
                if (stick.stepDx != 0f || stick.stepDy != 0f) {
                    analogMoved = true
                    currentOnMouseMove(stick.stepDx, stick.stepDy)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .height(totalHeight)
            .testTag("gaming_mouse_touchpad"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Recessed Hybrid Trackpad + Analog Mouse Joystick
        val padBaseColor = if (fingerActive) palette.activeFill else palette.modifierBg
        val padSurface = if (keyOpacity < 0.99f) {
            padBaseColor.copy(alpha = (padBaseColor.alpha * keyOpacity).coerceIn(0.16f, 1f))
        } else {
            padBaseColor
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .neumorphicInset(
                    surfaceColor = padSurface,
                    shadowLight = palette.shadowLight,
                    shadowDark = palette.shadowDark,
                    cornerRadius = cornerRadius,
                    depth = 4.dp,
                    borderColor = borderCol,
                    borderWidth = 1.dp,
                )
                .then(
                    if (rgbEnabled) {
                        Modifier.rgbAnimatedBorder(
                            phaseDegrees = rgbPhaseDeg,
                            cornerRadius = cornerRadius,
                            strokeWidth = if (fingerActive) 2.dp else 1.5.dp,
                            alpha = if (fingerActive) 0.98f else 0.78f,
                        )
                    } else {
                        Modifier
                    },
                )
                .pointerInput(touchSlop) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val cx = w * 0.5f
                        val cy = h * 0.5f
                        val radius = (min(w, h) * 0.38f).coerceAtLeast(18f)

                        fingerActive = true
                        isDragged = false
                        analogMoved = false
                        downAtMs = SystemClock.uptimeMillis()
                        rawOffsetX = (down.position.x - cx) / radius
                        rawOffsetY = (down.position.y - cy) / radius

                        var totalDist = 0f
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    if (!isDragged && !analogMoved) {
                                        currentOnMouseClick(false)
                                    }
                                    break
                                }
                                val delta = change.positionChange()
                                totalDist += hypot(delta.x.toDouble(), delta.y.toDouble()).toFloat()
                                if (totalDist > touchSlop) {
                                    isDragged = true
                                }
                                rawOffsetX = (change.position.x - cx) / radius
                                rawOffsetY = (change.position.y - cy) / radius
                                if (isDragged && (delta.x != 0f || delta.y != 0f)) {
                                    change.consume()
                                    currentOnMouseMove(delta.x, delta.y)
                                }
                            }
                        } finally {
                            fingerActive = false
                            isDragged = false
                            analogMoved = false
                            rawOffsetX = 0f
                            rawOffsetY = 0f
                            currentOnMouseEnd()
                        }
                    }
                }
                .padding(6.dp),
        ) {
            val stickVisual = MouseJoystickMath.computeStickOutput(rawOffsetX, rawOffsetY)

            // Circular analog joystick ring, deadzone, crosshairs, directional tether & thumbstick puck
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                val ringRadius = (min(size.width, size.height) * 0.40f).coerceAtLeast(14f)
                val deadzoneRadius = ringRadius * MouseJoystickMath.DEFAULT_DEADZONE

                val guideColor = palette.secondaryText.copy(alpha = if (fingerActive) 0.36f else 0.24f)
                val ringBorderColor = if (fingerActive) palette.accentBg.copy(alpha = 0.55f) else guideColor

                // Outer circular joystick well
                drawCircle(
                    color = palette.deckBg.copy(alpha = 0.28f),
                    radius = ringRadius,
                    center = center,
                )
                drawCircle(
                    color = ringBorderColor,
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 1.4.dp.toPx()),
                )

                // Subtle center crosshair + deadzone circle
                drawLine(
                    color = guideColor,
                    start = Offset(center.x - ringRadius * 0.72f, center.y),
                    end = Offset(center.x + ringRadius * 0.72f, center.y),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = guideColor,
                    start = Offset(center.x, center.y - ringRadius * 0.72f),
                    end = Offset(center.x, center.y + ringRadius * 0.72f),
                    strokeWidth = 1.dp.toPx(),
                )
                drawCircle(
                    color = guideColor,
                    radius = deadzoneRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )

                val knobCenter = Offset(
                    x = center.x + stickVisual.knobX * ringRadius,
                    y = center.y + stickVisual.knobY * ringRadius,
                )

                if (fingerActive && hypot(stickVisual.knobX.toDouble(), stickVisual.knobY.toDouble()) > 0.05) {
                    drawLine(
                        color = palette.accentBg.copy(alpha = 0.65f),
                        start = center,
                        end = knobCenter,
                        strokeWidth = 2.dp.toPx(),
                    )
                }

                // Thumbstick puck (stays centered cleanly when idle, tracks stick deflection when active)
                val puckColor = if (fingerActive) palette.accentBg else palette.secondaryText.copy(alpha = 0.65f)
                if (fingerActive) {
                    drawCircle(
                        color = palette.accentBg.copy(alpha = 0.24f),
                        radius = 16.dp.toPx(),
                        center = knobCenter,
                    )
                }
                drawCircle(
                    color = puckColor.copy(alpha = if (fingerActive) 0.38f else 0.20f),
                    radius = 10.dp.toPx(),
                    center = knobCenter,
                )
                drawCircle(
                    color = puckColor,
                    radius = 9.5.dp.toPx(),
                    center = knobCenter,
                    style = Stroke(width = 1.6.dp.toPx()),
                )
                drawCircle(
                    color = puckColor,
                    radius = 4.dp.toPx(),
                    center = knobCenter,
                )
            }

            Row(
                modifier = Modifier.align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Mouse,
                    contentDescription = "Mouse Joystick",
                    tint = if (fingerActive) palette.accentBg else palette.secondaryText,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = if (settings.gamingMcpeLookMode) "Look Joystick" else "Mouse Joystick",
                    fontFamily = BodyFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (fingerActive) palette.accentBg else palette.secondaryText,
                )
            }

            Text(
                text = "Drag/hold stick • Tap LMB",
                fontFamily = BodyFontFamily,
                fontSize = 8.5.sp,
                color = palette.secondaryText.copy(alpha = 0.72f),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // LMB / RMB tactile mouse buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height((settings.keyHeightDp * 0.85f).coerceIn(26f, 38f).dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TouchpadMouseButton(
                label = "LMB",
                palette = palette,
                cornerRadius = (cornerRadius - 2.dp).coerceAtLeast(4.dp),
                rgbPhaseDeg = rgbPhaseDeg,
                rgbEnabled = rgbEnabled,
                showBorder = borderVisible,
                keyOpacity = keyOpacity,
                testTag = "gaming_lmb_button",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { currentOnMouseClick(false) },
            )
            TouchpadMouseButton(
                label = "RMB",
                palette = palette,
                cornerRadius = (cornerRadius - 2.dp).coerceAtLeast(4.dp),
                rgbPhaseDeg = rgbPhaseDeg,
                rgbEnabled = rgbEnabled,
                showBorder = borderVisible,
                keyOpacity = keyOpacity,
                testTag = "gaming_rmb_button",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { currentOnMouseClick(true) },
            )
        }
    }
}

@Composable
private fun TouchpadMouseButton(
    label: String,
    palette: ResolvedKeyboardPalette,
    cornerRadius: Dp,
    rgbPhaseDeg: Float,
    rgbEnabled: Boolean,
    showBorder: Boolean,
    keyOpacity: Float = 1f,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val borderCol = if (showBorder && !rgbEnabled) palette.keyBorder.copy(alpha = 0.85f) else null
    val btnColor = (if (pressed) palette.activeFill else palette.keyBg).let { c ->
        if (keyOpacity < 0.99f) c.copy(alpha = (c.alpha * keyOpacity).coerceIn(0.16f, 1f)) else c
    }

    Box(
        modifier = modifier
            .then(
                if (pressed) {
                    Modifier.neumorphicInset(
                        surfaceColor = btnColor,
                        shadowLight = palette.shadowLight,
                        shadowDark = palette.shadowDark,
                        cornerRadius = cornerRadius,
                        depth = 3.dp,
                        borderColor = borderCol,
                    )
                } else {
                    Modifier.neumorphicKeyCap(
                        surfaceColor = btnColor,
                        shadowLight = palette.shadowLight,
                        shadowDark = palette.shadowDark,
                        cornerRadius = cornerRadius,
                        borderColor = borderCol,
                    )
                },
            )
            .then(
                if (rgbEnabled) {
                    Modifier.rgbAnimatedBorder(
                        phaseDegrees = rgbPhaseDeg,
                        cornerRadius = cornerRadius,
                        strokeWidth = 1.4.dp,
                        alpha = 0.80f,
                    )
                } else {
                    Modifier
                },
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.all { !it.pressed }) {
                                onClick()
                                break
                            }
                        }
                    } finally {
                        pressed = false
                    }
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = BodyFontFamily,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (pressed) palette.accentBg else palette.primaryText,
        )
    }
}

@Composable
fun DeckQuickResizeStrip(
    settings: KeyDeckSettings,
    palette: ResolvedKeyboardPalette,
    isOverlay: Boolean,
    onUpdateSettings: ((KeyDeckSettings) -> KeyDeckSettings) -> Unit,
    onResizeDragDelta: ((dx: Float, dy: Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val borderCol = if (settings.isKeyBorderVisible()) palette.keyBorder.copy(alpha = 0.65f) else null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .neumorphicInset(
                surfaceColor = palette.modifierBg,
                shadowLight = palette.shadowLight,
                shadowDark = palette.shadowDark,
                cornerRadius = 10.dp,
                depth = 2.dp,
                borderColor = borderCol,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("deck_quick_resize_strip"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "H ${settings.keyHeightDp}dp",
                fontFamily = BodyFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.primaryText,
            )
            MiniStepperBtn("-", palette, borderCol) {
                onUpdateSettings { it.copy(keyHeightDp = (it.keyHeightDp - 2).coerceIn(26, 54)) }
            }
            MiniStepperBtn("+", palette, borderCol) {
                onUpdateSettings { it.copy(keyHeightDp = (it.keyHeightDp + 2).coerceIn(26, 54)) }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "W ${(settings.keyboardWidthFraction * 100).roundToInt()}%",
                fontFamily = BodyFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.primaryText,
            )
            MiniStepperBtn("-", palette, borderCol) {
                onUpdateSettings { it.copy(keyboardWidthFraction = (it.keyboardWidthFraction - 0.05f).coerceIn(0.60f, 1.0f)) }
            }
            MiniStepperBtn("+", palette, borderCol) {
                onUpdateSettings { it.copy(keyboardWidthFraction = (it.keyboardWidthFraction + 0.05f).coerceIn(0.60f, 1.0f)) }
            }
        }

        if (isOverlay && onResizeDragDelta != null) {
            Row(
                modifier = Modifier
                    .neumorphicRaised(
                        surfaceColor = palette.activeFill,
                        shadowLight = palette.shadowLight,
                        shadowDark = palette.shadowDark,
                        cornerRadius = 8.dp,
                        shadowOffset = 2.dp,
                        blurRadius = 3.dp,
                        borderColor = borderCol,
                    )
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                val d = change.positionChange()
                                if (d.x != 0f || d.y != 0f) {
                                    change.consume()
                                    onResizeDragDelta(d.x, d.y)
                                }
                            }
                        }
                    }
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "Drag to resize keyboard",
                    tint = palette.accentBg,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = "Drag Size",
                    fontFamily = BodyFontFamily,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.accentBg,
                )
            }
        }
    }
}

@Composable
private fun MiniStepperBtn(
    label: String,
    palette: ResolvedKeyboardPalette,
    borderCol: Color?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 22.dp)
            .neumorphicKeyCap(
                surfaceColor = palette.keyBg,
                shadowLight = palette.shadowLight,
                shadowDark = palette.shadowDark,
                cornerRadius = 6.dp,
                borderColor = borderCol,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = BodyFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = palette.accentBg,
        )
    }
}

@Composable
fun DeckQuickSettingsPopup(
    settings: KeyDeckSettings,
    palette: ResolvedKeyboardPalette,
    isOverlay: Boolean = false,
    onUpdateSettings: ((KeyDeckSettings) -> KeyDeckSettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderCol = palette.keyBorder.copy(alpha = 0.65f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .neumorphicRaised(
                surfaceColor = palette.deckBg,
                shadowLight = palette.shadowLight,
                shadowDark = palette.shadowDark,
                cornerRadius = 14.dp,
                shadowOffset = 4.dp,
                blurRadius = 8.dp,
                borderColor = borderCol,
            )
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
            .testTag("deck_quick_settings_popup"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Quick Deck Settings",
                fontFamily = DisplayFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText,
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .neumorphicRaised(
                        surfaceColor = palette.keyBg,
                        shadowLight = palette.shadowLight,
                        shadowDark = palette.shadowDark,
                        cornerRadius = 14.dp,
                        shadowOffset = 2.dp,
                        blurRadius = 4.dp,
                        borderColor = borderCol,
                    )
                    .clickable(onClick = onDismiss)
                    .testTag("quick_settings_close"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close quick settings",
                    tint = palette.primaryText,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        // RGB Animated Border Mode Selector
        Text(
            text = "RGB Animated Border",
            fontFamily = BodyFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.secondaryText,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RgbBorderMode.entries.forEach { mode ->
                val active = settings.rgbBorderMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .then(
                            if (active) {
                                Modifier.neumorphicInset(
                                    surfaceColor = palette.activeFill,
                                    shadowLight = palette.shadowLight,
                                    shadowDark = palette.shadowDark,
                                    cornerRadius = 8.dp,
                                    depth = 2.dp,
                                    borderColor = borderCol,
                                )
                            } else {
                                Modifier.neumorphicRaised(
                                    surfaceColor = palette.keyBg,
                                    shadowLight = palette.shadowLight,
                                    shadowDark = palette.shadowDark,
                                    cornerRadius = 8.dp,
                                    shadowOffset = 2.dp,
                                    blurRadius = 4.dp,
                                    borderColor = borderCol,
                                )
                            },
                        )
                        .clickable { onUpdateSettings { it.copy(rgbBorderMode = mode) } }
                        .testTag("quick_rgb_${mode.name.lowercase()}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = mode.label,
                        fontFamily = BodyFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) palette.accentBg else palette.primaryText,
                    )
                }
            }
        }

        // Quick Theme Switcher Swatches
        Text(
            text = "Theme",
            fontFamily = BodyFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.secondaryText,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                KeyboardThemeId.PURE_BLACK,
                KeyboardThemeId.SOFT_NEUMORPH,
                KeyboardThemeId.DARK_SLATE_NEU,
                KeyboardThemeId.CYBER_MINT,
                KeyboardThemeId.NEUTRAL_GRAY,
                KeyboardThemeId.MIDNIGHT_NEU,
            ).forEach { theme ->
                val selected = settings.themeId == theme
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(theme.deckBg)
                        .clickable { onUpdateSettings { it.copy(themeId = theme) } },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = theme.displayName,
                            tint = theme.accentBg,
                            modifier = Modifier.size(14.dp),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(theme.accentBg),
                        )
                    }
                }
            }
        }

        // Compact Size, Floating Transparency & Touchpad Sensitivity Sliders
        if (isOverlay) {
            val transparency = (1f - settings.overlayOpacity.coerceIn(0.25f, 1.0f)).coerceIn(0f, 0.75f)
            QuickPopupSlider(
                label = "Floating Transparency (${(transparency * 100).roundToInt()}%)",
                value = transparency,
                range = 0f..0.75f,
                palette = palette,
                onValueChange = { t ->
                    onUpdateSettings { it.copy(overlayOpacity = (1f - t).coerceIn(0.25f, 1.0f)) }
                },
            )
        }
        QuickPopupSlider(
            label = "Key Height (${settings.keyHeightDp}dp)",
            value = settings.keyHeightDp.toFloat(),
            range = 26f..54f,
            palette = palette,
            onValueChange = { v -> onUpdateSettings { it.copy(keyHeightDp = v.roundToInt()) } },
        )
        QuickPopupSlider(
            label = "Width (${(settings.keyboardWidthFraction * 100).roundToInt()}%)",
            value = settings.keyboardWidthFraction,
            range = 0.60f..1.0f,
            palette = palette,
            onValueChange = { v -> onUpdateSettings { it.copy(keyboardWidthFraction = v) } },
        )
        if (settings.layoutMode == KeyboardLayoutMode.GAMING) {
            QuickPopupSlider(
                label = "Touchpad Speed (${String.format("%.1fx", settings.touchpadSensitivity)})",
                value = settings.touchpadSensitivity,
                range = 0.6f..3.5f,
                palette = palette,
                onValueChange = { v -> onUpdateSettings { it.copy(touchpadSensitivity = v) } },
            )
            QuickPopupToggle(
                label = "MCPE Camera Look Mode",
                checked = settings.gamingMcpeLookMode,
                palette = palette,
                onCheckedChange = { c -> onUpdateSettings { it.copy(gamingMcpeLookMode = c) } },
            )
            QuickPopupToggle(
                label = "Show PC Mouse Pointer (Game Mode)",
                checked = settings.showGameMousePointer,
                palette = palette,
                onCheckedChange = { c -> onUpdateSettings { it.copy(showGameMousePointer = c) } },
            )
        }

        // App Dark Mode Selector
        Text(
            text = "App Appearance",
            fontFamily = BodyFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.secondaryText,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AppThemeMode.entries.forEach { mode ->
                val active = settings.appThemeMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .then(
                            if (active) {
                                Modifier.neumorphicInset(
                                    surfaceColor = palette.activeFill,
                                    shadowLight = palette.shadowLight,
                                    shadowDark = palette.shadowDark,
                                    cornerRadius = 8.dp,
                                    depth = 2.dp,
                                    borderColor = borderCol,
                                )
                            } else {
                                Modifier.neumorphicRaised(
                                    surfaceColor = palette.keyBg,
                                    shadowLight = palette.shadowLight,
                                    shadowDark = palette.shadowDark,
                                    cornerRadius = 8.dp,
                                    shadowOffset = 2.dp,
                                    blurRadius = 4.dp,
                                    borderColor = borderCol,
                                )
                            },
                        )
                        .clickable { onUpdateSettings { it.copy(appThemeMode = mode) } }
                        .testTag("quick_app_theme_${mode.name.lowercase()}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = mode.label,
                        fontFamily = BodyFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) palette.accentBg else palette.primaryText,
                    )
                }
            }
        }

        QuickPopupToggle(
            label = "Manual Trigger Only (No Input Auto-Open)",
            checked = settings.suppressAutoImeOnInput,
            palette = palette,
            onCheckedChange = { c -> onUpdateSettings { it.copy(suppressAutoImeOnInput = c) } },
        )
        QuickPopupToggle(
            label = "Quick Clipboard Top Bar",
            checked = settings.simpleClipboardStripEnabled,
            palette = palette,
            onCheckedChange = { c -> onUpdateSettings { it.copy(simpleClipboardStripEnabled = c) } },
        )
        if (settings.layoutMode == KeyboardLayoutMode.SIMPLE) {
            QuickPopupToggle(
                label = "Simple 3-Suggestion Bar",
                checked = settings.simpleSuggestionsEnabled,
                palette = palette,
                onCheckedChange = { c -> onUpdateSettings { it.copy(simpleSuggestionsEnabled = c) } },
            )
            if (settings.simpleSuggestionsEnabled) {
                QuickPopupToggle(
                    label = "One-Time Auto-Correct",
                    checked = settings.simpleAutoCorrectEnabled,
                    palette = palette,
                    onCheckedChange = { c -> onUpdateSettings { it.copy(simpleAutoCorrectEnabled = c) } },
                )
            }
            QuickPopupToggle(
                label = "Auto-Capitalize Sentences",
                checked = settings.simpleAutoCapitalize,
                palette = palette,
                onCheckedChange = { c -> onUpdateSettings { it.copy(simpleAutoCapitalize = c) } },
            )
            QuickPopupToggle(
                label = "Smart Punctuation",
                checked = settings.simpleSmartPunctuation,
                palette = palette,
                onCheckedChange = { c -> onUpdateSettings { it.copy(simpleSmartPunctuation = c) } },
            )
            QuickPopupToggle(
                label = "Double-Space Period (. )",
                checked = settings.simpleDoubleSpacePeriod,
                palette = palette,
                onCheckedChange = { c -> onUpdateSettings { it.copy(simpleDoubleSpacePeriod = c) } },
            )
        }
        if (settings.layoutMode == KeyboardLayoutMode.PC_STANDARD ||
            settings.layoutMode == KeyboardLayoutMode.PC_WITH_NUMPAD ||
            settings.layoutMode == KeyboardLayoutMode.NUMPAD_ONLY
        ) {
            QuickPopupToggle(
                label = "Hold to Repeat (PC Mode)",
                checked = settings.holdToRepeat,
                palette = palette,
                onCheckedChange = { c -> onUpdateSettings { it.copy(holdToRepeat = c) } },
            )
        }
        QuickPopupToggle(
            label = "Haptic Feedback (Simple Mode)",
            checked = settings.hapticFeedback,
            palette = palette,
            onCheckedChange = { c -> onUpdateSettings { it.copy(hapticFeedback = c) } },
        )
        QuickPopupToggle(
            label = "Black Key Borders",
            checked = settings.showKeyBorder,
            palette = palette,
            onCheckedChange = { c -> onUpdateSettings { it.copy(showKeyBorder = c) } },
        )
        QuickPopupToggle(
            label = "Theme-Colored Key Borders",
            checked = settings.themeAdaptiveBorder,
            palette = palette,
            onCheckedChange = { c -> onUpdateSettings { it.copy(themeAdaptiveBorder = c) } },
        )
        QuickPopupToggle(
            label = "F1–F12 Row",
            checked = settings.showFunctionRow,
            palette = palette,
            onCheckedChange = { c -> onUpdateSettings { it.copy(showFunctionRow = c) } },
        )
        QuickPopupToggle(
            label = "NumLock Active",
            checked = settings.numLockOn,
            palette = palette,
            onCheckedChange = { c -> onUpdateSettings { it.copy(numLockOn = c) } },
        )
    }
}

@Composable
private fun QuickPopupSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    palette: ResolvedKeyboardPalette,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Text(
            text = label,
            fontFamily = BodyFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = palette.primaryText,
        )
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = palette.accentBg,
                activeTrackColor = palette.accentBg,
                inactiveTrackColor = palette.shadowDark.copy(alpha = 0.45f),
            ),
            modifier = Modifier.height(28.dp),
        )
    }
}

@Composable
private fun QuickPopupToggle(
    label: String,
    checked: Boolean,
    palette: ResolvedKeyboardPalette,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontFamily = BodyFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = palette.primaryText,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.accentBg,
                uncheckedThumbColor = palette.secondaryText,
                uncheckedTrackColor = palette.modifierBg,
                uncheckedBorderColor = palette.keyBorder,
            ),
        )
    }
}
