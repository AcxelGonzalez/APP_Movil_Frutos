package com.example.oyl_fs.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModelStore
import com.example.oyl_fs.data.local.UsuarioEntity
import com.example.oyl_fs.model.*
import com.example.oyl_fs.repository.*
import com.example.oyl_fs.util.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilAvatarUnitTest {
    private val dispatcher=StandardTestDispatcher()
    private val store=ViewModelStore()
    private val cuentas=mockk<UsuarioRoomRepository>()
    private val extras=mockk<DatosLocalesRemotosRepository>(relaxed=true)
    private val archivos=mockk<ArchivosAvatar>(relaxed=true)
    private val prefs=mockk<SharedPreferences>()
    private val editor=mockk<SharedPreferences.Editor>()
    private val usuario=UsuarioEntity("7","persona","persona","Persona","Apellido","12.345.678-5","persona@example.test","persona@example.test","912345678","Calle 1","hash","salt",1)
    private lateinit var vm:PerfilViewModel
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        every { prefs.getString(any(),any()) } answers { secondArg() }
        every { prefs.contains(any()) } returns false
        every { prefs.edit() } returns editor
        every { editor.putString(any(),any()) } returns editor
        every { editor.commit() } returns true
        every { archivos.nuevoNombre() } returns "nuevo.jpg"
        vm=PerfilViewModel(mockk<Application>(),cuentas,prefs,extras,archivos);store.put("perfil",vm)
    }
    @After fun limpiar() { store.clear();unmockkAll();Dispatchers.resetMain() }
    @Test fun guardarDemoNormalizaYNoPublicaSiFallaDisco()=runTest {
        val original=vm.perfiles.value.first()
        assertFalse(vm.guardarPerfil(original.copy(id="otro")))
        assertFalse(vm.guardarPerfil(original.copy(esAdmin=false)))
        assertFalse(vm.guardarPerfil(original.copy(nombre="")))
        assertTrue(vm.guardarPerfil(original.copy(nombre=" Editado ")))
        assertEquals("Editado",vm.perfiles.value.first().nombre)
        every { editor.commit() } returns false
        assertFalse(vm.guardarPerfil(original.copy(nombre="No guardar")))
        assertEquals("Editado",vm.perfiles.value.first().nombre);assertNotNull(vm.error.value)
    }
    @Test fun perfilLocalUsaLaCuentaYConservaErrorDeValidacionDeRepositorio()=runTest {
        vm.activarClienteRegistrado(usuario)
        val perfil=vm.perfiles.value.last()
        coEvery { cuentas.actualizarPerfil(any()) } returns ErroresPerfil(correo="Correo ocupado")
        assertFalse(vm.guardarPerfil(perfil));assertEquals("Correo ocupado",vm.errores.value.correo)
        coEvery { cuentas.actualizarPerfil(any()) } returns null
        assertTrue(vm.guardarPerfil(perfil.copy(nombre=" Editado ")))
        assertEquals("Editado",vm.perfiles.value.last().nombre)
        vm.limpiarSesionRegistrada();assertEquals(2,vm.perfiles.value.size)
    }
    @Test fun preferenciasLegadasSeMigranUnaVezYErrorNoSobrescribeCuenta()=runTest {
        every { prefs.contains("7.nombre") } returns true
        every { prefs.getString("7.nombre",any()) } returns "Migrado"
        coEvery { cuentas.actualizarPerfil(any()) } returns null
        coEvery { cuentas.obtenerUsuario("7") } returns usuario.copy(nombre="Migrado",perfilPreferenciasMigrado=true)
        vm.activarClienteRegistrado(usuario);assertEquals("Migrado",vm.perfiles.value.last().nombre)
        vm.activarClienteRegistrado(usuario.copy(perfilPreferenciasMigrado=true))
        coVerify(exactly=1) { cuentas.actualizarPerfil(any()) }
        coEvery { cuentas.actualizarPerfil(any()) } returns ErroresPerfil(rut="Duplicado")
        vm.activarClienteRegistrado(usuario)
        assertEquals("Persona",vm.perfiles.value.last().nombre);assertNotNull(vm.errores.value.general)
    }
    @Test fun avatarSeCargaSeReemplazaYSeQuitaPorCuenta()=runTest {
        vm.activarClienteRegistrado(usuario)
        coEvery { extras.obtenerAvatar("local:7") } returns "anterior.jpg"
        vm.cargarAvatar("7");advanceUntilIdle();assertEquals("anterior.jpg",vm.perfiles.value.last().avatarArchivo)
        val generacion=requireNotNull(vm.prepararSeleccionAvatar("7"))
        assertNull(vm.prepararSeleccionAvatar("otra"))
        val uri=mockk<Uri>()
        coEvery { extras.reemplazarAvatar("local:7","nuevo.jpg") } returns "anterior.jpg"
        vm.cambiarAvatar("7",generacion,uri);advanceUntilIdle()
        assertEquals("nuevo.jpg",vm.perfiles.value.last().avatarArchivo)
        coVerifyOrder { archivos.importar(uri,"nuevo.jpg");extras.reemplazarAvatar("local:7","nuevo.jpg");archivos.eliminar("anterior.jpg") }
        assertNotNull(vm.mensajeAvatar.value)
        coEvery { extras.reemplazarAvatar("local:7",null) } returns "nuevo.jpg"
        vm.quitarAvatar("7");advanceUntilIdle();assertNull(vm.perfiles.value.last().avatarArchivo)
        assertEquals("Avatar quitado.",vm.mensajeAvatar.value)
    }
    @Test fun avatarInvalidoConservaAnteriorYLimpiaArchivoNuevo()=runTest {
        vm.activarClienteRegistrado(usuario)
        coEvery { extras.obtenerAvatar("local:7") } returns "anterior.jpg"
        vm.cargarAvatar("7");advanceUntilIdle()
        val generacion=requireNotNull(vm.prepararSeleccionAvatar("7"))
        vm.errorSeleccionAvatar("7",generacion);assertNotNull(vm.errorAvatar.value)
        coEvery { archivos.importar(any(),any()) } throws AvatarInvalidoException("Imagen inválida")
        vm.cambiarAvatar("7",generacion,mockk());advanceUntilIdle()
        assertEquals("Imagen inválida",vm.errorAvatar.value);assertEquals("anterior.jpg",vm.perfiles.value.last().avatarArchivo)
        coVerify { archivos.eliminar("nuevo.jpg") }
        coVerify(exactly=0) { extras.reemplazarAvatar(any(),any()) }
        coEvery { extras.obtenerAvatar(any()) } throws IOException()
        vm.cargarAvatar("7");advanceUntilIdle();assertNotNull(vm.errorAvatar.value)
        coEvery { extras.reemplazarAvatar(any(),null) } throws IOException()
        vm.quitarAvatar("7");advanceUntilIdle();assertFalse(vm.guardando.value);assertNotNull(vm.errorAvatar.value)
    }
    @Test fun resultadoTardioDeGaleriaNoPasaAlSiguientePerfil()=runTest {
        vm.activarClienteRegistrado(usuario)
        val generacion=requireNotNull(vm.prepararSeleccionAvatar("7"))
        val importada=CompletableDeferred<Unit>()
        coEvery { archivos.importar(any(),any()) } coAnswers { importada.await() }
        vm.cambiarAvatar("7",generacion,mockk());runCurrent();assertTrue(vm.guardando.value)
        vm.quitarAvatar("7");assertNull(vm.prepararSeleccionAvatar("7"))
        vm.limpiarSesionRegistrada();importada.complete(Unit);advanceUntilIdle()
        coVerify(exactly=0) { extras.reemplazarAvatar(any(),any()) }
        coVerify { archivos.eliminar("nuevo.jpg") };assertFalse(vm.guardando.value)
        vm.errorSeleccionAvatar("7",generacion);assertNull(vm.errorAvatar.value)
    }
}
