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
    @Volatile private var clientePropio: OkHttpClient? = null

    private fun clienteAutenticado(): OkHttpClient = clientePropio ?: synchronized(this) {
        clientePropio ?: OkHttpClient.Builder()
            .callTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .followSslRedirects(false)
            .addInterceptor { cadena ->
                if (!com.example.oyl_fs.network.RutasFotoProducto.esPropia(cadena.request().url))
                    throw IOException("La imagen no pertenece al servidor de productos")
                cadena.proceed(cadena.request())
            }
            .addInterceptor(com.example.oyl_fs.network.AutorizacionInterceptor())
            .build().also { clientePropio = it }
    }

    private fun cliente(contexto: Context): OkHttpClient = cliente ?: synchronized(this) {
        cliente ?: OkHttpClient.Builder()
            .cache(Cache(File(contexto.applicationContext.cacheDir, "imagenes_http"), 20L * 1024 * 1024))
            .callTimeout(10, TimeUnit.SECONDS)
            .build().also { cliente = it }
    }

    suspend fun cargar(contexto: Context, direccion: String): Bitmap? = withContext(Dispatchers.IO) {
        val resuelta = com.example.oyl_fs.network.RutasFotoProducto.resolver(direccion)
        val uri = Uri.parse(resuelta)
        if (uri.scheme in setOf("http", "https")) {
            val solicitud = Request.Builder().url(resuelta).build()
            val conexion = if (com.example.oyl_fs.network.RutasFotoProducto.esPropia(solicitud.url))
                clienteAutenticado() else cliente(contexto)
            conexion.newCall(solicitud).execute().use { respuesta ->
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

    /** Lee solo el archivo seleccionado (hasta 8 MB), orienta y emite un JPG sin EXIF de hasta 2 MB. */
    suspend fun prepararSubida(contexto: Context, direccion: String): ByteArray = withContext(Dispatchers.IO) {
        val uri = Uri.parse(direccion)
        require(uri.scheme == "content") { "Selecciona una fotografía de la galería o cámara" }
        val bytes = try {
            val entrada = contexto.contentResolver.openInputStream(uri)
                ?: throw IOException("Fotografía no disponible")
            entrada.use { it.readBytesLimitados() }
        } catch (_: SecurityException) {
            throw IllegalArgumentException("No hay permiso para leer la fotografía. Selecciónala nuevamente.")
        } catch (_: IOException) {
            throw IllegalArgumentException("No se pudo abrir la fotografía o supera los 8 MB. Selecciona otra imagen.")
        }
        val jpg = bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte()
        val png = bytes.size >= 8 && bytes.take(8).toByteArray().contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a))
        require(jpg || png) { "Selecciona una fotografía JPG o PNG válida" }
        val bitmap = decodificar { opciones -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones) }
            ?: throw IllegalArgumentException("No se pudo leer la fotografía. Selecciona otra imagen JPG o PNG.")
        val orientacion = try {
            java.io.ByteArrayInputStream(bytes).use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        } catch (_: IOException) { ExifInterface.ORIENTATION_NORMAL }
        val orientada = orientar(bitmap, orientacion)
        var normalizada: Bitmap? = null
        try {
            normalizada = Bitmap.createBitmap(orientada.width, orientada.height, Bitmap.Config.ARGB_8888)
            android.graphics.Canvas(normalizada).apply {
                drawColor(android.graphics.Color.WHITE)
                drawBitmap(orientada, 0f, 0f, null)
            }
            val salida = java.io.ByteArrayOutputStream()
            check(normalizada.compress(Bitmap.CompressFormat.JPEG, 85, salida)) { "No se pudo preparar la fotografía" }
            val resultado = salida.toByteArray()
            require(resultado.isNotEmpty() && resultado.size <= 2 * 1024 * 1024) { "La fotografía procesada supera los 2 MB. Selecciona otra imagen." }
            resultado
        } finally {
            normalizada?.recycle()
            orientada.recycle()
        }
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
            if (leidos == -1) {
                break
            }
            total += leidos
            if (total > MAX_BYTES) {
                throw IOException("Imagen demasiado grande")
            }
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
