package com.example.keyboard

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import com.example.data.KeyDeckRuntimeState
import com.example.service.KeyDeckAccessibilityService
import com.example.service.KeyDeckImeService
import java.lang.ref.WeakReference

object KeyDispatcher {

    interface DirectKeyTarget {
        fun onDirectKeyEvent(keyCode: Int, metaState: Int, charToCommit: Char?): Boolean
        fun onDirectKeyHold(keyCode: Int, metaState: Int, isDown: Boolean): Boolean = false
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

        // Intercept custom App-Launch keyboard shortcuts (e.g. Ctrl+Alt+C -> launch Chrome)
        if (hotkeys.isNotEmpty() && keyCode != KeyEvent.KEYCODE_UNKNOWN) {
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
