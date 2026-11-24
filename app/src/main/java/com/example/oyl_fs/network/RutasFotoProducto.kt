package com.example.oyl_fs.network

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Solo estas rutas exactas del servidor de productos pueden recibir la sesión. */
object RutasFotoProducto {
    private val base = "${com.example.oyl_fs.BuildConfig.OYL_BACKEND_HOST}:8081/".toHttpUrl()
    private val ruta = Regex("/api/productos/[1-9][0-9]*/foto/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

    fun esPropia(url: HttpUrl): Boolean = url.scheme == base.scheme && url.host == base.host && url.port == base.port &&
        url.username.isEmpty() && url.password.isEmpty() && url.encodedQuery == null && url.fragment == null &&
        ruta.matches(url.encodedPath)

    fun resolver(referencia: String): String = if (ruta.matches(referencia))
        requireNotNull(base.resolve(referencia)).toString() else referencia

    /** Guardar la ruta relativa permite cambiar el host sin conservar la dirección del PC anterior. */
    fun referencia(direccion: String): String = if (ruta.matches(direccion)) direccion else {
        direccion.toHttpUrlOrNull()?.takeIf(::esPropia)?.encodedPath ?: direccion
    }

    fun correspondeAlProducto(referencia: String?, id: Long): Boolean = referencia != null &&
        ruta.matches(referencia) && referencia.startsWith("/api/productos/$id/foto/")
}
