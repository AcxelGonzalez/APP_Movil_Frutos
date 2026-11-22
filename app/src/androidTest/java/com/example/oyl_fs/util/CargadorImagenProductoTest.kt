package com.example.oyl_fs.util

import android.content.Context
import android.graphics.Bitmap
import android.media.ExifInterface
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CargadorImagenProductoTest {
    private val contexto = ApplicationProvider.getApplicationContext<Context>()

    @Test fun fotografiaVerticalRespetaLaOrientacionDeLaCamara() = runBlocking {
        val carpeta = File(contexto.filesDir, "fotos_productos").apply { mkdirs() }
        val archivo = File.createTempFile("prueba_", ".jpg", carpeta)
        try {
            val original = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888)
            archivo.outputStream().use { original.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            original.recycle()
            ExifInterface(archivo.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }
            val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fotos", archivo)
            val foto = requireNotNull(CargadorImagenProducto.cargar(contexto, uri.toString()))
            assertEquals(40, foto.width)
            assertEquals(80, foto.height)
            foto.recycle()
        } finally { archivo.delete() }
    }

    @Test fun fotografiaPrivadaSePuedeVolverALeerYReduceElTamano() = runBlocking {
        val carpeta = File(contexto.filesDir, "fotos_productos").apply { mkdirs() }
        val archivo = File.createTempFile("prueba_", ".png", carpeta)
        try {
            val original = Bitmap.createBitmap(2048, 1024, Bitmap.Config.ARGB_8888)
            archivo.outputStream().use { original.compress(Bitmap.CompressFormat.PNG, 100, it) }
            original.recycle()
            val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fotos", archivo).toString()
            repeat(2) {
                val foto = requireNotNull(CargadorImagenProducto.cargar(contexto, uri))
                assertEquals(512, foto.width)
                assertEquals(256, foto.height)
                foto.recycle()
            }
        } finally { archivo.delete() }
    }

    @Test fun imagenHttpSeDescargaYSeReutilizaDesdeCacheSinServidor() = runBlocking {
        val bitmap = Bitmap.createBitmap(24, 12, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().use { salida ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, salida)
            salida.toByteArray()
        }
        bitmap.recycle()
        val servidor = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val url = "http://127.0.0.1:${servidor.localPort}/${UUID.randomUUID()}.png"
        val ejecutor = Executors.newSingleThreadExecutor()
        val envio = ejecutor.submit {
            servidor.accept().use { socket ->
                socket.soTimeout = 5000
                val entrada = socket.getInputStream().bufferedReader()
                while (!entrada.readLine().isNullOrEmpty()) { }
                socket.getOutputStream().apply {
                    write(("HTTP/1.1 200 OK\r\nContent-Type: image/png\r\nContent-Length: ${bytes.size}\r\n" +
                        "Cache-Control: public, max-age=3600\r\nConnection: close\r\n\r\n").toByteArray())
                    write(bytes)
                    flush()
                }
            }
        }
        try {
            val foto = requireNotNull(CargadorImagenProducto.cargar(contexto, url))
            assertEquals(24, foto.width)
            foto.recycle()
            envio.get(5, TimeUnit.SECONDS)
            servidor.close()
            val guardada = requireNotNull(CargadorImagenProducto.cargar(contexto, url))
            assertEquals(12, guardada.height)
            guardada.recycle()
        } finally {
            servidor.close()
            ejecutor.shutdownNow()
        }
    }

    @Test fun contenidoNoGraficoYEsquemasNoAdmitidosNoProducenUnBitmap() = runBlocking {
        val carpeta = File(contexto.filesDir, "fotos_productos").apply { mkdirs() }
        val archivo = File.createTempFile("prueba_", ".txt", carpeta)
        try {
            archivo.writeText("Esto no es una fotografía")
            val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fotos", archivo)
            assertNull(CargadorImagenProducto.cargar(contexto, uri.toString()))
            assertNull(CargadorImagenProducto.cargar(contexto, "file:///no-admitido"))
        } finally { archivo.delete() }
    }
}
