package com.example.keyboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.KeyEvent
import com.example.data.KeyDeckRuntimeState
import com.example.service.FloatingKeyboardService
import com.example.service.KeyDeckAccessibilityService
import com.example.service.KeyDeckImeService
import java.lang.ref.WeakReference

object KeyDispatcher {

    interface DirectKeyTarget {
        fun onDirectKeyEvent(keyCode: Int, metaState: Int, charToCommit: Char?): Boolean
        fun onDirectKeyHold(keyCode: Int, metaState: Int, isDown: Boolean): Boolean = false
        fun getSurroundingChars(beforeLen: Int, afterLen: Int): Pair<String, String>? = null
        fun replaceTailText(expectedTail: String, replacement: String): Boolean = false
    }

    interface DirectMouseTarget {
        fun onMouseDelta(dx: Float, dy: Float)
        fun onMouseClick(isRightClick: Boolean)
    }

    private var directTargetRef: WeakReference<DirectKeyTarget>? = null
    private var directMouseRef: WeakReference<DirectMouseTarget>? = null

    fun setDirectTarget(target: DirectKeyTarget?) {
        directTargetRef = target?.let { WeakReference(it) }
    }

    fun setDirectMouseTarget(target: DirectMouseTarget?) {
        directMouseRef = target?.let { WeakReference(it) }
    }

    fun activeRouteLabel(): String {
        val direct = if (KeyDeckRuntimeState.isAppInForeground.value) directTargetRef?.get() else null
        val hasIme = KeyDeckImeService.hasActiveInputConnection()
        val hasA11y = KeyDeckAccessibilityService.activeInstance != null
        return when {
            direct != null -> "In-App"
            hasIme && hasA11y -> "IME+A11y"
            hasIme -> "IME"
            hasA11y -> "A11y"
            else -> "Setup"
        }
    }

    fun shouldDisableSuggestions(): Boolean {
        val direct = if (KeyDeckRuntimeState.isAppInForeground.value) directTargetRef?.get() else null
        if (direct != null) return false
        val ime = KeyDeckImeService.activeInstance
        if (ime != null && KeyDeckImeService.hasActiveInputConnection()) {
            return ime.isSuggestionsSuppressedInput()
        }
        val a11y = KeyDeckAccessibilityService.activeInstance
        if (a11y != null) {
            return a11y.isSuggestionsSuppressedInput()
        }
        return false
    }

    fun getSurroundingChars(beforeLen: Int = 2, afterLen: Int = 2): Pair<String, String>? {
        val direct = if (KeyDeckRuntimeState.isAppInForeground.value) directTargetRef?.get() else null
        if (direct != null) {
            return direct.getSurroundingChars(beforeLen, afterLen)
        }
        val ime = KeyDeckImeService.activeInstance
        if (ime != null && KeyDeckImeService.hasActiveInputConnection()) {
            return ime.getSurroundingChars(beforeLen, afterLen)
        }
        val a11y = KeyDeckAccessibilityService.activeInstance
        if (a11y != null) {
            return a11y.getSurroundingChars(beforeLen, afterLen)
        }
        return null
    }

    fun isCursorTouchingWord(): Boolean {
        val (before, after) = getSurroundingChars(beforeLen = 1, afterLen = 1) ?: return false
        val beforeTouches = before.lastOrNull()?.let { it.isLetterOrDigit() || it == '\'' } == true
        val afterTouches = after.firstOrNull()?.let { it.isLetterOrDigit() || it == '\'' } == true
        return beforeTouches || afterTouches
    }

