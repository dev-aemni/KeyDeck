package com.example.ui.screens

import android.annotation.SuppressLint
import android.os.SystemClock
import android.view.KeyEvent
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.keyboard.KeyDispatcher
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.NeuActiveFill
import com.example.ui.theme.NeuBg
import com.example.ui.theme.NeuEmerald
import com.example.ui.theme.NeuIndigo
import com.example.ui.theme.NeuShadowDark
import com.example.ui.theme.NeuShadowLight
import com.example.ui.theme.NeuTextPrimary
import com.example.ui.theme.NeuTextSecondary
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebSandboxScreen(
    isOverlayRunning: Boolean,
    onLaunchFloatingOverWeb: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var urlInput by remember { mutableStateOf("playground://shortcuts") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(webViewRef) {
        val wv = webViewRef
        if (wv != null) {
            val target = object : KeyDispatcher.DirectKeyTarget {
                override fun onDirectKeyEvent(keyCode: Int, metaState: Int, charToCommit: Char?): Boolean {
                    wv.requestFocus()
                    val now = SystemClock.uptimeMillis()
                    val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, metaState)
                    val up = KeyEvent(now, now + 10, KeyEvent.ACTION_UP, keyCode, 0, metaState)
                    wv.dispatchKeyEvent(down)
                    wv.dispatchKeyEvent(up)
                    return true
                }
            }
            KeyDispatcher.setDirectTarget(target)
        }
        onDispose {
            KeyDispatcher.setDirectTarget(null)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeuBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "Web Shortcut Sandbox",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeuTextPrimary,
                )
                Text(
                    text = "Test PC shortcuts & numpad with zero text inputs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeuTextSecondary,
                )
            }
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .then(
                        if (isOverlayRunning) {
                            Modifier.neumorphicInset(NeuActiveFill, NeuShadowLight, NeuShadowDark, 14.dp, 3.dp)
                        } else {
                            Modifier.neumorphicRaised(NeuBg, NeuShadowLight, NeuShadowDark, 14.dp, 4.dp, 8.dp)
                        },
                    )
                    .clickable(onClick = onLaunchFloatingOverWeb)
                    .padding(horizontal = 14.dp)
                    .testTag("sandbox_float_button"),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = if (isOverlayRunning) NeuEmerald else NeuIndigo,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOverlayRunning) "Toggle Deck" else "Float Deck",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isOverlayRunning) NeuEmerald else NeuIndigo,
                    )
                }
            }
        }

        // Neumorphic Inset URL / Playground Loader Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .neumorphicInset(NeuBg, NeuShadowLight, NeuShadowDark, 14.dp, 3.dp)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Public,
                        contentDescription = null,
                        tint = NeuIndigo,
                        modifier = Modifier.size(18.dp),
                    )
                    BasicTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = BodyFontFamily,
                            fontSize = 13.sp,
                            color = NeuTextPrimary,
                        ),
                        cursorBrush = SolidColor(NeuIndigo),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sandbox_url_input"),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .height(48.dp)
                    .neumorphicRaised(NeuBg, NeuShadowLight, NeuShadowDark, 14.dp, 4.dp, 8.dp)
                    .clickable {
                        val wv = webViewRef ?: return@clickable
                        val target = urlInput.trim()
                        if (target.startsWith("playground://") || target.isEmpty()) {
                            wv.loadDataWithBaseURL("https://keydeck.local/", PLAYGROUND_HTML, "text/html", "UTF-8", null)
                        } else {
                            val normalized = if (target.startsWith("http://") || target.startsWith("https://")) {
                                target
                            } else {
                                "https://$target"
                            }
                            wv.loadUrl(normalized)
                        }
                    }
                    .padding(horizontal = 16.dp)
                    .testTag("sandbox_go_button"),
                contentAlignment = Alignment.Center,
            ) {
                Text("Go", color = NeuIndigo, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .neumorphicRaised(NeuBg, NeuShadowLight, NeuShadowDark, 14.dp, 4.dp, 8.dp)
                    .clickable {
                        urlInput = "playground://shortcuts"
                        webViewRef?.loadDataWithBaseURL(
                            "https://keydeck.local/",
                            PLAYGROUND_HTML,
                            "text/html",
                            "UTF-8",
                            null,
                        )
                    }
                    .testTag("sandbox_reset_button"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset shortcut playground", tint = NeuTextPrimary)
            }
        }

        // Embedded WebView inside a Soft UI Neumorphic frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .neumorphicRaised(NeuBg, NeuShadowLight, NeuShadowDark, 20.dp, 5.dp, 10.dp)
                .padding(6.dp),
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        isFocusable = true
                        isFocusableInTouchMode = true
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                        loadDataWithBaseURL(
                            "https://keydeck.local/",
                            PLAYGROUND_HTML,
                            "text/html",
                            "UTF-8",
                            null,
                        )
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private val PLAYGROUND_HTML = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
<style>
  :root {
    --bg: #e0e5ec;
    --shadow-light: #ffffff;
    --shadow-dark: #b9bcc6;
    --text: #4b5563;
    --accent: #4f46e5;
  }
  body {
    margin: 0; padding: 14px; font-family: system-ui, -apple-system, sans-serif;
    background: var(--bg); color: var(--text);
  }
  .badge {
    display: inline-block; padding: 6px 12px; border-radius: 999px; font-size: 11px; font-weight: 700;
    background: #d7e3f4; color: var(--accent);
    box-shadow: inset 3px 3px 6px var(--shadow-dark), inset -3px -3px 6px var(--shadow-light);
  }
  .card {
    background: var(--bg); border-radius: 18px; padding: 14px; margin-top: 12px;
    box-shadow: 6px 6px 12px var(--shadow-dark), -6px -6px 12px var(--shadow-light);
  }
  .row { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 10px; }
  .pill {
    padding: 8px 12px; border-radius: 10px; background: var(--bg); font-size: 12px; font-weight: 600;
    box-shadow: 4px 4px 8px var(--shadow-dark), -4px -4px 8px var(--shadow-light);
  }
  .pill.active {
    background: #d7e3f4; color: var(--accent);
    box-shadow: inset 3px 3px 6px var(--shadow-dark), inset -3px -3px 6px var(--shadow-light);
  }
  .stage { display: flex; align-items: center; justify-content: space-between; margin-top: 4px; }
  .meter {
    height: 10px; background: var(--bg); border-radius: 99px; overflow: hidden; margin-top: 10px;
    box-shadow: inset 3px 3px 6px var(--shadow-dark), inset -3px -3px 6px var(--shadow-light);
  }
  .meter-fill { height: 100%; width: 35%; background: #4f46e5; border-radius: 99px; transition: width 0.15s; }
  #log { font-size: 11px; color: #6b7280; max-height: 150px; overflow-y: auto; margin-top: 8px; }
  .evt { padding: 5px 0; border-bottom: 1px solid rgba(185,188,198,0.4); }
  .k { color: var(--accent); font-weight: 700; }
</style>
</head>
<body>
  <div class="badge">NO TEXT INPUTS • SOFT UI KEYDOWN MODE</div>
  <div class="card">
    <div class="stage">
      <strong id="playerState">PAUSED (Press Space / K)</strong>
      <span id="posLabel" style="color:#4f46e5;font-weight:700">35%</span>
    </div>
    <div class="meter"><div id="fill" class="meter-fill"></div></div>
    <div class="row">
      <div class="pill" id="tool-brush">Ctrl+B Brush</div>
      <div class="pill" id="tool-undo">Ctrl+Z Undo</div>
      <div class="pill" id="tool-copy">Ctrl+C Copy</div>
      <div class="pill" id="tool-num">Numpad: <span id="numVal">-</span></div>
    </div>
  </div>

  <div class="card">
    <strong>JS keydown Listener (Live)</strong>
    <div id="log"><div class="evt">Tap the floating bubble, open KeyDeck, and press any key or hotkey...</div></div>
  </div>

<script>
  let playing = false;
  let pos = 35;
  const log = document.getElementById('log');
  const stateEl = document.getElementById('playerState');
  const fillEl = document.getElementById('fill');
  const posEl = document.getElementById('posLabel');
  const numVal = document.getElementById('numVal');

  function flash(id) {
    const el = document.getElementById(id);
    if (!el) return;
    el.classList.add('active');
    setTimeout(() => el.classList.remove('active'), 450);
  }

  window.addEventListener('keydown', (e) => {
    const mods = [];
    if (e.ctrlKey) mods.push('Ctrl');
    if (e.altKey) mods.push('Alt');
    if (e.shiftKey) mods.push('Shift');
    if (e.metaKey) mods.push('Meta');
    const combo = [...mods, e.key || e.code].join('+');

    if (e.code === 'Space' || e.key === ' ' || e.key.toLowerCase() === 'k') {
      playing = !playing;
      stateEl.textContent = playing ? 'PLAYING (Space / K)' : 'PAUSED (Space / K)';
      e.preventDefault();
    } else if (e.key === 'ArrowRight' || e.key.toLowerCase() === 'l') {
      pos = Math.min(100, pos + 10);
    } else if (e.key === 'ArrowLeft' || e.key.toLowerCase() === 'j') {
      pos = Math.max(0, pos - 10);
    } else if (e.ctrlKey && e.key.toLowerCase() === 'b') {
      flash('tool-brush');
    } else if (e.ctrlKey && e.key.toLowerCase() === 'z') {
      flash('tool-undo');
    } else if (e.ctrlKey && e.key.toLowerCase() === 'c') {
      flash('tool-copy');
    }

    if (/^[0-9]$/.test(e.key)) {
      numVal.textContent = e.key;
      flash('tool-num');
      pos = parseInt(e.key, 10) * 10;
    }

    fillEl.style.width = pos + '%';
    posEl.textContent = pos + '%';

    const row = document.createElement('div');
    row.className = 'evt';
    row.innerHTML = '<span class="k">' + combo + '</span> • code=' + e.code + ' • keyCode=' + e.keyCode;
    log.prepend(row);
    if (log.children.length > 18) log.removeChild(log.lastChild);
  });
</script>
</body>
</html>
""".trimIndent()
