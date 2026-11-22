package com.example.oyl_fs.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object CargadorImagenProducto {
    private const val MAX_BYTES = 8 * 1024 * 1024
    @Volatile private var cliente: OkHttpClient? = null

    private fun cliente(contexto: Context): OkHttpClient = cliente ?: synchronized(this) {
        cliente ?: OkHttpClient.Builder()
            .cache(Cache(File(contexto.applicationContext.cacheDir, "imagenes_http"), 20L * 1024 * 1024))
            .callTimeout(10, TimeUnit.SECONDS)
            .build().also { cliente = it }
    }

    suspend fun cargar(contexto: Context, direccion: String): Bitmap? = withContext(Dispatchers.IO) {
        val uri = Uri.parse(direccion)
        if (uri.scheme in setOf("http", "https")) {
            cliente(contexto).newCall(Request.Builder().url(direccion).build()).execute().use { respuesta ->
                if (!respuesta.isSuccessful) throw IOException("No se pudo descargar la imagen")
                val cuerpo = respuesta.body ?: return@withContext null
                if (cuerpo.contentLength() > MAX_BYTES) throw IOException("Imagen demasiado grande")
                // Limita también cuerpos sin Content-Length antes de reservar el bitmap.
                val bytes = cuerpo.byteStream().use { it.readBytesLimitados() }
                decodificar { opciones -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones) }
            }
        } else if (uri.scheme == "content") {
            val bitmap = decodificar { opciones ->
                contexto.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opciones)
                }
            }
            if (bitmap == null) return@withContext null
            val orientacion = try {
                contexto.contentResolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (_: IOException) { ExifInterface.ORIENTATION_NORMAL }
            orientar(bitmap, orientacion)
        } else null
    }

    private fun orientar(bitmap: Bitmap, orientacion: Int): Bitmap {
        val matriz = Matrix()
        when (orientacion) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matriz.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matriz.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matriz.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matriz.setRotate(90f); matriz.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matriz.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { matriz.setRotate(-90f); matriz.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matriz.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    }

    private fun java.io.InputStream.readBytesLimitados(): ByteArray {
        val salida = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val leidos = read(buffer)
            if (leidos == -1) break
            total += leidos
            if (total > MAX_BYTES) throw IOException("Imagen demasiado grande")
            salida.write(buffer, 0, leidos)
        }
        return salida.toByteArray()
    }

    private inline fun decodificar(lectura: (BitmapFactory.Options) -> Bitmap?): Bitmap? {
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true; inSampleSize = 1 }
        lectura(opciones)
        if (opciones.outWidth <= 0 || opciones.outHeight <= 0) return null
        while (opciones.outWidth / opciones.inSampleSize > 1000 || opciones.outHeight / opciones.inSampleSize > 1000) {
            opciones.inSampleSize *= 2
        }
        opciones.inJustDecodeBounds = false
        return lectura(opciones)
    }
}