    fun dispatchSuggestionReplace(
        context: Context,
        expectedTail: String,
        replacement: String,
        comboLabel: String,
        hapticEnabled: Boolean = true,
    ): Boolean {
        if (hapticEnabled) {
            triggerHaptic(context)
        }
        if (shouldDisableSuggestions()) return false

        val direct = if (KeyDeckRuntimeState.isAppInForeground.value) directTargetRef?.get() else null
        if (direct != null && direct.replaceTailText(expectedTail, replacement)) {
            KeyDeckRuntimeState.recordDispatchedKey(
                DispatchedKeyRecord(
                    timestampMs = System.currentTimeMillis(),
                    comboLabel = comboLabel,
                    keyCode = KeyEvent.KEYCODE_UNKNOWN,
                    route = "In-App",
                    detail = if (expectedTail.isNotEmpty()) "$expectedTail → ${replacement.trimEnd()}" else replacement.trimEnd(),
                ),
            )
            return true
        }

        val ime = KeyDeckImeService.activeInstance
        if (ime != null && KeyDeckImeService.hasActiveInputConnection() && ime.replaceTailText(expectedTail, replacement)) {
            KeyDeckRuntimeState.recordDispatchedKey(
                DispatchedKeyRecord(
                    timestampMs = System.currentTimeMillis(),
                    comboLabel = comboLabel,
                    keyCode = KeyEvent.KEYCODE_UNKNOWN,
                    route = "IME",
                    detail = if (expectedTail.isNotEmpty()) "$expectedTail → ${replacement.trimEnd()}" else replacement.trimEnd(),
                ),
            )
            return true
        }

        val a11y = KeyDeckAccessibilityService.activeInstance
        if (a11y != null && a11y.replaceTailText(expectedTail, replacement)) {
            KeyDeckRuntimeState.recordDispatchedKey(
                DispatchedKeyRecord(
                    timestampMs = System.currentTimeMillis(),
                    comboLabel = comboLabel,
                    keyCode = KeyEvent.KEYCODE_UNKNOWN,
                    route = "A11y",
                    detail = if (expectedTail.isNotEmpty()) "$expectedTail → ${replacement.trimEnd()}" else replacement.trimEnd(),
                ),
            )
            return true
        }
        return false
    }

    fun dispatchKey(
        context: Context,
        keyCode: Int,
        metaState: Int,
        charToCommit: Char? = null,
        comboLabel: String,
        hapticEnabled: Boolean = true,
        hotkeys: List<CustomHotkey> = emptyList(),
    ) {
        if (hapticEnabled) {
            triggerHaptic(context)
        }

        // Intercept secret/quick Floating Keyboard shortcut (e.g. Ctrl+Alt+F -> open Floating Keyboard from IME)
        if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
            val matchedFloatShortcut = hotkeys.firstOrNull { hk ->
                hk.actionType == HotkeyActionType.TOGGLE_FLOATING &&
                    hk.matchesKeyEvent(keyCode, metaState)
            }
            val isDefaultFloatCombo = hotkeys.isEmpty() &&
                keyCode == KeyEvent.KEYCODE_F &&
                (metaState and KeyEvent.META_CTRL_ON) != 0 &&
                (metaState and KeyEvent.META_ALT_ON) != 0 &&
                (metaState and KeyEvent.META_SHIFT_ON) == 0 &&
                (metaState and KeyEvent.META_META_ON) == 0
            if (matchedFloatShortcut != null || isDefaultFloatCombo) {
                launchOrToggleFloatingKeyboard(
                    context = context,
                    comboLabel = matchedFloatShortcut?.comboText() ?: "Ctrl+Alt+F",
                )
                return
            }

            // Intercept custom App-Launch keyboard shortcuts (e.g. Ctrl+Alt+C -> launch Chrome)
            val matchedAppShortcut = hotkeys.firstOrNull { hk ->
                hk.actionType == HotkeyActionType.LAUNCH_APP &&
                    hk.targetPackageName.isNotBlank() &&
                    hk.matchesKeyEvent(keyCode, metaState)
            }
            if (matchedAppShortcut != null) {
                launchAppByPackage(
                    context = context,
                    packageName = matchedAppShortcut.targetPackageName,
                    appLabel = matchedAppShortcut.targetAppLabel.ifBlank { matchedAppShortcut.label },
                    comboLabel = matchedAppShortcut.comboText(),
                )
                return
            }
        }

