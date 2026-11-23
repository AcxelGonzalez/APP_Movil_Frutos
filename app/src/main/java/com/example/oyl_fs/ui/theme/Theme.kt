package com.example.oyl_fs.ui.theme

import androidx.compose.foundation.layout.defaultMinSize

import android.app.Activity
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ArrowBack
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
    surfaceTint = Color.White,
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
    MaterialTheme(
        colorScheme = ColoresOYL,
        typography = Typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(30.dp)
        ),
        content = content
    )
}

// Animación visual compartida: no modifica el estado ni la navegación.
@Composable
fun Modifier.entradaOYL(clave: Any? = Unit): Modifier {
    val previa = LocalInspectionMode.current
    val avance = remember(clave) { Animatable(if (previa) 1f else 0f) }
    val distancia = with(LocalDensity.current) { 26.dp.toPx() }
    LaunchedEffect(clave) {
        if (!previa) {
            withFrameNanos { }
            avance.animateTo(1f, tween(650))
        }
    }
    return graphicsLayer {
        alpha = avance.value
        translationY = distancia * (1f - avance.value)
    }
}

// Respuesta táctil de las tarjetas, conservando su mismo evento onClick.
@Composable
fun Modifier.presionOYL(interacciones: MutableInteractionSource): Modifier {
    val presionada by interacciones.collectIsPressedAsState()
    val escala by animateFloatAsState(if (presionada) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 450f))
    return graphicsLayer { scaleX = escala; scaleY = escala }
}

// Presentación uniforme de los errores; no contiene reglas de negocio.
@Composable
fun MensajeErrorOYL(mensaje: String) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFCECE5), contentColor = Color(0xFF8C3529),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0CCBF)),
        shape = RoundedCornerShape(14.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier.padding(14.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
        ) {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Default.ErrorOutline,
                contentDescription = null, modifier = Modifier.size(20.dp)
            )
            androidx.compose.material3.Text(mensaje,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun BotonVolverOYL(onVolver: () -> Unit) {
    // El botón se ve más pequeño y mantiene espacio cómodo para pulsarlo.
    androidx.compose.foundation.layout.Box(Modifier.size(48.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center) {
        androidx.compose.material3.OutlinedButton(
            onClick = onVolver, modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE3D0B8)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White, contentColor = Color(0xFF4B2E15))
        ) {
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.ArrowBack,
                contentDescription = "Volver", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun TituloPantallaOYL(titulo: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier.defaultMinSize(minHeight = 40.dp).padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = androidx.compose.ui.Alignment.CenterStart
        ) {
            androidx.compose.material3.Text(titulo,
                color = Color(0xFF4B2E15), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.3).sp)
        }
    }
}

@Composable
fun EncabezadoOYL(titulo: String, onVolver: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        BotonVolverOYL(onVolver)
        TituloPantallaOYL(titulo, Modifier.weight(1f))
    }
}
