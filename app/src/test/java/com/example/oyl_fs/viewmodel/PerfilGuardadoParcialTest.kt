package com.example.oyl_fs.viewmodel

import android.app.Application
import android.content.SharedPreferences
import com.example.oyl_fs.model.PerfilUsuario
import com.example.oyl_fs.network.UsuarioDto
import com.example.oyl_fs.repository.*
import com.example.oyl_fs.util.ArchivosAvatar
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilGuardadoParcialTest {
    private val dispatcher = StandardTestDispatcher()
    private val extras = mockk<DatosLocalesRemotosRepository>()
    private val usuario = UsuarioDto(7, "Cliente", "cliente7", "cliente7@example.test", "912345678", rol = "CLIENTE")
    private val original = PerfilUsuario("7", false, "Cliente", correo = usuario.correo, telefono = "912345678")
    private val editado = original.copy(nombre = "Nombre editado", apellido = "Apellido local", rut = "12.345.678-5")
    private val remoto = usuario.copy(nombre = editado.nombre)
    private val confirmacion = PerfilRemotoConfirmado(remoto, "sesion-7")
    private lateinit var vm: PerfilViewModel
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        mockkObject(SesionRemotaRepository)
        every { SesionRemotaRepository.sesionActual() } returns SesionActiva(SesionIdentificada("7", "Cliente", false, false), "sesion-7", Long.MAX_VALUE)
        coEvery { extras.perfil(usuario) } returns original
        coEvery { SesionRemotaRepository.confirmarActualizacionPerfil(any(), any()) } returns Result.success(confirmacion)
        val preferencias = mockk<SharedPreferences>(relaxed = true)
        vm = PerfilViewModel(mockk<Application>(), mockk(), preferencias, extras, mockk<ArchivosAvatar>())
    }
    @After fun limpiar() { unmockkObject(SesionRemotaRepository); clearAllMocks(); Dispatchers.resetMain() }

    @Test fun falloDeSesionConservaPerfilYReintentaSoloLaSesion() = runTest {
        vm.activarRemoto(usuario)
        coEvery { SesionRemotaRepository.persistirPerfilConfirmado(confirmacion) } returnsMany listOf(false, true)
        coEvery { extras.guardarPerfil(remoto, editado.rut, editado.apellido) } returns editado
        assertFalse(vm.guardarPerfil(editado))
        assertEquals(editado.nombre, vm.perfiles.value.single().nombre)
        assertTrue(vm.errores.value.general.orEmpty().contains("se guardó en el servidor"))
        assertTrue(vm.guardarPerfil(editado))
        coVerify(exactly = 1) { SesionRemotaRepository.confirmarActualizacionPerfil(any(), any()) }
        coVerify(exactly = 2) { SesionRemotaRepository.persistirPerfilConfirmado(confirmacion) }
        coVerify(exactly = 1) { extras.guardarPerfil(remoto, editado.rut, editado.apellido) }
    }

    @Test fun falloRoomConservaRutYApellidoYNoReenviaLaEdicionRemota() = runTest {
        vm.activarRemoto(usuario)
        coEvery { SesionRemotaRepository.persistirPerfilConfirmado(confirmacion) } returns true
        var escrituras = 0
        coEvery { extras.guardarPerfil(remoto, editado.rut, editado.apellido) } coAnswers {
            escrituras++; if (escrituras == 1) throw IOException("Fallo simulado") else editado
        }
        assertFalse(vm.guardarPerfil(editado))
        assertEquals(editado.rut, vm.perfiles.value.single().rut)
        vm.activarRemoto(usuario)
        assertEquals(editado.apellido, vm.perfiles.value.single().apellido)
        assertTrue(vm.guardarPerfil(editado))
        coVerify(exactly = 1) { SesionRemotaRepository.confirmarActualizacionPerfil(any(), any()) }
        coVerify(exactly = 1) { SesionRemotaRepository.persistirPerfilConfirmado(confirmacion) }
        assertEquals(2, escrituras)
    }

    @Test fun cambiarSoloDatosLocalesPendientesNoVuelveAActualizarElServidor() = runTest {
        vm.activarRemoto(usuario)
        coEvery { SesionRemotaRepository.persistirPerfilConfirmado(confirmacion) } returns false
        coEvery { extras.guardarPerfil(any<UsuarioDto>(), any(), any()) } returns editado
        assertFalse(vm.guardarPerfil(editado))
        assertFalse(vm.guardarPerfil(editado.copy(apellido = "Otro apellido")))
        coVerify(exactly = 1) { SesionRemotaRepository.confirmarActualizacionPerfil(any(), any()) }
        coVerify { extras.guardarPerfil(remoto, editado.rut, "Otro apellido") }
    }
}
