package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
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

private class SandboxWebView(context: Context) : WebView(context) {
    var suppressSystemKeyboard: Boolean = true

    override fun onCheckIsTextEditor(): Boolean {
        return if (suppressSystemKeyboard) false else super.onCheckIsTextEditor()
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        return if (suppressSystemKeyboard) null else super.onCreateInputConnection(outAttrs)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebSandboxScreen(
    isOverlayRunning: Boolean,
    suppressSystemKeyboard: Boolean,
    onLaunchFloatingOverWeb: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var urlInput by remember { mutableStateOf("playground://shortcuts") }
    var webViewRef by remember { mutableStateOf<SandboxWebView?>(null) }

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

                override fun onDirectKeyHold(keyCode: Int, metaState: Int, isDown: Boolean): Boolean {
                    wv.requestFocus()
                    val now = SystemClock.uptimeMillis()
                    val action = if (isDown) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP
                    wv.dispatchKeyEvent(KeyEvent(now, now, action, keyCode, 0, metaState))
                    return true
                }
            }
            val mouseTarget = object : KeyDispatcher.DirectMouseTarget {
                override fun onMouseDelta(dx: Float, dy: Float) {
                    wv.post {
                        wv.evaluateJavascript("window.onKeyDeckMouseDelta && window.onKeyDeckMouseDelta($dx, $dy);", null)
                    }
                }

                override fun onMouseClick(isRightClick: Boolean) {
                    wv.post {
                        wv.evaluateJavascript("window.onKeyDeckMouseClick && window.onKeyDeckMouseClick($isRightClick);", null)
                    }
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
                    text = "Web & Gaming Lab",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeuTextPrimary,
                )
                Text(
                    text = "Test shortcuts, numpad, WASD hold & mouse touchpad",
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
                            Modifier.neumorphicRaised(NeuBg, NeuShadowLight, NeuShadowDark, 14.dp, 4.dp, 7.dp)
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
                    .neumorphicRaised(NeuBg, NeuShadowLight, NeuShadowDark, 14.dp, 4.dp, 7.dp)
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
                    .neumorphicRaised(NeuBg, NeuShadowLight, NeuShadowDark, 14.dp, 4.dp, 7.dp)
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
        val suppressIme = suppressSystemKeyboard
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .neumorphicRaised(NeuBg, NeuShadowLight, NeuShadowDark, 20.dp, 5.dp, 9.dp)
                .padding(6.dp),
        ) {
            AndroidView(
                factory = { ctx ->
                    SandboxWebView(ctx).apply {
                        this.suppressSystemKeyboard = suppressIme
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
                update = { wv ->
                    wv.suppressSystemKeyboard = suppressIme
                    if (suppressIme) {
                        val imm = wv.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.hideSoftInputFromWindow(wv.windowToken, 0)
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
    --bg: #d6dce5;
    --shadow-light: #f1f4f9;
    --shadow-dark: #a5aebc;
    --text: #1f2937;
    --accent: #4f46e5;
  }
  body {
    margin: 0; padding: 12px; font-family: system-ui, -apple-system, sans-serif;
    background: var(--bg); color: var(--text);
  }
  .badge {
    display: inline-block; padding: 5px 11px; border-radius: 999px; font-size: 11px; font-weight: 700;
    background: #cde0f8; color: var(--accent); border: 1px solid rgba(139,149,165,0.6);
  }
  .card {
    background: var(--bg); border-radius: 16px; padding: 12px; margin-top: 10px;
    border: 1px solid rgba(139,149,165,0.65);
    box-shadow: 5px 5px 10px var(--shadow-dark), -5px -5px 10px var(--shadow-light);
  }
  .arena {
    position: relative; height: 110px; border-radius: 12px; margin-top: 8px; overflow: hidden;
    background: #cfd6e2; border: 1px solid rgba(139,149,165,0.7);
    box-shadow: inset 3px 3px 6px var(--shadow-dark), inset -3px -3px 6px var(--shadow-light);
  }
  .cursor {
    position: absolute; width: 16px; height: 16px; border-radius: 50%;
    border: 2.5px solid #4f46e5; background: rgba(79,70,229,0.22);
    transform: translate(-50%, -50%); left: 50%; top: 50%; pointer-events: none;
  }
  .row { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 8px; }
  .pill {
    padding: 6px 10px; border-radius: 9px; background: #dce1ea; font-size: 11px; font-weight: 600;
    border: 1px solid rgba(139,149,165,0.65);
  }
  .pill.active { background: #cde0f8; color: var(--accent); border-color: #4f46e5; }
  .stage { display: flex; align-items: center; justify-content: space-between; margin-top: 4px; font-size: 13px; }
  .meter {
    height: 8px; background: #cfd6e2; border-radius: 99px; overflow: hidden; margin-top: 8px;
  }
  .meter-fill { height: 100%; width: 35%; background: #4f46e5; border-radius: 99px; transition: width 0.12s; }
  #log { font-size: 11px; color: #4b5563; max-height: 110px; overflow-y: auto; margin-top: 6px; }
  .evt { padding: 4px 0; border-bottom: 1px solid rgba(139,149,165,0.35); }
  .k { color: var(--accent); font-weight: 700; }
</style>
</head>
<body>
  <div class="badge">WEB + GAMING MODE TEST ARENA</div>
  <div class="card">
    <div class="stage">
      <strong id="playerState">WASD &amp; Mouse Aim Arena</strong>
      <span id="clickCounts" style="color:#4f46e5;font-weight:700">LMB: 0 • RMB: 0</span>
    </div>
    <div class="arena" id="arena">
      <div class="cursor" id="cursor"></div>
    </div>
    <div class="row">
      <div class="pill" id="k-w">W</div>
      <div class="pill" id="k-a">A</div>
      <div class="pill" id="k-s">S</div>
      <div class="pill" id="k-d">D</div>
      <div class="pill" id="k-space">Space</div>
      <div class="pill" id="k-shift">Shift</div>
      <div class="pill" id="tool-num">Num: <span id="numVal">-</span></div>
    </div>
    <div class="meter"><div id="fill" class="meter-fill"></div></div>
  </div>

  <div class="card">
    <strong>Live Key &amp; Mouse Events</strong>
    <div id="log"><div class="evt">Open Floating Deck → Game mode to test WASD hold &amp; Touchpad...</div></div>
  </div>

<script>
  let cx = 50, cy = 50, lmb = 0, rmb = 0, pos = 35;
  const cursor = document.getElementById('cursor');
  const clicksEl = document.getElementById('clickCounts');
  const log = document.getElementById('log');
  const fillEl = document.getElementById('fill');
  const numVal = document.getElementById('numVal');

  window.onKeyDeckMouseDelta = (dx, dy) => {
    cx = Math.max(5, Math.min(95, cx + dx * 0.25));
    cy = Math.max(8, Math.min(92, cy + dy * 0.35));
    cursor.style.left = cx + '%';
    cursor.style.top = cy + '%';
  };

  window.onKeyDeckMouseClick = (isRight) => {
    if (isRight) rmb++; else lmb++;
    clicksEl.textContent = 'LMB: ' + lmb + ' • RMB: ' + rmb;
    const row = document.createElement('div');
    row.className = 'evt';
    row.innerHTML = '<span class="k">' + (isRight ? 'RMB (Use/Place)' : 'LMB (Attack/Click)') + '</span> at (' + Math.round(cx) + '%, ' + Math.round(cy) + '%)';
    log.prepend(row);
    if (log.children.length > 14) log.removeChild(log.lastChild);
  };

  function toggleKeyPill(key, active) {
    const k = (key || '').toLowerCase();
    const map = { w: 'k-w', a: 'k-a', s: 'k-s', d: 'k-d', ' ': 'k-space', shift: 'k-shift' };
    const id = map[k];
    if (id) document.getElementById(id).classList.toggle('active', active);
  }

  window.addEventListener('keydown', (e) => {
    toggleKeyPill(e.key, true);
    if (/^[0-9]$/.test(e.key)) {
      numVal.textContent = e.key;
      pos = parseInt(e.key, 10) * 10;
      fillEl.style.width = pos + '%';
    }
    const row = document.createElement('div');
    row.className = 'evt';
    row.innerHTML = '<span class="k">DOWN ' + (e.key || e.code) + '</span> • keyCode=' + e.keyCode;
    log.prepend(row);
    if (log.children.length > 14) log.removeChild(log.lastChild);
  });

  window.addEventListener('keyup', (e) => {
    toggleKeyPill(e.key, false);
  });
</script>
</body>
</html>
""".trimIndent()
