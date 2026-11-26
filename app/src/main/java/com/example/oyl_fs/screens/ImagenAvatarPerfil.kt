package com.example.oyl_fs.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.oyl_fs.model.PerfilUsuario
import com.example.oyl_fs.util.ArchivosAvatar
import com.example.oyl_fs.util.CargadorImagenProducto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ImagenAvatarPerfil(perfil: PerfilUsuario) {
    val contexto = LocalContext.current
    // Una cuenta o archivo distinto empieza sin el bitmap de la identidad anterior.
    key(perfil.id, perfil.avatarArchivo) {
        val foto by produceState<Bitmap?>(initialValue = null) {
            value = try {
                withContext(Dispatchers.IO) {
                    ArchivosAvatar(contexto).uri(perfil.avatarArchivo)?.let {
                        CargadorImagenProducto.cargar(contexto, it.toString())
                    }
                }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { null }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val bitmap = foto
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), "Avatar de ${perfil.nombre}",
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.PersonOutline,
                    if (perfil.avatarArchivo == null) "Avatar predeterminado" else "Avatar no disponible",
                    tint = Color(0xFF4B2E15), modifier = Modifier.size(44.dp))
            }
        }
    }
}
