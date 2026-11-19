package com.example.keyboard

import android.content.Context
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
    }

    private var directTargetRef: WeakReference<DirectKeyTarget>? = null

    fun setDirectTarget(target: DirectKeyTarget?) {
        directTargetRef = target?.let { WeakReference(it) }
    }

    fun activeRouteLabel(): String {
        val direct = if (KeyDeckRuntimeState.isAppInForeground.value) directTargetRef?.get() else null
        return when {
            direct != null -> "In-App Direct"
            KeyDeckImeService.hasActiveInputConnection() -> "IME Input"
            KeyDeckAccessibilityService.activeInstance != null -> "A11y Global"
            else -> "Overlay Preview"
        }
    }

    fun dispatchKey(
        context: Context,
        keyCode: Int,
        metaState: Int,
        charToCommit: Char? = null,
        comboLabel: String,
        hapticEnabled: Boolean = true,
    ) {
        if (hapticEnabled) {
            triggerHaptic(context)
        }

        val now = SystemClock.uptimeMillis()

        // 1. Try direct in-app target ONLY when app is actively in foreground
        val directTarget = if (KeyDeckRuntimeState.isAppInForeground.value) directTargetRef?.get() else null
        if (directTarget != null && directTarget.onDirectKeyEvent(keyCode, metaState, charToCommit)) {
            KeyDeckRuntimeState.recordDispatchedKey(
                DispatchedKeyRecord(
                    timestampMs = System.currentTimeMillis(),
                    comboLabel = comboLabel,
                    keyCode = keyCode,
                    route = "In-App Direct",
                    detail = formatMetaDetail(keyCode, metaState),
                ),
            )
            return
        }

        // 2. Try active InputMethodService InputConnection
        val ime = KeyDeckImeService.activeInstance
        if (ime != null && ime.dispatchPcKey(keyCode, metaState, charToCommit, now)) {
            KeyDeckRuntimeState.recordDispatchedKey(
                DispatchedKeyRecord(
                    timestampMs = System.currentTimeMillis(),
                    comboLabel = comboLabel,
                    keyCode = keyCode,
                    route = "IME Input",
                    detail = formatMetaDetail(keyCode, metaState),
                ),
            )
            return
        }

        // 3. Try Accessibility Service global/node/scroll/media action (no text field focused!)
        val a11y = KeyDeckAccessibilityService.activeInstance
        if (a11y != null) {
            val handledDetail = a11y.dispatchPcShortcut(keyCode, metaState, charToCommit)
            KeyDeckRuntimeState.recordDispatchedKey(
                DispatchedKeyRecord(
                    timestampMs = System.currentTimeMillis(),
                    comboLabel = comboLabel,
                    keyCode = keyCode,
                    route = "A11y Global",
                    detail = handledDetail,
                ),
            )
            return
        }

        // 4. Record local telemetry if no external target is bound yet
        KeyDeckRuntimeState.recordDispatchedKey(
            DispatchedKeyRecord(
                timestampMs = System.currentTimeMillis(),
                comboLabel = comboLabel,
                keyCode = keyCode,
                route = "Preview",
                detail = "Enable Accessibility or select KeyDeck IME for external apps",
            ),
        )
    }

    fun dispatchHotkey(
        context: Context,
        hotkey: CustomHotkey,
        hapticEnabled: Boolean = true,
    ) {
        dispatchKey(
            context = context,
            keyCode = hotkey.keyCode,
            metaState = hotkey.metaState(),
            charToCommit = null,
            comboLabel = hotkey.comboText(),
            hapticEnabled = hapticEnabled,
        )
    }

    private fun formatMetaDetail(keyCode: Int, metaState: Int): String {
        val flags = buildList {
            if (metaState and KeyEvent.META_CTRL_ON != 0) add("CTRL")
            if (metaState and KeyEvent.META_ALT_ON != 0) add("ALT")
            if (metaState and KeyEvent.META_SHIFT_ON != 0) add("SHIFT")
            if (metaState and KeyEvent.META_META_ON != 0) add("META")
        }
        val flagText = if (flags.isEmpty()) "NONE" else flags.joinToString("|")
        return "KeyCode $keyCode • Meta: $flagText"
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
