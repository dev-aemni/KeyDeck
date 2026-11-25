package com.example

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import com.example.service.FloatingKeyboardService
import com.example.service.KeyDeckImeService

/**
 * Lightweight, transparent launcher entry activity that immediately starts and expands the
 * Floating Keyboard overlay without opening KeyDeck's main settings UI.
 *
 * Exposes a dedicated launcher icon ("KeyDeck Float"), component name, custom Intent action
 * (`com.aemni.keydeck.OPEN_FLOATING`), and deep link (`keydeck://floating`) so external launchers,
 * sidebars, automation tools, or system shortcuts can open the Floating Keyboard directly.
 */
class FloatingDeckLauncherActivity : Activity() {

    companion object {
        const val ACTION_OPEN_FLOATING = "com.aemni.keydeck.OPEN_FLOATING"
        const val DEEP_LINK_URI = "keydeck://floating"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        suppressTransition()
        launchFloatingDeck()
        finish()
        suppressTransition()
    }

    private fun launchFloatingDeck() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                getString(R.string.floating_launcher_overlay_required),
                Toast.LENGTH_LONG,
            ).show()
            val permIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { startActivity(permIntent) }
            return
        }

        val serviceIntent = Intent(this, FloatingKeyboardService::class.java).apply {
            action = FloatingKeyboardService.ACTION_START_AND_EXPAND
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
        KeyDeckImeService.activeInstance?.requestHideSelf(0)
    }

    @Suppress("DEPRECATION")
    private fun suppressTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            overridePendingTransition(0, 0)
        }
    }
}
