package com.example.oyl_fs.repository

import com.example.oyl_fs.network.ActualizarPerfilRequest
import com.example.oyl_fs.network.LoginRequest
import com.example.oyl_fs.network.UsuarioDto
import com.example.oyl_fs.network.UsuariosApi
import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.UnknownHostException

/* este test comprueba que la sesion se guarde al entrar, se borre al salir
y que el perfil no se pueda editar sin iniciar sesion.
 conexión al servidor es simulada
 */
class SesionRepositoryTest {

    private val api: UsuariosApi = mockk()
    private val admin = UsuarioDto(1, "Administrador", "admin", "admin@oyl.cl", rol = "ADMIN")

    private suspend fun prepararSesionVacia() {
        val contexto = mockk<Context>()
        val preferencias = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>()
        every { contexto.applicationContext } returns contexto
        every { contexto.getSharedPreferences(any(), any()) } returns preferencias
        every { preferencias.edit() } returns editor
        every { editor.clear() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.commit() } returns true
        SesionRemotaRepository.configurar(contexto)
        SesionRemotaRepository.cerrar()
    }

    @Test
    fun loginCorrectoGuardaLaSesionYRecortaElAlias() = runBlocking {
        prepararSesionVacia()
        val enviado = slot<LoginRequest>()
        coEvery { api.login(capture(enviado)) } returns admin

        val resultado = SesionRemotaRepository.iniciarSesion("  admin  ", "admin", api)

        assertTrue(resultado.isSuccess)
        assertEquals("admin", enviado.captured.alias)
        assertEquals(1L, SesionRemotaRepository.usuarioId)
        SesionRemotaRepository.cerrar()
    }

    @Test
    fun siElServidorNoRespondeNoQuedaSesionIniciada() = runBlocking {
        prepararSesionVacia()
        coEvery { api.login(any()) } throws UnknownHostException("sin conexion")

        val resultado = SesionRemotaRepository.iniciarSesion("admin", "mala", api)

        assertTrue(resultado.isFailure)
        assertNull(SesionRemotaRepository.usuarioId)
    }

    @Test
    fun actualizarPerfilSinSesionFallaSinLlamarAlServidor() = runBlocking {
        prepararSesionVacia()

        val resultado = SesionRemotaRepository.actualizarPerfil(
            ActualizarPerfilRequest("Nuevo", "nuevo@oyl.cl"), api
        )

        assertTrue(resultado.isFailure)
        coVerify(exactly = 0) { api.actualizar(any(), any()) }
    }

    @Test
    fun actualizarPerfilConSesionReemplazaAlUsuarioGuardado() = runBlocking {
        prepararSesionVacia()
        coEvery { api.login(any()) } returns admin
        coEvery { api.actualizar(1L, any()) } returns admin.copy(nombre = "Admin editado")
        SesionRemotaRepository.iniciarSesion("admin", "admin", api)

        val resultado = SesionRemotaRepository.actualizarPerfil(
            ActualizarPerfilRequest("Admin editado", "admin@oyl.cl"), api
        )

        assertTrue(resultado.isSuccess)
        assertEquals("Admin editado", SesionRemotaRepository.usuario.value?.nombre)
        SesionRemotaRepository.cerrar()
    }

    @Test
    fun cerrarSesionBorraAlUsuarioGuardado() = runBlocking {
        prepararSesionVacia()
        coEvery { api.login(any()) } returns admin
        SesionRemotaRepository.iniciarSesion("admin", "admin", api)

        SesionRemotaRepository.cerrar()

        assertNull(SesionRemotaRepository.usuario.value)
    }
}