package com.example.oyl_fs.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Paleta fija de OYL: no depende del modo oscuro ni del fondo del teléfono.
private val ColoresOYL = lightColorScheme(
    primary = Color(0xFF4B2E15),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF2E3CF),
    onPrimaryContainer = Color(0xFF4B2E15),
    secondary = Color(0xFF92623D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2E3CF),
    onSecondaryContainer = Color(0xFF4B2E15),
    tertiary = Color(0xFF8A5428),
    onTertiary = Color.White,
    background = Color(0xFFFFF5E7),
    onBackground = Color(0xFF4B2E15),
    surface = Color.White,
    onSurface = Color(0xFF4B2E15),
    surfaceVariant = Color(0xFFF8EDDF),
    onSurfaceVariant = Color(0xFF785334),
    outline = Color(0xFF92623D),
    error = Color(0xFFC0392B),
    onError = Color.White,
    errorContainer = Color(0xFFFCE5E1),
    onErrorContainer = Color(0xFF7E251B),
    inverseSurface = Color(0xFF4B2E15),
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFFE4C29F)
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun OYL_FSTheme(
    // Se conservan los parámetros para no romper llamadas de otras pantallas.
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                window.statusBarColor = ColoresOYL.primary.toArgb()
                window.navigationBarColor = Color.White.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = true
                }
            }
        }
    }
    MaterialTheme(colorScheme = ColoresOYL, typography = Typography, content = content)
}
