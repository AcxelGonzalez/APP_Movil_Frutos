package com.example.oyl_fs.repository

import com.example.oyl_fs.network.ActualizarPerfilRequest
import com.example.oyl_fs.network.LoginRequest
import com.example.oyl_fs.network.UsuarioDto
import com.example.oyl_fs.network.UsuariosApi
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

    private fun prepararSesionVacia() {
        SesionRepository.api = api
        SesionRepository.cerrarSesion()
    }

    @Test
    fun loginCorrectoGuardaLaSesionYRecortaElAlias() = runBlocking {
        prepararSesionVacia()
        val enviado = slot<LoginRequest>()
        coEvery { api.login(capture(enviado)) } returns admin

        val resultado = SesionRepository.iniciarSesion("  admin  ", "admin")

        assertTrue(resultado.isSuccess)
        assertEquals("admin", enviado.captured.alias)
        assertEquals(1L, SesionRepository.usuarioId)
        SesionRepository.cerrarSesion()
    }

    @Test
    fun siElServidorNoRespondeNoQuedaSesionIniciada() = runBlocking {
        prepararSesionVacia()
        coEvery { api.login(any()) } throws UnknownHostException("sin conexion")

        val resultado = SesionRepository.iniciarSesion("admin", "mala")

        assertTrue(resultado.isFailure)
        assertNull(SesionRepository.usuarioId)
    }

    @Test
    fun actualizarPerfilSinSesionFallaSinLlamarAlServidor() = runBlocking {
        prepararSesionVacia()

        val resultado = SesionRepository.actualizarPerfil(
            ActualizarPerfilRequest("Nuevo", "nuevo@oyl.cl")
        )

        assertTrue(resultado.isFailure)
        coVerify(exactly = 0) { api.actualizar(any(), any()) }
    }

    @Test
    fun actualizarPerfilConSesionReemplazaAlUsuarioGuardado() = runBlocking {
        prepararSesionVacia()
        coEvery { api.login(any()) } returns admin
        coEvery { api.actualizar(1L, any()) } returns admin.copy(nombre = "Admin editado")
        SesionRepository.iniciarSesion("admin", "admin")

        val resultado = SesionRepository.actualizarPerfil(
            ActualizarPerfilRequest("Admin editado", "admin@oyl.cl")
        )

        assertTrue(resultado.isSuccess)
        assertEquals("Admin editado", SesionRepository.usuario.value?.nombre)
        SesionRepository.cerrarSesion()
    }

    @Test
    fun cerrarSesionBorraAlUsuarioGuardado() = runBlocking {
        prepararSesionVacia()
        coEvery { api.login(any()) } returns admin
        SesionRepository.iniciarSesion("admin", "admin")

        SesionRepository.cerrarSesion()

        assertNull(SesionRepository.usuario.value)
    }
}