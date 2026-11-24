package com.example.oyl_fs.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AvatarInvalidoException(mensaje: String) : IllegalArgumentException(mensaje)

/** Copia solo la imagen elegida; los originales de la galería nunca se modifican. */
class ArchivosAvatar(contexto: Context) {
    private val contexto = contexto.applicationContext ?: contexto
    private val carpeta get() = File(contexto.filesDir, "avatares_perfil")
    private val nombreValido = Regex("""[0-9a-f-]{36}\.jpg""")

    private fun archivo(nombre: String?): File? =
        nombre?.takeIf { nombreValido.matches(it) }?.let { File(carpeta, it) }

    fun uri(nombre: String?): Uri? = archivo(nombre)?.takeIf { it.isFile }?.let {
        FileProvider.getUriForFile(contexto, "${contexto.packageName}.fotos", it)
    }

    fun nuevoNombre(): String = "${UUID.randomUUID()}.jpg"

    suspend fun importar(origen: Uri, nombre: String): Unit = withContext(Dispatchers.IO) {
        if (origen.scheme != "content") throw AvatarInvalidoException("Selecciona una imagen desde la galería.")
        check(carpeta.isDirectory || carpeta.mkdirs())
        val temporal = File.createTempFile("seleccion_", ".tmp", carpeta)
        val destino = requireNotNull(archivo(nombre))
        var completado = false
        try {
            val entrada = contexto.contentResolver.openInputStream(origen)
                ?: throw AvatarInvalidoException("No se pudo abrir la imagen seleccionada.")
            entrada.use { fuente ->
                temporal.outputStream().use { salida ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val leidos = fuente.read(buffer)
                        if (leidos < 0) break
                        total += leidos
                        if (total > 8L * 1024 * 1024) throw AvatarInvalidoException("Selecciona una imagen de hasta 8 MB.")
                        salida.write(buffer, 0, leidos)
                    }
                }
            }
            val uriTemporal = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fotos", temporal)
            val original = CargadorImagenProducto.cargar(contexto, uriTemporal.toString())
                ?: throw AvatarInvalidoException("No se pudo leer la imagen. Prueba con JPG o PNG.")
            var reducido = original
            try {
                val mayor = maxOf(original.width, original.height)
                if (mayor > 512) reducido = Bitmap.createScaledBitmap(original,
                    (original.width * 512 / mayor).coerceAtLeast(1),
                    (original.height * 512 / mayor).coerceAtLeast(1), true)
                currentCoroutineContext().ensureActive()
                check(destino.createNewFile())
                destino.outputStream().use { check(reducido.compress(Bitmap.CompressFormat.JPEG, 85, it)) }
                currentCoroutineContext().ensureActive()
                completado = true
                Unit
            } finally {
                if (reducido !== original) reducido.recycle()
                original.recycle()
            }
        } finally {
            temporal.delete()
            if (!completado) destino.delete()
        }
    }

    suspend fun eliminar(nombre: String?) = withContext(Dispatchers.IO) {
        // Solo archivos UUID creados por esta función, dentro de su carpeta privada.
        runCatching { archivo(nombre)?.delete() }
        Unit
    }
}
