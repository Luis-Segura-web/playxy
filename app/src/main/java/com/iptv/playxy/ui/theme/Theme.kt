package com.iptv.playxy.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Dark Blue theme - Only theme for the app
 * Deep blue backgrounds with electric blue accents
 * High contrast for maximum readability
 */
private val DarkColorScheme = darkColorScheme(
    // Primary (Electric Blue)
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnSurface,
    
    // Secondary (Cyan)
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSurface,
    
    // Tertiary
    tertiary = DarkSecondary,
    onTertiary = DarkOnSecondary,
    tertiaryContainer = DarkSecondaryContainer,
    onTertiaryContainer = DarkOnSurface,
    
    // Backgrounds & Surfaces
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = DarkPrimary,
    
    // Status
    error = DarkError,
    onError = DarkOnPrimary,
    errorContainer = Color(0xFF5C1A1A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    // Outlines
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    
    // Inverse (for special UI elements)
    inverseSurface = Color(0xFFE8F0FF),
    inverseOnSurface = Color(0xFF0A0E27),
    inversePrimary = Color(0xFF2D7FDB),
    
    // Scrim (for dialogs/modals)
    scrim = Color(0xFF000000)
)

@Composable
fun PlayxyTheme(
    darkTheme: Boolean = true, // Always use dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme // Only dark theme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar to match background
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            // Set navigation bar to match surface
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.surface.toArgb()
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            // Dark appearance for status bar
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
