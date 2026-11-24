package com.example.oyl_fs.network

import com.example.oyl_fs.repository.SesionRemotaRepository
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class SesionExpiradaException : IOException("La sesión venció")

/** Solo se instala en los clientes de los tres microservicios propios. */
class AutorizacionInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val ruta = original.url.encodedPath
        val publica = original.method == "POST" && ruta in setOf("/api/usuarios/login", "/api/usuarios/registro")
        val credencial = if (publica) null else SesionRemotaRepository.credencialParaPeticion()
        val solicitud = original.newBuilder().removeHeader("Authorization").apply {
            if (credencial != null) header("Authorization", "Bearer ${credencial.token}")
        }.build()
        val respuesta = chain.proceed(solicitud)
        if (respuesta.code == 401 && credencial != null) {
            SesionRemotaRepository.invalidarSiCoincide(credencial.idSesion, "Tu sesión ya no es válida. Vuelve a iniciar sesión. Si estabas confirmando una compra, revisa Mis pedidos antes de repetirla.")
        }
        // Un 403 no borra la sesión; ninguna operación se reenvía automáticamente.
        return respuesta
    }
}
