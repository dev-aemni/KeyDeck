package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Eye-friendly Soft UI Neumorphic Palette (muted brightness, crisp slate-gray borders)
val NeuBg = Color(0xFFD6DCE5)
val NeuSurface = Color(0xFFDCE1EA)
val NeuSurfaceElevated = Color(0xFFE3E7EE)
val NeuShadowLight = Color(0xFFF3F6FA)
val NeuShadowDark = Color(0xFFAAB2C0)
val NeuKeyBorder = Color(0xFF8B95A5)
val NeuActiveFill = Color(0xFFD0DCF0)

val NeuIndigo = Color(0xFF4F46E5)
val NeuIndigoSoft = Color(0xFF6366F1)
val NeuEmerald = Color(0xFF059669)
val NeuAmber = Color(0xFFD97706)
val NeuCoral = Color(0xFFE11D48)

val NeuTextPrimary = Color(0xFF1F2937)
val NeuTextSecondary = Color(0xFF4B5563)
val NeuTextMuted = Color(0xFF6B7280)

// Compatibility aliases
val MidnightBg = NeuBg
val MidnightSurface = NeuSurface
val NavyCard = NeuSurface
val NavyCardElevated = NeuActiveFill
val NavyBorder = NeuKeyBorder.copy(alpha = 0.55f)
val DeepPillNavy = NeuIndigo

val CoralHero = NeuIndigo
val CoralHeroPressed = NeuIndigoSoft
val CyanAccent = NeuIndigo
val EmeraldActive = NeuEmerald
val AmberWarm = NeuAmber

val TextPrimary = NeuTextPrimary
val TextSecondary = NeuTextSecondary
val TextMuted = NeuTextMuted
