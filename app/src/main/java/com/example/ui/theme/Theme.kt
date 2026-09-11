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

private val DarkColorScheme =
  darkColorScheme(
    primary = ShiftlyPrimaryDark,
    onPrimary = ShiftlyOnPrimaryDark,
    primaryContainer = ShiftlyPrimaryContainerDark,
    onPrimaryContainer = ShiftlyOnPrimaryContainerDark,
    secondary = ShiftlySecondaryDark,
    onSecondary = ShiftlyOnSecondaryDark,
    secondaryContainer = ShiftlySecondaryContainerDark,
    onSecondaryContainer = ShiftlyOnSecondaryContainerDark,
    tertiary = ShiftlyTertiaryDark,
    onTertiary = ShiftlyOnTertiaryDark,
    tertiaryContainer = ShiftlyTertiaryContainerDark,
    onTertiaryContainer = ShiftlyOnTertiaryContainerDark,
    background = ShiftlyBackgroundDark,
    onBackground = ShiftlyOnBackgroundDark,
    surface = ShiftlySurfaceDark,
    onSurface = ShiftlyOnSurfaceDark,
    surfaceVariant = ShiftlySurfaceVariantDark,
    onSurfaceVariant = ShiftlyOnSurfaceVariantDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ShiftlyPrimaryLight,
    onPrimary = ShiftlyOnPrimaryLight,
    primaryContainer = ShiftlyPrimaryContainerLight,
    onPrimaryContainer = ShiftlyOnPrimaryContainerLight,
    secondary = ShiftlySecondaryLight,
    onSecondary = ShiftlyOnSecondaryLight,
    secondaryContainer = ShiftlySecondaryContainerLight,
    onSecondaryContainer = ShiftlyOnSecondaryContainerLight,
    tertiary = ShiftlyTertiaryLight,
    onTertiary = ShiftlyOnTertiaryLight,
    tertiaryContainer = ShiftlyTertiaryContainerLight,
    onTertiaryContainer = ShiftlyOnTertiaryContainerLight,
    background = ShiftlyBackgroundLight,
    onBackground = ShiftlyOnBackgroundLight,
    surface = ShiftlySurfaceLight,
    onSurface = ShiftlyOnSurfaceLight,
    surfaceVariant = ShiftlySurfaceVariantLight,
    onSurfaceVariant = ShiftlyOnSurfaceVariantLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
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
