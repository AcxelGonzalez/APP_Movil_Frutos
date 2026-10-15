package com.example.oyl_fs.screens


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Muestra una fotografía de drawable o una imagen del teléfono.
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
            value = withContext(Dispatchers.IO) {
                // Reduce el tamaño de la foto para no cargarla completa.
                runCatching {
                    val direccion = Uri.parse(uri)
                    val opciones = BitmapFactory.Options()
                    opciones.inJustDecodeBounds = true

                    contexto.contentResolver
                        .openInputStream(direccion)?.use {
                            BitmapFactory.decodeStream(it, null, opciones)
                        }

                    opciones.inSampleSize = 1

                    while (
                        opciones.outWidth / opciones.inSampleSize > 1000 ||
                        opciones.outHeight / opciones.inSampleSize > 1000
                    ) {
                        opciones.inSampleSize *= 2
                    }

                    opciones.inJustDecodeBounds = false

                    contexto.contentResolver
                        .openInputStream(direccion)?.use {
                            BitmapFactory.decodeStream(it, null, opciones)
                        }
                }.getOrNull()
            }
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