        val now = SystemClock.uptimeMillis()

        val directTarget = if (KeyDeckRuntimeState.isAppInForeground.value) directTargetRef?.get() else null
        if (directTarget != null && directTarget.onDirectKeyEvent(keyCode, metaState, charToCommit)) {
            KeyDeckRuntimeState.recordDispatchedKey(
                DispatchedKeyRecord(
                    timestampMs = System.currentTimeMillis(),
                    comboLabel = comboLabel,
                    keyCode = keyCode,
                    route = "In-App",
                    detail = formatMetaDetail(keyCode, metaState),
                ),
            )
            return
        }

        val a11y = KeyDeckAccessibilityService.activeInstance
        if (a11y != null && a11y.isGlobalSystemShortcut(keyCode, metaState)) {
            val handledDetail = a11y.dispatchPcShortcut(keyCode, metaState, charToCommit)
            KeyDeckRuntimeState.recordDispatchedKey(
                DispatchedKeyRecord(
                    timestampMs = System.currentTimeMillis(),
                    comboLabel = comboLabel,
                    keyCode = keyCode,
                    route = "A11y",
                    detail = handledDetail,
                ),
            )
            return
        }

        val ime = KeyDeckImeService.activeInstance
        if (ime != null && ime.dispatchPcKey(keyCode, metaState, charToCommit, now)) {
            KeyDeckRuntimeState.recordDispatchedKey(
                DispatchedKeyRecord(
                    timestampMs = System.currentTimeMillis(),
                    comboLabel = comboLabel,
                    keyCode = keyCode,
                    route = "IME",
                    detail = formatMetaDetail(keyCode, metaState),
                ),
            )
            return
        }

        if (a11y != null) {
            val handledDetail = a11y.dispatchPcShortcut(keyCode, metaState, charToCommit)
            KeyDeckRuntimeState.recordDispatchedKey(
                DispatchedKeyRecord(
                    timestampMs = System.currentTimeMillis(),
                    comboLabel = comboLabel,
                    keyCode = keyCode,
                    route = "A11y",
                    detail = handledDetail,
                ),
            )
            return
        }

