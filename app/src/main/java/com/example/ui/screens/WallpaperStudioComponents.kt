package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyboard.BuiltInWallpapers
import com.example.keyboard.KeyDeckSettings
import com.example.keyboard.KeyboardWallpaper
import com.example.keyboard.KeyboardWallpaperBackdrop
import com.example.keyboard.ResolvedKeyboardPalette
import com.example.keyboard.WallpaperSourceType
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.KeyDeckColors
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised
import kotlin.math.roundToInt

private val WALLPAPER_TINT_SWATCHES = listOf(
    0L,            // Neutral / None
    0xFF10B981L,   // Emerald Green
    0xFF0D9488L,   // Teal
    0xFF38BDF8L,   // Sky Cyan
    0xFF3B82F6L,   // Classic Blue
    0xFF6366F1L,   // Indigo
    0xFF8B5CF6L,   // Violet
    0xFFE11D48L,   // Rose
    0xFFD97706L,   // Amber
    0xFF6B7280L,   // Neutral Gray
    0xFFFFFFFFL,   // Pure White
)

@Composable
fun MiniKeyboardPreviewCard(
    palette: ResolvedKeyboardPalette,
    cornerDp: Int,
    opacity: Float,
    showKeyBorder: Boolean,
    wallpaper: KeyboardWallpaper? = null,
    modifier: Modifier = Modifier,
) {
    val hasWallpaper = wallpaper != null && wallpaper.sourceType != WallpaperSourceType.NONE
    val keyOpacity = if (hasWallpaper) wallpaper!!.keyOpacity.coerceIn(0.15f, 1.0f) else 1f
    val effectivePalette = remember(palette, wallpaper) {
        if (wallpaper != null && wallpaper.sourceType != WallpaperSourceType.NONE && wallpaper.darkBackdrop) {
            palette.copy(
                deckBg = Color.Black,
                keyBg = if (palette.isDark) lerp(palette.keyBg, Color(0xFF0A0A0D), 0.55f) else Color(0xFF0E1015),
                modifierBg = if (palette.isDark) lerp(palette.modifierBg, Color(0xFF050608), 0.55f) else Color(0xFF151820),
                activeFill = lerp(Color(0xFF121620), palette.accentBg, 0.28f),
                shadowLight = Color(0xFF222630),
                shadowDark = Color(0xFF000000),
                primaryText = Color(0xFFF8FAFC),
                secondaryText = Color(0xFFCBD5E1),
                isDark = true,
            )
        } else {
            palette
        }
    }
    val corner = (cornerDp - 2).coerceIn(4, 14).dp
    val borderMod = if (showKeyBorder) {
        Modifier.border(1.dp, effectivePalette.keyBorder.copy(alpha = 0.88f), RoundedCornerShape(corner))
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .neumorphicRaised(
                surfaceColor = effectivePalette.deckBg.copy(alpha = opacity),
                shadowLight = effectivePalette.shadowLight,
                shadowDark = effectivePalette.shadowDark,
                cornerRadius = 16.dp,
                shadowOffset = 4.dp,
                blurRadius = 8.dp,
                borderColor = if (showKeyBorder) effectivePalette.keyBorder.copy(alpha = 0.65f) else null,
            ),
    ) {
        KeyboardWallpaperBackdrop(
            wallpaper = wallpaper,
            palette = effectivePalette,
            overlayAlpha = opacity,
            modifier = Modifier.matchParentSize(),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("ctrl", "q", "w", "e", "r", "t", "enter").forEachIndexed { idx, label ->
                    val isMod = idx == 0
                    val isAcc = label == "enter"
                    val baseCol = when {
                        isAcc -> effectivePalette.activeFill
                        isMod -> effectivePalette.modifierBg
                        else -> effectivePalette.keyBg
                    }
                    val fillCol = if (keyOpacity < 0.99f) {
                        baseCol.copy(alpha = (baseCol.alpha * if (isAcc) (keyOpacity + 0.18f).coerceAtMost(0.95f) else keyOpacity).coerceIn(0.14f, 1f))
                    } else {
                        baseCol
                    }
                    Box(
                        modifier = Modifier
                            .weight(if (isMod || isAcc) 1.5f else 1f)
                            .height(26.dp)
                            .then(
                                if (isAcc) {
                                    Modifier.neumorphicInset(
                                        surfaceColor = fillCol,
                                        shadowLight = effectivePalette.shadowLight,
                                        shadowDark = effectivePalette.shadowDark,
                                        cornerRadius = corner,
                                        depth = 2.5.dp,
                                    )
                                } else {
                                    Modifier.neumorphicRaised(
                                        surfaceColor = fillCol,
                                        shadowLight = effectivePalette.shadowLight,
                                        shadowDark = effectivePalette.shadowDark,
                                        cornerRadius = corner,
                                        shadowOffset = 2.dp,
                                        blurRadius = 4.dp,
                                    )
                                },
                            )
                            .then(borderMod),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            fontFamily = BodyFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isAcc) effectivePalette.accentBg else effectivePalette.primaryText,
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("shift", "a", "s", "space", "fn", "◁", "▷").forEachIndexed { idx, label ->
                    val isMod = idx == 0 || idx >= 4
                    val isSpace = label == "space"
                    val baseCol = if (isMod) effectivePalette.modifierBg else effectivePalette.keyBg
                    val fillCol = if (keyOpacity < 0.99f) {
                        baseCol.copy(alpha = (baseCol.alpha * keyOpacity).coerceIn(0.14f, 1f))
                    } else {
                        baseCol
                    }
                    Box(
                        modifier = Modifier
                            .weight(if (isSpace) 2.5f else 1f)
                            .height(24.dp)
                            .neumorphicRaised(
                                surfaceColor = fillCol,
                                shadowLight = effectivePalette.shadowLight,
                                shadowDark = effectivePalette.shadowDark,
                                cornerRadius = corner,
                                shadowOffset = 2.dp,
                                blurRadius = 4.dp,
                            )
                            .then(borderMod),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            fontFamily = BodyFontFamily,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (label == "fn") effectivePalette.activeIndicator else effectivePalette.primaryText,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperStudioSection(
    settings: KeyDeckSettings,
    palette: ResolvedKeyboardPalette,
    savedWallpapers: List<KeyboardWallpaper>,
    activeWallpaper: KeyboardWallpaper?,
    onSelectWallpaper: (String) -> Unit,
    onSaveWallpaper: (KeyboardWallpaper) -> Unit,
    onDeleteWallpaper: (String) -> Unit,
    onImportWallpaperImage: (Uri, (KeyboardWallpaper?) -> Unit) -> Unit,
    editingDraft: KeyboardWallpaper?,
    onEditingDraftChanged: (KeyboardWallpaper?) -> Unit,
) {
    val colors = KeyDeckColors.current
    var importError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            importError = null
            onImportWallpaperImage(uri) { draft ->
                if (draft != null) {
                    onEditingDraftChanged(draft)
                } else {
                    importError = "Could not load selected image. Try another photo or PNG."
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (editingDraft != null) {
            WallpaperFilterEditor(
                draft = editingDraft,
                onDraftChange = onEditingDraftChanged,
                onCancel = { onEditingDraftChanged(null) },
                onSave = {
                    onSaveWallpaper(editingDraft)
                    onEditingDraftChanged(null)
                },
            )
        } else {
            // Primary Gallery Photo / Logo Picker Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .neumorphicRaised(
                        surfaceColor = colors.accentFill,
                        shadowLight = colors.shadowLight,
                        shadowDark = colors.shadowDark,
                        cornerRadius = 14.dp,
                        shadowOffset = 4.dp,
                        blurRadius = 8.dp,
                        borderColor = null,
                    )
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }
                    .testTag("wallpaper_pick_image_button"),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp),
                    )
                    Text(
                        text = "Pick Image / Logo from Gallery",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }

            if (importError != null) {
                Text(
                    text = importError.orEmpty(),
                    color = colors.coral,
                    fontSize = 12.sp,
                )
            }

            // Quick Customize Active Wallpaper button
            if (activeWallpaper != null && activeWallpaper.sourceType != WallpaperSourceType.NONE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .neumorphicRaised(
                            surfaceColor = colors.bg,
                            shadowLight = colors.shadowLight,
                            shadowDark = colors.shadowDark,
                            cornerRadius = 12.dp,
                            shadowOffset = 3.dp,
                            blurRadius = 6.dp,
                            borderColor = colors.keyBorder.copy(alpha = 0.45f),
                        )
                        .clickable {
                            onEditingDraftChanged(
                                if (activeWallpaper.isBuiltIn) {
                                    activeWallpaper.copy(
                                        id = "draft_${System.currentTimeMillis()}",
                                        name = "${activeWallpaper.name} Custom",
                                        isBuiltIn = false,
                                    )
                                } else {
                                    activeWallpaper
                                },
                            )
                        }
                        .testTag("wallpaper_tune_active_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = colors.indigo,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Tune Filters (${activeWallpaper.name})",
                            color = colors.indigo,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                        )
                    }
                }
            }

            // Saved User Wallpapers List
            Text(
                text = "My Saved Wallpapers (${savedWallpapers.size})",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
            )

            if (savedWallpapers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphicInset(
                            surfaceColor = colors.bg,
                            shadowLight = colors.shadowLight,
                            shadowDark = colors.shadowDark,
                            cornerRadius = 14.dp,
                            depth = 3.dp,
                            borderColor = colors.keyBorder.copy(alpha = 0.35f),
                        )
                        .padding(14.dp),
                ) {
                    Text(
                        text = "Your created photo & pattern wallpapers appear here so you can switch, edit filters, or delete them anytime.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    savedWallpapers.forEach { wp ->
                        val isSelected = settings.activeWallpaperId == wp.id
                        SavedWallpaperRow(
                            wallpaper = wp,
                            palette = palette,
                            selected = isSelected,
                            onSelect = { onSelectWallpaper(wp.id) },
                            onEdit = { onEditingDraftChanged(wp) },
                            onDelete = { onDeleteWallpaper(wp.id) },
                        )
                    }
                }
            }

            // Built-In Pattern Wallpapers List
            Text(
                text = "Built-in Pattern Wallpapers",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
            )

            // None / Solid Theme Card
            val isNoneActive = settings.activeWallpaperId.isBlank()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isNoneActive) {
                            Modifier.neumorphicInset(
                                surfaceColor = colors.activeFill,
                                shadowLight = colors.shadowLight,
                                shadowDark = colors.shadowDark,
                                cornerRadius = 14.dp,
                                depth = 3.dp,
                                borderColor = colors.keyBorder.copy(alpha = 0.45f),
                            )
                        } else {
                            Modifier.neumorphicRaised(
                                surfaceColor = colors.bg,
                                shadowLight = colors.shadowLight,
                                shadowDark = colors.shadowDark,
                                cornerRadius = 14.dp,
                                shadowOffset = 3.dp,
                                blurRadius = 6.dp,
                                borderColor = colors.keyBorder.copy(alpha = 0.45f),
                            )
                        },
                    )
                    .clickable { onSelectWallpaper("") }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("wallpaper_option_none"),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.deckBg)
                                .border(1.dp, colors.shadowDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.HideImage,
                                contentDescription = null,
                                tint = palette.secondaryText,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "None (Solid Theme)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isNoneActive) colors.indigo else colors.textPrimary,
                            )
                            Text(
                                text = "Pure theme colors without wallpaper pattern",
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                            )
                        }
                    }
                    if (isNoneActive) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = colors.indigo,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BuiltInWallpapers.presets.forEach { preset ->
                    val isSelected = settings.activeWallpaperId == preset.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isSelected) {
                                    Modifier.neumorphicInset(
                                        surfaceColor = colors.activeFill,
                                        shadowLight = colors.shadowLight,
                                        shadowDark = colors.shadowDark,
                                        cornerRadius = 14.dp,
                                        depth = 3.dp,
                                        borderColor = colors.keyBorder.copy(alpha = 0.45f),
                                    )
                                } else {
                                    Modifier.neumorphicRaised(
                                        surfaceColor = colors.bg,
                                        shadowLight = colors.shadowLight,
                                        shadowDark = colors.shadowDark,
                                        cornerRadius = 14.dp,
                                        shadowOffset = 3.dp,
                                        blurRadius = 6.dp,
                                        borderColor = colors.keyBorder.copy(alpha = 0.45f),
                                    )
                                },
                            )
                            .clickable { onSelectWallpaper(preset.id) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("wallpaper_option_${preset.id}"),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 52.dp, height = 34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, colors.shadowDark.copy(alpha = 0.55f), RoundedCornerShape(8.dp)),
                                ) {
                                    KeyboardWallpaperBackdrop(
                                        wallpaper = preset,
                                        palette = palette,
                                        modifier = Modifier.matchParentSize(),
                                    )
                                }
                                Column {
                                    Text(
                                        text = preset.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) colors.indigo else colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${preset.sourceType.label} • ${(preset.lightness * 100).roundToInt()}% Light",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = colors.indigo,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            onEditingDraftChanged(
                                                preset.copy(
                                                    id = "draft_${System.currentTimeMillis()}",
                                                    name = "${preset.name} Custom",
                                                    isBuiltIn = false,
                                                ),
                                            )
                                        }
                                        .testTag("customize_pattern_${preset.id}"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Customize ${preset.name}",
                                        tint = colors.indigo,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedWallpaperRow(
    wallpaper: KeyboardWallpaper,
    palette: ResolvedKeyboardPalette,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = KeyDeckColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) {
                    Modifier.neumorphicInset(
                        surfaceColor = colors.activeFill,
                        shadowLight = colors.shadowLight,
                        shadowDark = colors.shadowDark,
                        cornerRadius = 14.dp,
                        depth = 3.dp,
                        borderColor = colors.keyBorder.copy(alpha = 0.45f),
                    )
                } else {
                    Modifier.neumorphicRaised(
                        surfaceColor = colors.bg,
                        shadowLight = colors.shadowLight,
                        shadowDark = colors.shadowDark,
                        cornerRadius = 14.dp,
                        shadowOffset = 3.dp,
                        blurRadius = 6.dp,
                        borderColor = colors.keyBorder.copy(alpha = 0.45f),
                    )
                },
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("saved_wallpaper_${wallpaper.id}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 54.dp, height = 36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, colors.shadowDark.copy(alpha = 0.55f), RoundedCornerShape(8.dp)),
                ) {
                    KeyboardWallpaperBackdrop(
                        wallpaper = wallpaper,
                        palette = palette,
                        modifier = Modifier.matchParentSize(),
                    )
                }
                Column {
                    Text(
                        text = wallpaper.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) colors.indigo else colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${(wallpaper.lightness * 100).roundToInt()}% Light • Sat ${(wallpaper.saturation * 100).roundToInt()}%",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = colors.indigo,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onEdit)
                        .testTag("edit_wallpaper_${wallpaper.id}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit ${wallpaper.name}",
                        tint = colors.indigo,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDelete)
                        .testTag("delete_wallpaper_${wallpaper.id}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete ${wallpaper.name}",
                        tint = colors.coral,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WallpaperFilterEditor(
    draft: KeyboardWallpaper,
    onDraftChange: (KeyboardWallpaper) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = KeyDeckColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicInset(
                surfaceColor = colors.bg,
                shadowLight = colors.shadowLight,
                shadowDark = colors.shadowDark,
                cornerRadius = 16.dp,
                depth = 4.dp,
                borderColor = colors.keyBorder.copy(alpha = 0.45f),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Live Wallpaper Filter Studio",
                style = MaterialTheme.typography.titleMedium,
                color = colors.indigo,
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel editing",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Name Input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphicRaised(
                    surfaceColor = colors.bg,
                    shadowLight = colors.shadowLight,
                    shadowDark = colors.shadowDark,
                    cornerRadius = 10.dp,
                    shadowOffset = 2.dp,
                    blurRadius = 4.dp,
                    borderColor = colors.keyBorder.copy(alpha = 0.35f),
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            BasicTextField(
                value = draft.name,
                onValueChange = { onDraftChange(draft.copy(name = it.take(32))) },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = BodyFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                ),
                cursorBrush = SolidColor(colors.indigo),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallpaper_name_input"),
                decorationBox = { inner ->
                    if (draft.name.isEmpty()) {
                        Text("Wallpaper name...", color = colors.textSecondary, fontSize = 13.sp)
                    }
                    inner()
                },
            )
        }

        // Quick Lightness Presets (90% Dark Black Look vs Balanced vs Vivid)
        Text("Quick Stealth Presets", fontSize = 11.5.sp, color = colors.textSecondary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                Triple("90% Dark", 0.14f, 0.62f),
                Triple("Balanced 35%", 0.35f, 0.56f),
                Triple("Vivid 75%", 0.75f, 0.46f),
            ).forEach { (label, lightVal, keyOp) ->
                val isMatch = kotlin.math.abs(draft.lightness - lightVal) < 0.04f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .then(
                            if (isMatch) {
                                Modifier.neumorphicInset(
                                    surfaceColor = colors.activeFill,
                                    shadowLight = colors.shadowLight,
                                    shadowDark = colors.shadowDark,
                                    cornerRadius = 10.dp,
                                    depth = 2.dp,
                                    borderColor = colors.keyBorder.copy(alpha = 0.45f),
                                )
                            } else {
                                Modifier.neumorphicRaised(
                                    surfaceColor = colors.bg,
                                    shadowLight = colors.shadowLight,
                                    shadowDark = colors.shadowDark,
                                    cornerRadius = 10.dp,
                                    shadowOffset = 2.dp,
                                    blurRadius = 4.dp,
                                    borderColor = colors.keyBorder.copy(alpha = 0.45f),
                                )
                            },
                        )
                        .clickable {
                            onDraftChange(
                                draft.copy(
                                    lightness = lightVal,
                                    keyOpacity = keyOp,
                                    darkBackdrop = lightVal < 0.50f,
                                ),
                            )
                        }
                        .testTag("wallpaper_preset_${label.lowercase().replace(" ", "_").replace("%", "")}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMatch) colors.indigo else colors.textPrimary,
                    )
                }
            }
        }

        // Live Filter Sliders
        WallpaperFilterSlider(
            label = "Wallpaper Lightness",
            valueText = "${(draft.lightness * 100).roundToInt()}%",
            value = draft.lightness,
            range = 0.04f..1.0f,
            accent = colors.indigo,
            onValueChange = { onDraftChange(draft.copy(lightness = it)) },
        )

        WallpaperFilterSlider(
            label = "Saturation",
            valueText = "${(draft.saturation * 100).roundToInt()}%",
            value = draft.saturation,
            range = 0.0f..2.0f,
            accent = colors.indigo,
            onValueChange = { onDraftChange(draft.copy(saturation = it)) },
        )

        WallpaperFilterSlider(
            label = "Contrast",
            valueText = "${(draft.contrast * 100).roundToInt()}%",
            value = draft.contrast,
            range = 0.45f..1.85f,
            accent = colors.emerald,
            onValueChange = { onDraftChange(draft.copy(contrast = it)) },
        )

        WallpaperFilterSlider(
            label = "Brightness",
            valueText = "${if (draft.brightness >= 0f) "+" else ""}${(draft.brightness * 100).roundToInt()}%",
            value = draft.brightness,
            range = -0.40f..0.40f,
            accent = colors.emerald,
            onValueChange = { onDraftChange(draft.copy(brightness = it)) },
        )

        WallpaperFilterSlider(
            label = "Keycap Opacity (Glass Look)",
            valueText = "${(draft.keyOpacity * 100).roundToInt()}%",
            value = draft.keyOpacity,
            range = 0.18f..1.0f,
            accent = colors.indigo,
            onValueChange = { onDraftChange(draft.copy(keyOpacity = it)) },
        )

        // Tint Swatches & Intensity
        Text("Color Tint", fontSize = 11.5.sp, color = colors.textSecondary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WALLPAPER_TINT_SWATCHES.forEach { hex ->
                val selected = draft.tintHex == hex
                val swatchCol = if (hex == 0L) colors.bg else Color(hex)
                val isLight = hex == 0xFFFFFFFFL || (hex == 0L && !colors.isDark)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(swatchCol)
                        .border(
                            width = if (selected) 2.5.dp else 1.dp,
                            color = if (selected) colors.indigo else colors.shadowDark.copy(alpha = 0.55f),
                            shape = CircleShape,
                        )
                        .clickable { onDraftChange(draft.copy(tintHex = hex)) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (hex == 0L) {
                        Text(
                            text = "Off",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                        )
                    } else if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected tint",
                            tint = if (isLight) colors.indigo else Color.White,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }

        if (draft.tintHex != 0L) {
            WallpaperFilterSlider(
                label = "Tint Intensity",
                valueText = "${(draft.tintIntensity * 100).roundToInt()}%",
                value = draft.tintIntensity,
                range = 0.0f..0.85f,
                accent = Color(draft.tintHex),
                onValueChange = { onDraftChange(draft.copy(tintIntensity = it)) },
            )
        }

        // Toggle: 90% Dark Black Backdrop
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDraftChange(draft.copy(darkBackdrop = !draft.darkBackdrop)) }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text("Dark Black Keyboard Backdrop", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Text("Keeps keyboard obsidian dark with soft glowing wallpaper", fontSize = 11.sp, color = colors.textSecondary)
            }
            Switch(
                checked = draft.darkBackdrop,
                onCheckedChange = { onDraftChange(draft.copy(darkBackdrop = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = colors.accentFill,
                ),
            )
        }

        if (draft.sourceType == WallpaperSourceType.CUSTOM_IMAGE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDraftChange(draft.copy(fitCenter = !draft.fitCenter)) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("Fit Logo Centered (vs Crop Fill)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text("Shows your whole logo centered without cropping edges", fontSize = 11.sp, color = colors.textSecondary)
                }
                Switch(
                    checked = draft.fitCenter,
                    onCheckedChange = { onDraftChange(draft.copy(fitCenter = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.accentFill,
                    ),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .neumorphicRaised(
                        surfaceColor = colors.bg,
                        shadowLight = colors.shadowLight,
                        shadowDark = colors.shadowDark,
                        cornerRadius = 12.dp,
                        shadowOffset = 3.dp,
                        blurRadius = 6.dp,
                        borderColor = colors.keyBorder.copy(alpha = 0.45f),
                    )
                    .clickable(onClick = onCancel)
                    .testTag("wallpaper_editor_cancel"),
                contentAlignment = Alignment.Center,
            ) {
                Text("Cancel", color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(48.dp)
                    .neumorphicRaised(
                        surfaceColor = colors.accentFill,
                        shadowLight = colors.shadowLight,
                        shadowDark = colors.shadowDark,
                        cornerRadius = 12.dp,
                        shadowOffset = 4.dp,
                        blurRadius = 8.dp,
                        borderColor = null,
                    )
                    .clickable(onClick = onSave)
                    .testTag("wallpaper_editor_save"),
                contentAlignment = Alignment.Center,
            ) {
                Text("Save & Apply", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
        }
    }
}

@Composable
private fun WallpaperFilterSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onValueChange: (Float) -> Unit,
) {
    val colors = KeyDeckColors.current
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, fontSize = 12.sp, color = colors.textPrimary)
            Text(valueText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = colors.shadowDark.copy(alpha = 0.45f),
            ),
        )
    }
}
