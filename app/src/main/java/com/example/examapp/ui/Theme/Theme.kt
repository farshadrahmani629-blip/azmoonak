package com.example.examapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5C9FF),
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF004689),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFBEC6DC),
    onSecondary = Color(0xFF283141),
    secondaryContainer = Color(0xFF3E4759),
    onSecondaryContainer = Color(0xFFDAE2F9),
    tertiary = Color(0xFFDEBCE0),
    onTertiary = Color(0xFF3F2842),
    tertiaryContainer = Color(0xFF573E5A),
    onTertiaryContainer = Color(0xFFFBD7FC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1B1B1D),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1B1B1D),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474F),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE3E2E6),
    inverseOnSurface = Color(0xFF303033),
    inversePrimary = Color(0xFF005DB3),
    surfaceTint = Color(0xFFA5C9FF),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF005DB3),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3D),
    secondary = Color(0xFF565E71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDAE2F9),
    onSecondaryContainer = Color(0xFF131B2C),
    tertiary = Color(0xFF705574),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFBD7FC),
    onTertiaryContainer = Color(0xFF29132E),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1B1B1D),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1B1B1D),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF303033),
    inverseOnSurface = Color(0xFFF2F0F4),
    inversePrimary = Color(0xFFA5C9FF),
    surfaceTint = Color(0xFF005DB3),

    // Custom colors for our app
    /* Additional colors for our app */
)

@Composable
fun AzmoonakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Typography
val Typography = Typography(
    displayLarge = androidx.compose.material3.MaterialTheme.typography.displayLarge.copy(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        lineHeight = 64.sp,
        fontSize = 57.sp
    ),
    displayMedium = androidx.compose.material3.MaterialTheme.typography.displayMedium.copy(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        lineHeight = 52.sp,
        fontSize = 45.sp
    ),
    displaySmall = androidx.compose.material3.MaterialTheme.typography.displaySmall.copy(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        lineHeight = 44.sp,
        fontSize = 36.sp
    ),
    headlineLarge = androidx.compose.material3.MaterialTheme.typography.headlineLarge.copy(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        lineHeight = 40.sp,
        fontSize = 32.sp
    ),
    headlineMedium = androidx.compose.material3.MaterialTheme.typography.headlineMedium.copy(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        lineHeight = 36.sp,
        fontSize = 28.sp
    ),
    headlineSmall = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        lineHeight = 32.sp,
        fontSize = 24.sp
    ),
    titleLarge = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        lineHeight = 28.sp,
        fontSize = 22.sp
    ),
    titleMedium = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        lineHeight = 24.sp,
        fontSize = 16.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
    ),
    titleSmall = androidx.compose.material3.MaterialTheme.typography.titleSmall.copy(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        lineHeight = 20.sp,
        fontSize = 14.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
    ),
    bodyLarge = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        lineHeight = 24.sp,
        fontSize = 16.sp
    ),
    bodyMedium = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        lineHeight = 20.sp,
        fontSize = 14.sp
    ),
    bodySmall = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        lineHeight = 16.sp,
        fontSize = 12.sp
    ),
    labelLarge = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        lineHeight = 20.sp,
        fontSize = 14.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    ),
    labelMedium = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        lineHeight = 16.sp,
        fontSize = 12.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    ),
    labelSmall = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        lineHeight = 16.sp,
        fontSize = 11.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    )
)