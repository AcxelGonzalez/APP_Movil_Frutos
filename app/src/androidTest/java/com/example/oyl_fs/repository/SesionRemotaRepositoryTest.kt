package com.example.oyl_fs.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.oyl_fs.network.*
import com.example.oyl_fs.viewmodel.SesionIdentificada
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class SesionRemotaRepositoryTest {
    private val contexto = ApplicationProvider.getApplicationContext<Context>()
    private val preferencias = contexto.getSharedPreferences("sesion_remota_oyl", Context.MODE_PRIVATE)
    private lateinit var sesionProtegida: com.example.oyl_fs.util.SesionDispositivoProtegida
    private val repo = SesionRemotaRepository
    private class Api : UsuariosApi {
        val cliente = UsuarioDto(7, "Cliente prueba", "prueba", "prueba@example.test", "912345678", rol = "CLIENTE")
        var solicitud: LoginRequest? = null
        var rechazar = false
        override suspend fun login(request: LoginRequest): LoginResponseDto {
            solicitud = request
            if (rechazar) throw HttpException(Response.error<Unit>(401, "{}".toResponseBody()))
            return LoginResponseDto(cliente, "cabecera.carga.firma", "Bearer", System.currentTimeMillis() / 1000 + 3600)
        }
        override suspend fun registrar(request: RegistroRequest) = cliente.copy(id = 8)
        override suspend fun actualizar(id: Long, request: ActualizarPerfilRequest) = cliente.copy(nombre = request.nombre)
    }
    @Before fun preparar() = runBlocking {
        sesionProtegida = com.example.oyl_fs.util.SesionDispositivoProtegida(contexto)
    }
    @After fun limpiar() = runBlocking {
        sesionProtegida.close()
        Unit
    }

    @Test fun autenticaContraApiGuardaIdentidadSinContrasenaYRestauraYCierra() = runBlocking {
        val api = Api()
        val usuario = repo.iniciarSesion(" prueba ", "clave-secreta", api).getOrThrow()
        assertEquals(LoginRequest("prueba", "clave-secreta"), api.solicitud)
        val identidad = SesionIdentificada("7", "Cliente prueba", false, false)
        repo.guardar(identidad)
        assertFalse(preferencias.getString("usuario", "").orEmpty().contains("clave-secreta"))
        assertEquals(identidad, repo.restaurar())
        assertEquals(usuario, repo.usuario.value)
        repo.cerrar()
        assertNull(repo.restaurar())
        assertNull(repo.usuario.value)
    }

    @Test fun rechazoDelBackendNoActivaUnaCuentaDemoLocal() = runBlocking {
        val resultado = repo.iniciarSesion("admin", "admin", Api().apply { rechazar = true })
        assertTrue(resultado.isFailure)
        assertNull(repo.usuario.value)
        assertNull(repo.restaurar())
    }

    @Test fun registroNoIniciaSesionYPerfilActualizadoSeConservaAlRestaurar() = runBlocking {
        val api = Api()
        assertEquals(8L, repo.registrar(RegistroRequest("Prueba", "prueba", "prueba@example.test", "912345678", contrasena = "clave-secreta"), api).getOrThrow().id)
        assertNull(repo.usuario.value)
        repo.iniciarSesion("prueba", "clave-secreta", api).getOrThrow()
        repo.guardar(SesionIdentificada("7", "Cliente prueba", false, false))
        repo.actualizarPerfil(ActualizarPerfilRequest("Nombre editado", "prueba@example.test", "912345678"), api).getOrThrow()
        assertEquals("Nombre editado", repo.restaurar()?.nombre)
    }

    @Test fun cacheCorruptaSeDescartaSinAbrirUnaSesion() = runBlocking {
        check(preferencias.edit().putString("usuario", "{incompleto").commit())
        assertNull(repo.restaurar())
        assertFalse(preferencias.contains("usuario"))
    }
}
