package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1D1B20),
    surface = Color(0xFF211F26),
    onBackground = Color(0xFFFEF7FF),
    onSurface = Color(0xFFFEF7FF)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SleekPrimary,
    secondary = SleekContainer,
    tertiary = SleekActiveBg,
    background = SleekBackground,
    surface = SleekCardBg,
    onPrimary = SleekOnPrimary,
    onSecondary = SleekOnContainer,
    onBackground = SleekTextPrimary,
    onSurface = SleekTextPrimary,
    error = SleekError
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default so our custom branding is used
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
