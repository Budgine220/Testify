package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val HighDensityColorScheme = darkColorScheme(
    primary = DensePrimary,
    secondary = DensePrimaryContainer,
    tertiary = DenseOnPrimaryContainer,
    background = DenseBg,
    surface = DenseSurface,
    onBackground = DenseTextPrimary,
    onSurface = DenseTextPrimary,
    onPrimary = Color.Black,
    onSecondary = DenseTextPrimary,
    error = DenseRed,
    onError = Color.White,
    outline = DenseOutline,
    surfaceVariant = DenseContainer,
    onSurfaceVariant = DenseTextSecondary
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // High Density theme is optimized for the light/clean clinical diagnostic look
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve customized high density aesthetic
  content: @Composable () -> Unit,
) {
  val colorScheme = HighDensityColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
