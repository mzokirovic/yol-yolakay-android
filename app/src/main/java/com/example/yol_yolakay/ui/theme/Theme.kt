package com.example.yol_yolakay.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    // Brand
    primary = BrandBlueDark,
    onPrimary = Color(0xFF081028),
    primaryContainer = Color(0xFF1D3A8A),
    onPrimaryContainer = TextPrimaryDark,

    secondary = BrandBlueDark,
    onSecondary = Color(0xFF081028),
    secondaryContainer = Color(0xFF1A2B55),
    onSecondaryContainer = TextPrimaryDark,

    // Accent (rating star, warnings)
    tertiary = Warning,
    onTertiary = Color(0xFF2A1D00),
    tertiaryContainer = Color(0xFF5A3A00),
    onTertiaryContainer = TextPrimaryDark,

    // Surfaces
    background = BgDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,

    // Outlines
    outline = OutlineDark,
    outlineVariant = Color(0xFF2B3A55),

    // Error
    error = Danger,
    onError = Color.White,
    errorContainer = Color(0xFF4C1D1D),
    onErrorContainer = TextPrimaryDark,

    // Useful for snackbars / elevated surfaces
    inverseSurface = Color(0xFFE5E7EB),
    inverseOnSurface = Color(0xFF0F172A),
    inversePrimary = BrandBlue,

    scrim = Color.Black
)

private val LightColorScheme = lightColorScheme(
    // Brand
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = TextPrimaryLight,

    secondary = BrandBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAF0FF),
    onSecondaryContainer = TextPrimaryLight,

    // Accent
    tertiary = Warning,
    onTertiary = Color(0xFF2A1D00),
    tertiaryContainer = Color(0xFFFFE7B6),
    onTertiaryContainer = Color(0xFF2A1D00),

    // Surfaces
    background = BgLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,

    // Outlines
    outline = OutlineLight,
    outlineVariant = Color(0xFFCBD5E1),

    // Error
    error = Danger,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),

    inverseSurface = Color(0xFF0F172A),
    inverseOnSurface = Color(0xFFE5E7EB),
    inversePrimary = BrandBlueDark,

    scrim = Color.Black
)

@Composable
fun YolYolakayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme =
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}