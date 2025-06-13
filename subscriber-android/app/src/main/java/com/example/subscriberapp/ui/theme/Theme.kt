package com.example.subscriberapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Primary80,
    primaryContainer = PrimaryContainer80,
    secondary = Secondary80,
    secondaryContainer = SecondaryContainer80,
    tertiary = Tertiary80,
    tertiaryContainer = TertiaryContainer80,
    error = Error80,
    errorContainer = ErrorContainer80,
    background = androidx.compose.ui.graphics.Color(0xFF121212),
    surface = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2C2C2C),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onPrimaryContainer = androidx.compose.ui.graphics.Color.Black,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onSecondaryContainer = androidx.compose.ui.graphics.Color.Black,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    onTertiaryContainer = androidx.compose.ui.graphics.Color.Black,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB0B0B0)
)

private val LightColorScheme = lightColorScheme(
    primary = Primary40,
    primaryContainer = PrimaryContainer80,
    secondary = Secondary40,
    secondaryContainer = SecondaryContainer80,
    tertiary = Tertiary40,
    tertiaryContainer = TertiaryContainer80,
    error = Error40,
    errorContainer = ErrorContainer80,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onPrimaryContainer = Primary40,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onSecondaryContainer = Secondary40,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    onTertiaryContainer = Tertiary40,
    onBackground = OnBackground,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant
)

@Composable
fun SubscriberAppTheme(
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
