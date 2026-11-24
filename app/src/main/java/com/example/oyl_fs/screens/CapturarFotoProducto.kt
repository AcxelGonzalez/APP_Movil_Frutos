package com.example.oyl_fs.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/** Delega la captura a la cámara del teléfono sin solicitar acceso general al almacenamiento. */
@Composable
fun CapturarFotoProducto(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onFoto: (String) -> Unit,
    onError: (String) -> Unit
) {
    val contexto = LocalContext.current
    var pendiente by rememberSaveable { mutableStateOf<String?>(null) }
    val capturar = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { completada ->
        val nombre = pendiente
        pendiente = null
        if (nombre != null) {
            val archivo = File(File(contexto.filesDir, "fotos_productos"), nombre)
            if (completada && archivo.length() > 0) {
                val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fotos", archivo)
                onFoto(uri.toString())
            } else {
                archivo.delete()
                if (completada) onError("La cámara no guardó una fotografía. Inténtalo nuevamente.")
            }
        }
    }
    OutlinedButton(
        enabled = enabled && pendiente == null,
        modifier = modifier,
        onClick = {
            var archivo: File? = null
            try {
                val carpeta = File(contexto.filesDir, "fotos_productos")
                check(carpeta.isDirectory || carpeta.mkdirs())
                archivo = File.createTempFile("producto_", ".jpg", carpeta)
                val uri: Uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fotos", archivo)
                pendiente = archivo.name
                capturar.launch(uri)
            } catch (_: Exception) {
                pendiente = null
                archivo?.delete()
                onError("No se pudo abrir la cámara. Puedes seleccionar una imagen de la galería.")
            }
        }
    ) { Text("Tomar foto") }
}
