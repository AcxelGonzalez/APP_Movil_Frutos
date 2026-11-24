package com.example.oyl_fs.screens


import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.util.CargadorImagenProducto
import kotlinx.coroutines.CancellationException

// Muestra recursos, fotografías del teléfono e imágenes HTTP/HTTPS del catálogo.
@Composable
fun ImagenProducto(
    producto: ProductoInfo,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val uri = producto.imagenUri

    val foto by produceState<Bitmap?>(
        initialValue = null,
        key1 = uri
    ) {
        value = null

        if (!uri.isNullOrBlank()) {
            value = try {
                CargadorImagenProducto.cargar(contexto, uri)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) { null }
        }
    }

    val bitmap = foto

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (!uri.isNullOrBlank()) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = producto.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Foto no disponible")
            }
        } else if (producto.imagen != 0) {
            Image(
                painter = painterResource(producto.imagen),
                contentDescription = producto.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text("Sin imagen")
        }
    }
}
