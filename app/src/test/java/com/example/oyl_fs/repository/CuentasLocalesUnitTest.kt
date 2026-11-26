package com.example.oyl_fs.repository

import androidx.room.withTransaction
import com.example.oyl_fs.data.local.*
import com.example.oyl_fs.model.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CuentasLocalesUnitTest {
    private val db=mockk<OylDatabase>()
    private val dao=mockk<UsuarioDao>(relaxed=true)
    private lateinit var repo:UsuarioRoomRepository
    private val datos=DatosRegistroCliente(" Persona "," Nombre "," PERSONA@example.test ","Clave123!",rut="12.345.678-5",telefono="912345678")
    private val usuario=UsuarioEntity("7","Persona","persona","Nombre","Apellido","12.345.678-5","persona@example.test","persona@example.test","912345678","Calle 1","hash","salt",1)
    private val perfil=PerfilUsuario("7",false,"Nombre","Apellido","12.345.678-5","persona@example.test","912345678","Calle 1")
    @Before fun preparar() {
        every { db.usuarioDao() } returns dao
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Any?>()) } coAnswers { secondArg<suspend () -> Any?>().invoke() }
        mockkObject(HashContrasena)
        every { HashContrasena.crear(any()) } returns CredencialProtegida("hash","salt")
        repo=UsuarioRoomRepository(db)
    }
    @After fun limpiar() { unmockkAll() }
    @Test fun registroNormalizaIdentidadYAlmacenaSoloCredencialProtegida() = runTest {
        val resultado=repo.registrarCliente(datos) as ResultadoRegistro.Exito
        assertTrue(resultado.usuarioId.isNotBlank())
        coVerify { dao.insertar(match { it.id==resultado.usuarioId && it.alias=="Persona" && it.aliasNormalizado=="persona" &&
            it.correoNormalizado=="persona@example.test" && it.claveHash=="hash" && it.claveSalt=="salt" }) }
    }
    @Test fun datosInvalidosNoConsultanNiEscribenCuentas() = runTest {
        for(malo in listOf(datos.copy(alias="x"),datos.copy(alias="admin"),datos.copy(nombre=""),datos.copy(nombre="x".repeat(256)),
            datos.copy(correo="sin-arroba"),datos.copy(clave="corta"))) assertTrue(repo.registrarCliente(malo) is ResultadoRegistro.Error)
        coVerify(exactly=0) { dao.insertar(any()) }
        verify(exactly=0) { HashContrasena.crear(any()) }
    }
    @Test fun duplicadosSeDetectanAntesDelHashYSeRevisaRutDeNuevoAntesDeInsertar() = runTest {
        coEvery { dao.existeAlias("persona") } returns true
        assertTrue((repo.registrarCliente(datos) as ResultadoRegistro.Error).mensaje.contains("alias"))
        coEvery { dao.existeAlias("persona") } returns false
        coEvery { dao.existeCorreo("persona@example.test") } returns true
        assertTrue((repo.registrarCliente(datos) as ResultadoRegistro.Error).mensaje.contains("correo"))
        coEvery { dao.existeCorreo(any()) } returns false
        coEvery { dao.existeRutNormalizado("123456785") } returns true
        assertTrue((repo.registrarCliente(datos) as ResultadoRegistro.Error).mensaje.contains("RUT"))
        coEvery { dao.existeRutNormalizado(any()) } returnsMany listOf(false,true)
        assertTrue(repo.registrarCliente(datos) is ResultadoRegistro.Error)
        coVerify(exactly=0) { dao.insertar(any()) }
    }
    @Test fun autenticacionNormalizaAliasSinExponerHash() = runTest {
        assertNull(repo.autenticarCliente("","clave"));assertNull(repo.autenticarCliente("persona",""))
        coEvery { dao.obtenerPorAlias("persona") } returns null
        assertNull(repo.autenticarCliente("Persona","Clave123!"))
        coEvery { dao.obtenerPorAlias("persona") } returns usuario
        every { HashContrasena.verificar("Clave123!","hash","salt") } returnsMany listOf(false,true)
        assertNull(repo.autenticarCliente("Persona","Clave123!"))
        assertEquals(ClienteAutenticado("7","Persona","Nombre"),repo.autenticarCliente(" Persona ","Clave123!"))
        assertNull(repo.obtenerUsuario(""));coEvery { dao.obtenerPorId("7") } returns usuario
        assertEquals(usuario,repo.obtenerUsuario("7"))
    }
    @Test fun editarPerfilPreservaCredencialesYRechazaCuentasODuplicados() = runTest {
        assertNotNull(repo.actualizarPerfil(perfil.copy(nombre="")))
        assertNotNull(repo.actualizarPerfil(perfil.copy(esAdmin=true)))
        coEvery { dao.obtenerPorId("7") } returns null
        assertNotNull(repo.actualizarPerfil(perfil))
        coEvery { dao.obtenerPorId("7") } returns usuario
        coEvery { dao.existeCorreo("otro@example.test") } returns true
        assertNotNull(repo.actualizarPerfil(perfil.copy(correo="otro@example.test"))?.correo)
        coEvery { dao.existeRutNormalizado("111111111") } returns true
        assertNotNull(repo.actualizarPerfil(perfil.copy(rut="11.111.111-1"))?.rut)
        assertNull(repo.actualizarPerfil(perfil.copy(nombre=" Editado ")))
        coVerify { dao.actualizar(match { it.nombre=="Editado" && it.claveHash=="hash" && it.claveSalt=="salt" && it.perfilPreferenciasMigrado }) }
    }
}