        KeyDeckRuntimeState.recordDispatchedKey(
            DispatchedKeyRecord(
                timestampMs = System.currentTimeMillis(),
                comboLabel = comboLabel,
                keyCode = keyCode,
                route = "Setup",
                detail = "Enable Accessibility or select KeyDeck IME for external apps",
            ),
        )
    }

    fun dispatchKeyHold(
        context: Context,
        keyCode: Int,
        metaState: Int,
        isDown: Boolean,
        comboLabel: String,
        hapticEnabled: Boolean = true,
    ) {
        if (isDown && hapticEnabled) {
            triggerHaptic(context)
        }
        val now = SystemClock.uptimeMillis()

        val directTarget = if (KeyDeckRuntimeState.isAppInForeground.value) directTargetRef?.get() else null
        if (directTarget != null && directTarget.onDirectKeyHold(keyCode, metaState, isDown)) {
            if (isDown) {
                KeyDeckRuntimeState.recordDispatchedKey(
                    DispatchedKeyRecord(
                        timestampMs = System.currentTimeMillis(),
                        comboLabel = "$comboLabel (Hold)",
                        keyCode = keyCode,
                        route = "In-App",
                        detail = "Hold Active",
                    ),
                )
            }
            return
        }

        val ime = KeyDeckImeService.activeInstance
        if (ime != null && ime.dispatchKeyHoldEvent(keyCode, metaState, isDown, now)) {
            if (isDown) {
                KeyDeckRuntimeState.recordDispatchedKey(
                    DispatchedKeyRecord(
                        timestampMs = System.currentTimeMillis(),
                        comboLabel = "$comboLabel (Hold)",
                        keyCode = keyCode,
                        route = "IME",
                        detail = "Raw Key Hold",
                    ),
                )
            }
            return
        }

        val a11y = KeyDeckAccessibilityService.activeInstance
        if (a11y != null && a11y.dispatchKeyHoldViaA11y(keyCode, metaState, isDown)) {
            if (isDown) {
                KeyDeckRuntimeState.recordDispatchedKey(
                    DispatchedKeyRecord(
                        timestampMs = System.currentTimeMillis(),
                        comboLabel = "$comboLabel (Hold)",
                        keyCode = keyCode,
                        route = "A11y",
                        detail = "Hold Active (A11y IME)",
                    ),
                )
            }
            return
        }

        if (isDown) {
            val detail = a11y?.dispatchPcShortcut(keyCode, metaState, null) ?: "Game key $comboLabel"
            KeyDeckRuntimeState.recordDispatchedKey(
                DispatchedKeyRecord(
                    timestampMs = System.currentTimeMillis(),
                    comboLabel = comboLabel,
                    keyCode = keyCode,
                    route = if (a11y != null) "A11y" else "GamePad",
                    detail = detail,
                ),
            )
        }
    }

    fun dispatchMouseMove(
        dx: Float,
        dy: Float,
        sensitivity: Float,
        mcpeLookMode: Boolean,
    ) {
        val scaledDx = dx * sensitivity
        val scaledDy = dy * sensitivity

        val directMouse = if (KeyDeckRuntimeState.isAppInForeground.value) directMouseRef?.get() else null
        if (directMouse != null) {
            directMouse.onMouseDelta(scaledDx, scaledDy)
            return
        }

        val a11y = KeyDeckAccessibilityService.activeInstance
        a11y?.dispatchMouseLookOrMove(scaledDx, scaledDy, mcpeLookMode)
    }

    fun dispatchMouseEnd(mcpeLookMode: Boolean) {
        val a11y = KeyDeckAccessibilityService.activeInstance
        a11y?.dispatchMouseEnd(mcpeLookMode)
    }

    fun dispatchMouseClick(
        context: Context,
        isRightClick: Boolean,
        mcpeLookMode: Boolean,
        hapticEnabled: Boolean = true,
    ) {
        if (hapticEnabled) {
            triggerHaptic(context)
        }

        val directMouse = if (KeyDeckRuntimeState.isAppInForeground.value) directMouseRef?.get() else null
        if (directMouse != null) {
            directMouse.onMouseClick(isRightClick)
            KeyDeckRuntimeState.recordDispatchedKey(
                DispatchedKeyRecord(
                    timestampMs = System.currentTimeMillis(),
                    comboLabel = if (isRightClick) "RMB" else "LMB",
                    keyCode = 0,
                    route = "Mouse",
                    detail = if (isRightClick) "Right Click / Use" else "Left Click / Attack",
                ),
            )
            return
        }

        val a11y = KeyDeckAccessibilityService.activeInstance
        val detail = a11y?.dispatchMouseTap(isRightClick, mcpeLookMode)
            ?: "Enable Accessibility for external touch/mouse clicks"
        KeyDeckRuntimeState.recordDispatchedKey(
            DispatchedKeyRecord(
                timestampMs = System.currentTimeMillis(),
                comboLabel = if (isRightClick) "RMB" else "LMB",
                keyCode = 0,
                route = if (a11y != null) "A11y Mouse" else "Mouse",
                detail = detail,
            ),
        )
    }

    fun dispatchHotkey(
        context: Context,
        hotkey: CustomHotkey,
        hapticEnabled: Boolean = true,
    ) {
        if (hapticEnabled) {
            triggerHaptic(context)
        }
        if (hotkey.actionType == HotkeyActionType.TOGGLE_FLOATING) {
            launchOrToggleFloatingKeyboard(
                context = context,
                comboLabel = hotkey.comboText(),
            )
            return
        }
        if (hotkey.actionType == HotkeyActionType.LAUNCH_APP && hotkey.targetPackageName.isNotBlank()) {
            launchAppByPackage(
                context = context,
                packageName = hotkey.targetPackageName,
                appLabel = hotkey.targetAppLabel.ifBlank { hotkey.label },
                comboLabel = hotkey.comboText(),
            )
            return
        }
        dispatchKey(
            context = context,
            keyCode = hotkey.keyCode,
            metaState = hotkey.metaState(),
            charToCommit = null,
            comboLabel = hotkey.comboText(),
            hapticEnabled = false,
        )
    }

    fun launchOrToggleFloatingKeyboard(
        context: Context,
        comboLabel: String = "Ctrl+Alt+F",
    ): Boolean {
        if (!Settings.canDrawOverlays(context)) {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }
            KeyDeckRuntimeState.recordDispatchedKey(
                DispatchedKeyRecord(
                    timestampMs = System.currentTimeMillis(),
                    comboLabel = comboLabel,
                    keyCode = 0,
                    route = "Setup",
                    detail = "Enable Display over other apps to open Floating Keyboard",
                ),
            )
            return false
        }

        val isRunning = KeyDeckRuntimeState.isOverlayServiceRunning.value
        val isExpanded = KeyDeckRuntimeState.isKeyboardOverlayExpanded.value
        val imeShown = KeyDeckImeService.activeInstance?.isInputViewShown == true
        val shouldExpand = !isRunning || !isExpanded || imeShown

        val intent = Intent(context, FloatingKeyboardService::class.java).apply {
            action = if (shouldExpand) {
                FloatingKeyboardService.ACTION_START_AND_EXPAND
            } else {
                FloatingKeyboardService.ACTION_TOGGLE_KEYBOARD
            }
        }

        val started = runCatching {
            if (!isRunning && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            true
        }.getOrDefault(false)

        if (started && shouldExpand) {
            KeyDeckImeService.activeInstance?.requestHideSelf(0)
        }

        KeyDeckRuntimeState.recordDispatchedKey(
            DispatchedKeyRecord(
                timestampMs = System.currentTimeMillis(),
                comboLabel = comboLabel,
                keyCode = 0,
                route = "Overlay",
                detail = when {
                    !started -> "Could not start Floating Keyboard"
                    shouldExpand -> "Opened Floating Keyboard"
                    else -> "Minimized Floating Keyboard"
                },
            ),
        )
        return started
    }

    fun launchAppByPackage(
        context: Context,
        packageName: String,
        appLabel: String = "",
        comboLabel: String = "Launch App",
    ): Boolean {
        val pkg = packageName.trim()
        val display = appLabel.ifBlank { pkg }
        val launchIntent = runCatching {
            context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }.getOrNull()

        val launched = if (launchIntent != null) {
            runCatching {
                context.startActivity(launchIntent)
                true
            }.getOrDefault(false)
        } else {
            false
        }

        KeyDeckRuntimeState.recordDispatchedKey(
            DispatchedKeyRecord(
                timestampMs = System.currentTimeMillis(),
                comboLabel = comboLabel,
                keyCode = 0,
                route = "App",
                detail = if (launched) "Opened $display ($pkg)" else "App not found: $pkg",
            ),
        )
        return launched
    }

    private fun formatMetaDetail(keyCode: Int, metaState: Int): String {
        val flags = buildList {
            if (metaState and KeyEvent.META_CTRL_ON != 0) add("CTRL")
            if (metaState and KeyEvent.META_ALT_ON != 0) add("ALT")
            if (metaState and KeyEvent.META_SHIFT_ON != 0) add("SHIFT")
            if (metaState and KeyEvent.META_META_ON != 0) add("META")
        }
        val flagText = if (flags.isEmpty()) "NONE" else flags.joinToString("|")
        return "KeyCode $keyCode • $flagText"
    }

    @Suppress("DEPRECATION")
    private fun triggerHaptic(context: Context) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(14L, 90))
                } else {
                    vibrator.vibrate(14L)
                }
            }
        }
    }
}
