package com.example.oyl_fs.viewmodel

import com.example.oyl_fs.repository.*
import com.example.oyl_fs.network.UsuarioDto
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AccesoRegistroUnitTest {
    private val dispatcher=StandardTestDispatcher()
    private val cuentas=mockk<UsuarioRoomRepository>()
    private val extras=mockk<DatosLocalesRemotosRepository>(relaxed=true)
    private val usuario=UsuarioDto(7,"Nombre","persona","persona@example.test","912345678",rol="CLIENTE")
    private val formulario=FormularioRegistro(" Persona "," Nombre "," Persona@example.test ","12.345.678-5","912345678","Clave123!","Clave123!")
    @Before fun preparar() { Dispatchers.setMain(dispatcher);mockkObject(SesionRemotaRepository) }
    @After fun limpiar() { unmockkAll();Dispatchers.resetMain() }
    @Test fun registroLocalNormalizaDatosYNoDuplicaMientrasProcesa() = runTest {
        val resultado=CompletableDeferred<ResultadoRegistro>()
        coEvery { cuentas.registrarCliente(any()) } coAnswers { resultado.await() }
        val vm=RegistroViewModel(cuentas);var navegaciones=0
        assertFalse(vm.usaBackend)
        vm.registrar(formulario) { navegaciones++ };runCurrent()
        assertTrue(vm.estado.value.procesando)
        vm.registrar(formulario) { navegaciones++ };vm.reiniciar()
        assertTrue(vm.estado.value.procesando)
        resultado.complete(ResultadoRegistro.Exito("7"));advanceUntilIdle()
        assertEquals(1,navegaciones);assertFalse(vm.estado.value.procesando)
        coVerify(exactly=1) { cuentas.registrarCliente(match { it.alias=="Persona" && it.nombre=="Nombre" && it.rut=="12.345.678-5" }) }
    }
    @Test fun erroresDelRepositorioSeAsignanAlCampoCorrectoYSeLimpian() = runTest {
        val vm=RegistroViewModel(cuentas)
        for((mensaje,campo) in listOf("RUT ocupado" to CampoRegistro.RUT,"alias ocupado" to CampoRegistro.ALIAS,"correo ocupado" to CampoRegistro.CORREO,"alias o correo ocupado" to CampoRegistro.NOMBRE)) {
            coEvery { cuentas.registrarCliente(any()) } returns ResultadoRegistro.Error(mensaje)
            vm.registrar(formulario) { fail() };advanceUntilIdle()
            val e=vm.estado.value.errores
            assertEquals(mensaje,when(campo) { CampoRegistro.RUT->e.rut;CampoRegistro.ALIAS->e.alias;CampoRegistro.CORREO->e.correo;else->e.general })
            vm.limpiarError(campo);assertFalse(vm.estado.value.errores.hayErrores())
        }
        vm.registrar(formulario.copy(alias="",nombre="",correo="",rut="",telefono="x",clave="",confirmacion="x")) { fail() }
        assertTrue(vm.estado.value.errores.hayErrores())
        for(campo in CampoRegistro.values()) vm.limpiarError(campo)
        assertFalse(vm.estado.value.errores.hayErrores());vm.reiniciar();assertEquals(RegistroUiState(),vm.estado.value)
    }
    @Test fun registroRemotoGuardaRutSoloDespuesDeCrearCuenta() = runTest {
        coEvery { SesionRemotaRepository.registrar(any(),any()) } returns Result.success(usuario)
        val vm=RegistroViewModel(cuentas,extras);var exito=false
        assertTrue(vm.usaBackend)
        vm.registrar(formulario) { exito=true };advanceUntilIdle()
        assertTrue(exito)
        coVerifyOrder { SesionRemotaRepository.registrar(match { it.alias=="persona" && it.correo=="persona@example.test" },any());extras.guardarPerfil(usuario,"12.345.678-5","") }
        coVerify(exactly=0) { cuentas.registrarCliente(any()) }
    }
    @Test fun cuentaCreadaConErrorLocalInformaQueNoHayQueRegistrarseDeNuevo() = runTest {
        coEvery { SesionRemotaRepository.registrar(any(),any()) } returns Result.success(usuario)
        coEvery { extras.guardarPerfil(any<UsuarioDto>(),any(),any()) } throws IOException()
        val vm=RegistroViewModel(cuentas,extras)
        vm.registrar(formulario) { fail() };advanceUntilIdle()
        assertTrue(vm.estado.value.errores.general.orEmpty().contains("ya fue creada"))
        assertFalse(vm.estado.value.procesando)
    }
    @Test fun limitesRemotosYErroresDeRedNoNavegan() = runTest {
        val vm=RegistroViewModel(cuentas,extras)
        vm.registrar(formulario.copy(alias="a".repeat(21),telefono="")) { fail() }
        assertNotNull(vm.estado.value.errores.alias);assertNotNull(vm.estado.value.errores.telefono)
        coEvery { SesionRemotaRepository.registrar(any(),any()) } returns Result.failure(IOException())
        vm.registrar(formulario) { fail() };advanceUntilIdle();assertNotNull(vm.estado.value.errores.general)
        coEvery { cuentas.registrarCliente(any()) } throws IOException()
        val local=RegistroViewModel(cuentas);local.registrar(formulario) { fail() };advanceUntilIdle()
        assertTrue(local.estado.value.errores.general.orEmpty().contains("No se pudo guardar"))
        coEvery { cuentas.registrarCliente(any()) } throws CancellationException()
        local.registrar(formulario) { fail() };advanceUntilIdle();assertFalse(local.estado.value.procesando)
    }
    @Test fun loginValidaCamposYLimpiaErroresSinConsultarRepositorio() = runTest {
        val vm=LoginViewModel(cuentas,true)
        vm.iniciarSesion("","") { fail() }
        assertNotNull(vm.estado.value.errorUsuario);assertNotNull(vm.estado.value.errorContrasena)
        vm.limpiarErrorUsuario();assertNull(vm.estado.value.errorUsuario)
        vm.limpiarErrorContrasena();assertNull(vm.estado.value.errorContrasena)
        vm.iniciarSesion("persona","é".repeat(37)) { fail() };assertNotNull(vm.estado.value.errorContrasena)
        assertTrue(vm.autenticarCredenciales("persona","é".repeat(37)) is ResultadoLogin.Error)
        vm.limpiarError();assertNull(vm.estado.value.error)
        coVerify(exactly=0) { SesionRemotaRepository.iniciarSesion(any(),any(),any()) }
    }
    @Test fun loginRemotoRespetaRolYBloqueaPulsacionesRepetidas() = runTest {
        val respuesta=CompletableDeferred<Result<UsuarioDto>>()
        coEvery { SesionRemotaRepository.iniciarSesion(any(),any(),any()) } coAnswers { respuesta.await() }
        val vm=LoginViewModel(cuentas,true);var sesion:SesionIdentificada?=null
        vm.iniciarSesionRemota(" PERSONA ","Clave123!") { sesion=it };runCurrent()
        assertTrue(vm.estado.value.procesando)
        vm.iniciarSesion("persona","Clave123!") { fail() }
        respuesta.complete(Result.success(usuario));advanceUntilIdle()
        assertEquals(SesionIdentificada("7","Nombre",false,false),sesion)
        coVerify(exactly=1) { SesionRemotaRepository.iniciarSesion("persona","Clave123!",any()) }
    }
    @Test fun loginFallidoOExcepcionNoNavegaYPermiteReintentar() = runTest {
        coEvery { SesionRemotaRepository.iniciarSesion(any(),any(),any()) } returns Result.failure(IOException())
        val vm=LoginViewModel(cuentas,true);vm.iniciarSesion("persona","Clave123!") { fail() };advanceUntilIdle()
        assertNotNull(vm.estado.value.error);assertFalse(vm.estado.value.procesando)
        coEvery { cuentas.autenticarCliente(any(),any()) } throws IOException()
        val local=LoginViewModel(cuentas);local.iniciarSesion("persona","Clave123!") { fail() };advanceUntilIdle()
        assertEquals("No se pudo comprobar la cuenta.",local.estado.value.error)
        coEvery { cuentas.autenticarCliente(any(),any()) } returns ClienteAutenticado("7","persona","Nombre")
        assertEquals("7",(local.autenticarCredenciales("persona","Clave123!") as ResultadoLogin.Exito).sesion.usuarioId)
        coEvery { cuentas.autenticarCliente(any(),any()) } throws CancellationException()
        local.iniciarSesion("persona","Clave123!") { fail() };advanceUntilIdle();assertFalse(local.estado.value.procesando)
    }
}
