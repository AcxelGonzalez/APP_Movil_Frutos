package com.example.oyl_fs.viewmodel

import com.example.oyl_fs.repository.UsuarioRoomRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/* comprueba las reglas de negocios del login: campos obligatorios, cuentas
demostración, etc.
 */
class LoginViewModelTest {

    private val usuarios: UsuarioRoomRepository = mockk()
    private val viewModel = LoginViewModel(usuarios)

    @Test
    fun faltandoUsuarioOContrasenaAvisaSinConsultarNada() = runBlocking {
        assertTrue(viewModel.autenticarCredenciales("", "") is ResultadoLogin.Error)
        assertTrue(viewModel.autenticarCredenciales("admin", "") is ResultadoLogin.Error)
    }

    @Test
    fun lasCuentasDeDemostracionEntranConElRolCorrecto() = runBlocking {
        val admin = viewModel.autenticarCredenciales("admin", "admin")
        val cliente = viewModel.autenticarCredenciales("cliente", "cliente")

        assertTrue((admin as ResultadoLogin.Exito).sesion.esAdmin)
        assertTrue(admin.sesion.esDemo)
        assertFalse((cliente as ResultadoLogin.Exito).sesion.esAdmin)
    }

    @Test
    fun elAliasIgnoraMayusculasYEspaciosPeroLaContrasenaNo() = runBlocking {
        coEvery { usuarios.autenticarCliente(any(), any()) } returns null

        assertTrue(viewModel.autenticarCredenciales("  ADMIN  ", "admin") is ResultadoLogin.Exito)
        assertTrue(viewModel.autenticarCredenciales("admin", "ADMIN") is ResultadoLogin.Error)
    }

    @Test
    fun unUsuarioInexistenteRecibeElMismoMensajeGenerico() = runBlocking {
        coEvery { usuarios.autenticarCliente(any(), any()) } returns null

        val resultado = viewModel.autenticarCredenciales("fantasma", "1234")

        assertEquals(
            "Usuario o contraseña incorrectos.",
            (resultado as ResultadoLogin.Error).mensaje
        )
    }
}