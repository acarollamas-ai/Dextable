package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = DesktableCyanLight,
    onPrimary = Color(0xFF003544),
    primaryContainer = Color(0xFF004D63),
    onPrimaryContainer = Color(0xFFBBE9FF),
    secondary = DesktableVioletSecondary,
    onSecondary = Color(0xFF1E1B4B),
    background = DesktableDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = DesktableDarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DesktableDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = DesktableDarkBorder,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = DesktableCyanDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = DesktableIndigoDark,
    onSecondary = Color.White,
    background = DesktableLightBg,
    onBackground = Color(0xFF0F172A),
    surface = DesktableLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = DesktableLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = DesktableLightBorder,
  )

@Composable
fun DesktableOSTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

// Keep backwards-compatible alias for existing references
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  DesktableOSTheme(darkTheme = darkTheme, content = content)
}

