package com.example.kotlinlibraryapp.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = MintDark,
    secondary = Clay,
    onSecondary = Color.White,
    secondaryContainer = Peach,
    onSecondaryContainer = Clay,
    tertiary = SlateBlue,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE7E4DA),
    onSurfaceVariant = Muted,
    surfaceContainer = Color.White,
    outline = Color(0xFFB9B6AB),
    outlineVariant = Color(0xFFDDDACF)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = Forest,
    onPrimaryContainer = Sage,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = Color(0xFF5A3017),
    onSecondaryContainer = Peach,
    tertiary = Color(0xFFB4C6E8),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF26352F),
    onSurfaceVariant = Color(0xFFB5C2BB),
    surfaceContainer = DarkSurfaceContainer,
    outline = Color(0xFF7A8782),
    outlineVariant = Color(0xFF3A4A44)
)

@Composable
fun KotlinLibraryAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so the brand palette shows; set true to follow the wallpaper on Android 12+.
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
