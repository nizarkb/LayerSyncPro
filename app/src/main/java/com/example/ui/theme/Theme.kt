package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Teal600,
    onPrimary = Slate900,
    secondary = Teal100,
    onSecondary = Slate900,
    tertiary = Teal700,
    background = Slate900,
    surface = Slate900,
    onBackground = Slate100,
    onSurface = Slate100,
    surfaceVariant = Slate600,
    onSurfaceVariant = Slate200
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Teal900,
    onPrimary = Color.White,
    secondary = Teal700,
    onSecondary = Color.White,
    tertiary = Teal600,
    background = Slate100,
    surface = Color.White,
    onBackground = Slate900,
    onSurface = Slate900,
    surfaceVariant = Slate200,
    onSurfaceVariant = Slate600
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Set false to ensure professional branding
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
