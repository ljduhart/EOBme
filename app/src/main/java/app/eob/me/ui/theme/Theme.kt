package app.eob.me.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CyberColorScheme = darkColorScheme(
    primary = EobCyberAccent,
    onPrimary = EobCyberBackground,
    primaryContainer = EobCyberAccent.copy(alpha = 0.18f),
    onPrimaryContainer = EobCyberAccentBright,
    secondary = EobCyberAccentBright,
    onSecondary = EobCyberBackground,
    secondaryContainer = EobCyberGlow.copy(alpha = 0.14f),
    onSecondaryContainer = EobCyberTextPrimary,
    tertiary = EobCyberGlow,
    onTertiary = EobCyberBackground,
    background = EobCyberBackground,
    onBackground = EobCyberTextPrimary,
    surface = EobCyberSurface,
    onSurface = EobCyberTextPrimary,
    surfaceVariant = EobCyberSurfaceVariant,
    onSurfaceVariant = EobCyberTextSecondary,
    outline = EobCyberGlassBorder,
    outlineVariant = EobCyberGlassFill,
    error = EobCyberError,
    onError = EobCyberTextPrimary,
    errorContainer = EobCyberError.copy(alpha = 0.18f),
    onErrorContainer = EobCyberError
)

private val LightColorScheme = lightColorScheme(
    primary = EobCyberAccent,
    onPrimary = EobLightBackground,
    primaryContainer = EobCyberAccent.copy(alpha = 0.12f),
    onPrimaryContainer = EobLightTextPrimary,
    secondary = EobCyberAccentBright,
    onSecondary = EobLightBackground,
    secondaryContainer = EobLightSurfaceVariant,
    onSecondaryContainer = EobLightTextPrimary,
    tertiary = EobCyberGlow,
    onTertiary = EobLightBackground,
    background = EobLightBackground,
    onBackground = EobLightTextPrimary,
    surface = EobLightSurface,
    onSurface = EobLightTextPrimary,
    surfaceVariant = EobLightSurfaceVariant,
    onSurfaceVariant = EobLightTextSecondary,
    outline = EobLightOutline,
    error = EobCyberError,
    onError = EobLightBackground
)

@Composable
fun EOBmeTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> CyberColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
