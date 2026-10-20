package com.lemomation.lucidlogger.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LucidColorScheme = darkColorScheme(
    primary = LucidPrimary,
    onPrimary = LucidText,
    primaryContainer = LucidSurface3,
    onPrimaryContainer = LucidPrimarySoft,
    secondary = LucidAmber,
    onSecondary = LucidOnAmber,
    secondaryContainer = LucidAmberContainer,
    onSecondaryContainer = LucidAmber,
    tertiary = LucidLocation,
    onTertiary = LucidText,
    background = LucidCanvas,
    onBackground = LucidText,
    surface = LucidSurface,
    onSurface = LucidText,
    surfaceVariant = LucidSurface2,
    onSurfaceVariant = LucidTextVariant,
    outline = LucidOutline,
    outlineVariant = LucidBorderStrong,
    error = LucidError,
    onError = LucidOnError,
    errorContainer = LucidErrorContainer,
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = LucidCanvas,
)

@Composable
fun LucidLoggerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = LucidCanvas.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(
        colorScheme = LucidColorScheme,
        typography = LucidTypography,
        content = content,
    )
}
