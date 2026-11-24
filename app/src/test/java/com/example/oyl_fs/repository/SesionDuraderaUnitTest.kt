package com.example.oyl_fs.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.oyl_fs.network.*
import com.example.oyl_fs.util.CifradoSesion
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SesionDuraderaUnitTest {
    private val contexto=mockk<Context>()
    private val prefs=mockk<SharedPreferences>()
    private val editor=mockk<SharedPreferences.Editor>()
    private val cifrado=mockk<CifradoSesion>()
    private val api=mockk<UsuariosApi>()
    private val textos=mutableMapOf<String,String?>()
    private val usuario=UsuarioDto(7,"Persona","persona","persona@example.test","912345678",rol="CLIENTE")
    private fun login()=LoginResponseDto(usuario,"cabecera.carga.firma","Bearer",System.currentTimeMillis()/1000+3600)
    @Before fun preparar(): Unit=runBlocking {
        every { contexto.applicationContext } returns contexto
        every { contexto.getSharedPreferences(any(),any()) } returns prefs
        every { prefs.edit() } returns editor
        every { prefs.getString(any(),any()) } answers { textos[firstArg()] ?: secondArg() }
        every { prefs.contains(any()) } answers { textos.containsKey(firstArg()) }
        every { editor.clear() } answers { textos.clear();editor }
        every { editor.putString(any(),any()) } answers { textos[firstArg()]=secondArg();editor }
        every { editor.remove(any()) } answers { textos.remove(firstArg());editor }
        every { editor.commit() } returns true
        every { cifrado.cifrar(any()) } answers { firstArg<String>() }
        every { cifrado.descifrar(any()) } answers { firstArg<String>() }
        SesionRemotaRepository.configurar(contexto,cifrado);SesionRemotaRepository.cerrar()
        coEvery { api.login(any()) } returns login()
    }
    @After fun limpiar()=runBlocking { SesionRemotaRepository.cerrar();unmockkAll() }
    @Test fun restauracionRecuperaIdentidadYDescartaSesionAntiguaODanada()=runBlocking {
        assertNull(SesionRemotaRepository.restaurar())
        textos["usuario"]="legado";assertNull(SesionRemotaRepository.restaurar());assertTrue(textos.isEmpty())
        assertNotNull(SesionRemotaRepository.mensajeRestauracion)
        SesionRemotaRepository.iniciarSesion("persona","Clave123!",api).getOrThrow()
        val identidad=with(SesionRemotaRepository) { this@SesionDuraderaUnitTest.usuario.identidad() }
        SesionRemotaRepository.guardar(identidad)
        assertEquals(identidad,SesionRemotaRepository.restaurar())
        val guardada=requireNotNull(textos["sesion_cifrada"])
        textos["sesion_cifrada"]=guardada.replace(Regex("\"expiresAtEpochSeconds\":\\d+"),"\"expiresAtEpochSeconds\":1")
        assertNull(SesionRemotaRepository.restaurar());assertTrue(textos.isEmpty())
        textos["sesion_cifrada"]="null";assertNull(SesionRemotaRepository.restaurar())
    }
    @Test fun respuestasDeLoginInvalidasNoActivanSesion()=runBlocking {
        for(mala in listOf(login().copy(tokenType="Basic"),login().copy(accessToken="invalido"),login().copy(expiresAtEpochSeconds=1),
            login().copy(usuario=usuario.copy(id=0)),login().copy(usuario=usuario.copy(rol="OTRO")))) {
            coEvery { api.login(any()) } returns mala
            assertTrue(SesionRemotaRepository.iniciarSesion("persona","Clave123!",api).isFailure)
            assertNull(SesionRemotaRepository.sesionActual())
        }
    }
    @Test fun invalidacionDeSesionAnteriorNoAfectaActualYPerfilConfirmadoSePersisteSinHttp()=runBlocking {
        SesionRemotaRepository.iniciarSesion("persona","Clave123!",api).getOrThrow()
        val sesion=requireNotNull(SesionRemotaRepository.sesionActual())
        assertEquals("cabecera.carga.firma",SesionRemotaRepository.credencialParaPeticion()?.token)
        SesionRemotaRepository.invalidarSiCoincide("anterior","Antigua");assertNotNull(SesionRemotaRepository.sesionActual())
        SesionRemotaRepository.expirarSiCoincide(sesion.idSesion);assertNotNull(SesionRemotaRepository.sesionActual())
        coEvery { api.actualizar(7,any()) } returns usuario.copy(nombre="Editada")
        val confirmacion=SesionRemotaRepository.confirmarActualizacionPerfil(ActualizarPerfilRequest("Editada",usuario.correo),api).getOrThrow()
        every { editor.commit() } returns false
        assertFalse(SesionRemotaRepository.persistirPerfilConfirmado(confirmacion))
        every { editor.commit() } returns true
        assertTrue(SesionRemotaRepository.persistirPerfilConfirmado(confirmacion))
        coVerify(exactly=1) { api.actualizar(any(),any()) }
        SesionRemotaRepository.invalidarSiCoincide(sesion.idSesion,"Venció")
        assertNull(SesionRemotaRepository.credencialParaPeticion());assertEquals("Venció",SesionRemotaRepository.invalidacion.value?.mensaje)
    }
    @Test fun perfilDeOtraCuentaYRegistroConRolAdminSonRechazados()=runBlocking {
        SesionRemotaRepository.iniciarSesion("persona","Clave123!",api).getOrThrow()
        coEvery { api.actualizar(any(),any()) } returns usuario.copy(id=8)
        assertTrue(SesionRemotaRepository.confirmarActualizacionPerfil(ActualizarPerfilRequest("Nombre",usuario.correo),api).isFailure)
        val solicitud=RegistroRequest(nombre="Persona",alias="persona",correo=usuario.correo,telefono="912345678",contrasena="Clave123!")
        coEvery { api.registrar(any()) } returns usuario.copy(rol="ADMIN")
        assertTrue(SesionRemotaRepository.registrar(solicitud,api).isFailure)
        coEvery { api.registrar(any()) } returns usuario
        assertEquals(usuario,SesionRemotaRepository.registrar(solicitud,api).getOrThrow())
    }
    @Test fun sesionLocalRestauraRolDesdeCuentaYNoDesdePreferencias()=runBlocking {
        val cuentas=mockk<UsuarioRoomRepository>()
        val local=SesionRepository(prefs,cuentas)
        assertNull(local.restaurar())
        for((id,admin) in listOf("admin-demo" to true,"cliente-demo" to false)) {
            textos["usuarioId"]=id;val sesion=requireNotNull(local.restaurar());assertEquals(admin,sesion.esAdmin)
            local.guardar(sesion);assertEquals(id,textos["usuarioId"])
        }
        textos["usuarioId"]="no-existe";coEvery { cuentas.obtenerUsuario("no-existe") } returns null
        assertNull(local.restaurar());assertFalse(textos.containsKey("usuarioId"))
    }
}
