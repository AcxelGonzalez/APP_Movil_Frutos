package com.example.oyl_fs.network

import com.example.oyl_fs.BuildConfig
import com.example.oyl_fs.repository.*
import io.mockk.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProteccionPeticionesUnitTest {
    @Before fun preparar() { mockkObject(SesionRemotaRepository) }
    @After fun limpiar() { unmockkAll() }
    @Test fun interceptorAgregaTokenSoloAProtegidasYNoReintenta() {
        val cadena=mockk<Interceptor.Chain>()
        val credencial=CredencialPeticion("token.qa.firma","s7")
        every { SesionRemotaRepository.credencialParaPeticion() } returns credencial
        every { SesionRemotaRepository.invalidarSiCoincide(any(),any()) } just Runs
        for((ruta,codigo) in listOf("/api/usuarios/login" to 401,"/api/usuarios/registro" to 201,"/api/productos" to 403,"/api/pedidos" to 401)) {
            val publica=ruta.contains("usuarios")
            val peticion=Request.Builder().url("http://localhost$ruta").header("Authorization","Anterior").apply {
                if(publica) post(ByteArray(0).toRequestBody())
            }.build()
            every { cadena.request() } returns peticion
            every { cadena.proceed(any()) } answers {
                val enviada=firstArg<Request>()
                assertEquals(if(publica)null else "Bearer token.qa.firma",enviada.header("Authorization"))
                Response.Builder().request(enviada).protocol(Protocol.HTTP_1_1).code(codigo).message("QA").body("".toResponseBody()).build()
            }
            AutorizacionInterceptor().intercept(cadena).close()
        }
        verify(exactly=4) { cadena.proceed(any()) }
        verify(exactly=1) { SesionRemotaRepository.invalidarSiCoincide("s7",any()) }
    }
    @Test fun fotosPropiasExigenHostPuertoRutaYSinParametros() {
        val ruta="/api/productos/7/foto/12345678-1234-1234-1234-123456789abc"
        val url="${BuildConfig.OYL_BACKEND_HOST}:8081$ruta"
        assertTrue(RutasFotoProducto.esPropia(url.toHttpUrl()))
        assertEquals(url,RutasFotoProducto.resolver(ruta));assertEquals(ruta,RutasFotoProducto.referencia(url))
        assertTrue(RutasFotoProducto.correspondeAlProducto(ruta,7));assertFalse(RutasFotoProducto.correspondeAlProducto(ruta,8))
        for(mala in listOf("http://ajeno.example:8081$ruta","$url?token=x","$url#fragmento",url.replace(":8081",":8082"),url.replace("/7/","/0/"))) {
            assertFalse(RutasFotoProducto.esPropia(mala.toHttpUrl()));assertEquals(mala,RutasFotoProducto.referencia(mala))
        }
        assertEquals("content://galeria/1",RutasFotoProducto.resolver("content://galeria/1"))
        assertFalse(RutasFotoProducto.correspondeAlProducto(null,7))
    }
}
