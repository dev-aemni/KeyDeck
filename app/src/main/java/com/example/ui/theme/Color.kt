package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Soft UI Neumorphic Palette (matching --bg: #e0e5ec, --shadow-light: #ffffff, --shadow-dark: #b9bcc6, --accent: #4f46e5)
val NeuBg = Color(0xFFE0E5EC)
val NeuSurface = Color(0xFFE0E5EC)
val NeuSurfaceElevated = Color(0xFFE8ECF2)
val NeuShadowLight = Color(0xFFFFFFFF)
val NeuShadowDark = Color(0xFFB9BCC6)
val NeuActiveFill = Color(0xFFD7E3F4)

val NeuIndigo = Color(0xFF4F46E5)
val NeuIndigoSoft = Color(0xFF6366F1)
val NeuEmerald = Color(0xFF10B981)
val NeuAmber = Color(0xFFF59E0B)
val NeuCoral = Color(0xFFEF4444)

val NeuTextPrimary = Color(0xFF374151)
val NeuTextSecondary = Color(0xFF6B7280)
val NeuTextMuted = Color(0xFF9CA3AF)

// Legacy aliases kept mapped harmoniously for secondary components
val MidnightBg = NeuBg
val MidnightSurface = NeuSurface
val NavyCard = NeuSurface
val NavyCardElevated = NeuActiveFill
val NavyBorder = NeuShadowDark.copy(alpha = 0.45f)
val DeepPillNavy = NeuIndigo

val CoralHero = NeuIndigo
val CoralHeroPressed = NeuIndigoSoft
val CyanAccent = NeuIndigo
val EmeraldActive = NeuEmerald
val AmberWarm = NeuAmber

val TextPrimary = NeuTextPrimary
val TextSecondary = NeuTextSecondary
val TextMuted = NeuTextMuted
