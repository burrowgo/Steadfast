package com.example.steadfast.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val OlivePrimaryLight = Color(0xFF4C662B)
val OliveOnPrimaryLight = Color(0xFFFFFFFF)
val OlivePrimaryContainerLight = Color(0xFFCDEDA3)
val OliveOnPrimaryContainerLight = Color(0xFF354E16)
val OliveSecondaryLight = Color(0xFF586249)
val OliveSecondaryContainerLight = Color(0xFFDCE7C8)
val OliveTertiaryLight = Color(0xFF386663)
val OliveTertiaryContainerLight = Color(0xFFBCECE7)
val OliveBackgroundLight = Color(0xFFF9FAEF)
val OliveSurfaceLight = Color(0xFFF9FAEF)
val OliveOnSurfaceLight = Color(0xFF1A1C16)
val OliveSurfaceVariantLight = Color(0xFFE1E4D5)
val OliveOutlineLight = Color(0xFF75796C)
val OliveErrorLight = Color(0xFFBA1A1A)

val OlivePrimaryDark = Color(0xFFB1D18A)
val OliveOnPrimaryDark = Color(0xFF1F3701)
val OlivePrimaryContainerDark = Color(0xFF354E16)
val OliveOnPrimaryContainerDark = Color(0xFFCDEDA3)
val OliveSecondaryDark = Color(0xFFBFCBAD)
val OliveSecondaryContainerDark = Color(0xFF404A33)
val OliveTertiaryDark = Color(0xFFA0D0CB)
val OliveTertiaryContainerDark = Color(0xFF1F4E4B)
val OliveBackgroundDark = Color(0xFF12140E)
val OliveSurfaceDark = Color(0xFF12140E)
val OliveOnSurfaceDark = Color(0xFFE2E3D8)
val OliveSurfaceVariantDark = Color(0xFF44483D)
val OliveOutlineDark = Color(0xFF8F9285)
val OliveErrorDark = Color(0xFFFFB4AB)

val LightColorScheme = lightColorScheme(
    primary = OlivePrimaryLight,
    onPrimary = OliveOnPrimaryLight,
    primaryContainer = OlivePrimaryContainerLight,
    onPrimaryContainer = OliveOnPrimaryContainerLight,
    secondary = OliveSecondaryLight,
    secondaryContainer = OliveSecondaryContainerLight,
    tertiary = OliveTertiaryLight,
    tertiaryContainer = OliveTertiaryContainerLight,
    background = OliveBackgroundLight,
    surface = OliveSurfaceLight,
    onSurface = OliveOnSurfaceLight,
    surfaceVariant = OliveSurfaceVariantLight,
    outline = OliveOutlineLight,
    error = OliveErrorLight
)

val DarkColorScheme = darkColorScheme(
    primary = OlivePrimaryDark,
    onPrimary = OliveOnPrimaryDark,
    primaryContainer = OlivePrimaryContainerDark,
    onPrimaryContainer = OliveOnPrimaryContainerDark,
    secondary = OliveSecondaryDark,
    secondaryContainer = OliveSecondaryContainerDark,
    tertiary = OliveTertiaryDark,
    tertiaryContainer = OliveTertiaryContainerDark,
    background = OliveBackgroundDark,
    surface = OliveSurfaceDark,
    onSurface = OliveOnSurfaceDark,
    surfaceVariant = OliveSurfaceVariantDark,
    outline = OliveOutlineDark,
    error = OliveErrorDark
)

data class RankColors(
    val accent: Color,
    val container: Color
)

val LightRankColors = RankColors(
    accent = Color(0xFF7A5900),
    container = Color(0xFFFFDEA0)
)

val DarkRankColors = RankColors(
    accent = Color(0xFFEFC24C),
    container = Color(0xFF5C4300)
)

val LocalRankColors = staticCompositionLocalOf { LightRankColors }
