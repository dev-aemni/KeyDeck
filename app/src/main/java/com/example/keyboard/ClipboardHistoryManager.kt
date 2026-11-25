package com.example.keyboard

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicKeyCap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

data class ClipboardClipEntry(
    val id: Long,
    val text: String,
    val preview: String,
    val capturedAtMs: Long,
)

/**
 * In-memory, privacy-aware rolling clipboard history store.
 * Never persists clips to disk and ignores internal KeyDeck temp clips or sensitive clips.
 */
object ClipboardHistoryStore {

    private const val MAX_ENTRIES = 12
    private const val MAX_CLIP_LENGTH = 4000
    private const val INTERNAL_KEYDECK_CLIP_LABEL = "KeyDeckInput"

    private val nextId = AtomicLong(1L)
    private val listenerAttached = AtomicBoolean(false)
    private val _clips = MutableStateFlow<List<ClipboardClipEntry>>(emptyList())
    val clips: StateFlow<List<ClipboardClipEntry>> = _clips.asStateFlow()

    fun ensureListening(context: Context) {
        val appCtx = context.applicationContext ?: context
        if (!listenerAttached.compareAndSet(false, true)) {
            syncFromSystemClipboard(appCtx)
            return
        }
        val cm = runCatching {
            appCtx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        }.getOrNull() ?: return
        runCatching {
            cm.addPrimaryClipChangedListener {
                syncFromSystemClipboard(appCtx)
            }
            syncFromSystemClipboard(appCtx)
        }
    }

    fun syncFromSystemClipboard(context: Context) {
        val cm = runCatching {
            context.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        }.getOrNull() ?: return
        runCatching {
            val clip = cm.primaryClip ?: return
            val desc = clip.description
            val label = desc?.label?.toString()
            val isSensitive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                desc?.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false) == true
            } else {
                false
            }
            if (clip.itemCount <= 0) return
            val text = clip.getItemAt(0)?.coerceToText(context)?.toString()
            recordClip(rawText = text, isSensitive = isSensitive, label = label)
        }
    }

    fun recordClip(
        rawText: String?,
        isSensitive: Boolean = false,
        label: String? = null,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        if (isSensitive) return
        if (label == INTERNAL_KEYDECK_CLIP_LABEL) return
        val normalized = rawText ?: return
        if (normalized.isBlank()) return
        val capped = if (normalized.length > MAX_CLIP_LENGTH) {
            normalized.take(MAX_CLIP_LENGTH)
        } else {
            normalized
        }
        val preview = capped
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(36)
            .ifBlank { capped.trim().take(24) }

        val updated = buildList {
            add(
                ClipboardClipEntry(
                    id = nextId.getAndIncrement(),
                    text = capped,
                    preview = preview,
                    capturedAtMs = nowMs,
                ),
            )
            _clips.value.forEach { existing ->
                if (existing.text != capped && size < MAX_ENTRIES) {
                    add(existing)
                }
            }
        }
        _clips.value = updated
    }

    fun clearAll() {
        _clips.value = emptyList()
    }
}

@Composable
fun KeyboardClipboardHistoryStrip(
    clips: List<ClipboardClipEntry>,
    palette: ResolvedKeyboardPalette,
    borderColor: androidx.compose.ui.graphics.Color?,
    onPasteClip: (ClipboardClipEntry) -> Unit,
    onClearAll: () -> Unit,
    shadowsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 2.dp)
            .testTag("deck_clipboard_strip"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (clips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 32.dp)
                    .neumorphicInset(
                        surfaceColor = palette.modifierBg.copy(alpha = 0.72f),
                        shadowLight = palette.shadowLight,
                        shadowDark = palette.shadowDark,
                        cornerRadius = 9.dp,
                        depth = 2.dp,
                        borderColor = borderColor,
                        shadowsEnabled = shadowsEnabled,
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("clipboard_empty_hint"),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        tint = palette.secondaryText,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = "Copy text anywhere — recent non-sensitive clips appear here",
                        fontFamily = BodyFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            return@Row
        }

        Row(
            modifier = Modifier
                .heightIn(min = 32.dp)
                .neumorphicInset(
                    surfaceColor = palette.activeFill,
                    shadowLight = palette.shadowLight,
                    shadowDark = palette.shadowDark,
                    cornerRadius = 8.dp,
                    depth = 2.dp,
                    borderColor = borderColor,
                    shadowsEnabled = shadowsEnabled,
                )
                .clickable(onClick = onClearAll)
                .padding(horizontal = 9.dp, vertical = 5.dp)
                .testTag("clipboard_clear_all"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = "Clear clipboard history",
                tint = palette.accentBg,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = "Clear",
                fontFamily = BodyFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = palette.accentBg,
                maxLines = 1,
            )
        }

        clips.forEachIndexed { idx, entry ->
            Row(
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .widthIn(max = 180.dp)
                    .neumorphicKeyCap(
                        surfaceColor = palette.keyBg,
                        shadowLight = palette.shadowLight,
                        shadowDark = palette.shadowDark,
                        cornerRadius = 8.dp,
                        borderColor = borderColor,
                        shadowsEnabled = shadowsEnabled,
                    )
                    .clickable { onPasteClip(entry) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("clipboard_item_$idx"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste clip",
                    tint = palette.accentBg,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    text = entry.preview,
                    fontFamily = BodyFontFamily,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
