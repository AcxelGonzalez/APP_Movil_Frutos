package com.example.oyl_fs.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.oyl_fs.ui.theme.EncabezadoOYL
import com.example.oyl_fs.ui.theme.MensajeErrorOYL
import com.example.oyl_fs.util.EstadoPermisosPerfil
import com.example.oyl_fs.util.PermisoCamaraPerfil
import com.example.oyl_fs.util.PermisoUbicacionPerfil
import com.example.oyl_fs.util.PermisosPerfil

@Composable
fun PermisosPerfilScreen(onVolver: () -> Unit) {
    val contexto = LocalContext.current
    val cicloDeVida = LocalLifecycleOwner.current.lifecycle
    var estado by remember { mutableStateOf<EstadoPermisosPerfil?>(null) }
    var errorConsulta by remember { mutableStateOf<String?>(null) }
    var errorAjustes by rememberSaveable { mutableStateOf<String?>(null) }
    fun actualizar() {
        try {
            estado = PermisosPerfil.consultar(contexto)
            errorConsulta = null
        } catch (_: Exception) {
            estado = null
            errorConsulta = "No se pudo consultar el estado de los permisos. Intenta nuevamente."
        }
    }
    DisposableEffect(cicloDeVida, contexto) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) actualizar()
        }
        cicloDeVida.addObserver(observador)
        actualizar()
        onDispose { cicloDeVida.removeObserver(observador) }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFFFF5E7))
        .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EncabezadoOYL("Permisos del dispositivo", onVolver)
        Text("Consulta cómo se usan la cámara y la ubicación en OYL FS.")
        val actual = estado
        if (actual != null) {
            val estadoCamara = when (actual.camara) {
                PermisoCamaraPerfil.NO_SOLICITADO -> "No solicitado por OYL FS"
                PermisoCamaraPerfil.PERMITIDO -> "Permitido"
                PermisoCamaraPerfil.NO_PERMITIDO -> "No permitido"
            }
            TarjetaPermisoPerfil("Cámara", estadoCamara,
                if (actual.camara == PermisoCamaraPerfil.NO_SOLICITADO)
                    "Las fotos se toman con la aplicación de cámara y los QR con el escáner de Google. Sus avisos y permisos se gestionan allí."
                else "Este estado corresponde al permiso de cámara de OYL FS. Otras aplicaciones pueden tener sus propios permisos.")
            val estadoUbicacion = when (actual.ubicacion) {
                PermisoUbicacionPerfil.NO_SOLICITADA -> "No solicitada por OYL FS"
                PermisoUbicacionPerfil.PRECISA -> "Ubicación precisa permitida"
                PermisoUbicacionPerfil.APROXIMADA -> "Ubicación aproximada permitida"
                PermisoUbicacionPerfil.NO_PERMITIDA -> "No permitida"
            }
            TarjetaPermisoPerfil("Ubicación", estadoUbicacion,
                if (actual.ubicacion == PermisoUbicacionPerfil.NO_SOLICITADA)
                    "El mapa muestra los puntos de retiro guardados y no consulta la ubicación de tu teléfono. Si abres otra aplicación de mapas, esa aplicación gestiona sus permisos."
                else "Este estado corresponde al permiso de ubicación de OYL FS. Puedes revisarlo en los ajustes de Android.")
        } else if (errorConsulta == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Consultando permisos…")
        }
        errorConsulta?.let { MensajeErrorOYL(it) }
        OutlinedButton(onClick = { actualizar() }, modifier = Modifier.fillMaxWidth()) {
            Text("Actualizar estado")
        }
        Button(onClick = {
            errorAjustes = null
            try {
                contexto.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", contexto.packageName, null)))
            } catch (_: ActivityNotFoundException) {
                errorAjustes = "No se pudieron abrir los ajustes. Busca OYL FS en Ajustes → Aplicaciones."
            } catch (_: SecurityException) {
                errorAjustes = "Android no permitió abrir los ajustes. Busca OYL FS en Ajustes → Aplicaciones."
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Ver ajustes de OYL FS") }
        errorAjustes?.let { MensajeErrorOYL(it) }
        Text("Android muestra los permisos que utiliza cada aplicación. El estado se actualiza al regresar a esta pantalla.",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TarjetaPermisoPerfil(titulo: String, estado: String, descripcion: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(estado, fontWeight = FontWeight.Bold)
            Text(descripcion)
        }
    }
}
