package com.example

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.example.service.FloatingKeyboardService
import com.example.service.KeyDeckImeService
import com.example.ui.inspectSystemReadiness

/**
 * Lightweight, transparent launcher shortcut activity that directly opens the Floating Keyboard
 * overlay once minimum requirements (KeyDeck IME enabled & selected, plus Display over other apps)
 * are granted. If any of those minimum requirements are missing, it opens MainActivity instead of
 * forcing the user into system permission menus.
 */
class FloatingDeckLauncherActivity : Activity() {

    companion object {
        const val ACTION_FLOAT_ALIAS = "com.aemni.keydeck.float"
        const val ACTION_OPEN_FLOATING = "com.aemni.keydeck.OPEN_FLOATING"
        const val DEEP_LINK_URI = "keydeck://floating"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        suppressTransition()
        launchFloatingDeckOrMain()
        finish()
        suppressTransition()
    }

    private fun launchFloatingDeckOrMain() {
        val readiness = inspectSystemReadiness(this)
        if (!readiness.isFloatingMinReady) {
            Toast.makeText(
                this,
                getString(R.string.floating_launcher_setup_needed),
                Toast.LENGTH_SHORT,
            ).show()
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
            }
            runCatching { startActivity(mainIntent) }
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
