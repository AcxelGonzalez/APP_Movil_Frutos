package com.example.oyl_fs.repository

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.example.oyl_fs.data.local.*
import com.example.oyl_fs.model.*
import com.example.oyl_fs.network.UsuarioDto
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ComplementosLocalesUnitTest {
    private val db=mockk<PerfilRemotoDatabase>()
    private val dao=mockk<PerfilRemotoDao>(relaxed=true)
    private val avatar=mockk<AvatarPerfilDao>(relaxed=true)
    private val prefs=mockk<SharedPreferences>()
    private val editor=mockk<SharedPreferences.Editor>()
    private val textos=mutableMapOf<String,String?>()
    private val filas=mutableMapOf<Long,PerfilRemotoEntity>()
    private lateinit var repo:DatosLocalesRemotosRepository
    private val usuario=UsuarioDto(7,"Nombre","persona","persona@example.test","912345678",rol="CLIENTE")
    @Before fun preparar() {
        every { db.perfilDao() } returns dao;every { db.avatarDao() } returns avatar
        every { prefs.getString(any(),any()) } answers { textos[firstArg()] ?: secondArg() }
        every { prefs.edit() } returns editor
        every { editor.putString(any(),any()) } answers { textos[firstArg()]=secondArg();editor }
        every { editor.commit() } returns true
        coEvery { dao.obtener(any()) } answers { filas[firstArg()] }
        coEvery { dao.guardar(any()) } answers { firstArg<PerfilRemotoEntity>().let { filas[it.usuarioId]=it } }
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Any?>()) } coAnswers { secondArg<suspend () -> Any?>().invoke() }
        repo=DatosLocalesRemotosRepository(prefs,db)
    }
    @After fun limpiar() { unmockkAll() }
    @Test fun migraComplementosUnaVezYActualizaDatosRemotosSinPerderlos() = runTest {
        textos["perfil.7"]="""{"rut":"12.345.678-5","apellido":"Local"}"""
        assertEquals("Local",repo.perfil(usuario).apellido)
        textos["perfil.7"]="""{"rut":"anterior","apellido":"No usar"}"""
        val actualizado=repo.perfil(usuario.copy(nombre="Editado",rol="ADMIN",telefono=null))
        assertEquals("Editado",actualizado.nombre);assertTrue(actualizado.esAdmin)
        assertEquals("12.345.678-5",actualizado.rut);assertEquals("",actualizado.telefono)
        assertEquals("Local",actualizado.apellido)
    }
    @Test fun guardarAislaCuentasYValidaIdentidad() = runTest {
        val guardado=repo.guardarPerfil(usuario,"12.345.678-5","Apellido")
        assertEquals("Apellido",guardado.apellido)
        repo.guardarPerfil("8","11.111.111-1","Otro")
        assertEquals("Apellido",repo.perfil(usuario).apellido)
        assertEquals("Otro",filas[8]?.apellido)
        for(malo in listOf(usuario.copy(id=0),usuario.copy(rol="OTRO"))) {
            try { repo.perfil(malo);fail() } catch (_:IllegalArgumentException) { }
        }
        try { repo.guardarPerfil("x","","");fail() } catch (_:IllegalArgumentException) { }
    }
    @Test fun complementoCorruptoNoSeSobrescribeSilenciosamente() = runTest {
        for(texto in listOf("null","""{"rut":null,"apellido":"A"}""","""{"rut":"R","apellido":null}""")) {
            textos["perfil.7"]=texto
            try { repo.perfil(usuario);fail() } catch (_:IllegalArgumentException) { }
            assertFalse(filas.containsKey(7))
        }
    }
    @Test fun avatarConfirmaReferenciaAntesDeDevolverArchivoAnterior() = runTest {
        coEvery { avatar.obtener("remoto:7") } returns "anterior.jpg"
        assertEquals("anterior.jpg",repo.obtenerAvatar("remoto:7"))
        assertEquals("anterior.jpg",repo.reemplazarAvatar("remoto:7","nuevo.jpg"))
        coVerify { avatar.guardar(AvatarPerfilEntity("remoto:7","nuevo.jpg")) }
        repo.reemplazarAvatar("remoto:7",null);coVerify { avatar.quitar("remoto:7") }
        try { repo.reemplazarAvatar("7","x");fail() } catch (_:IllegalArgumentException) { }
    }
    @Test fun entregaSoloSeCompletaParaElDuenoYFalloDeDiscoSePropaga() = runTest {
        val entrega=EntregaPedido(ModalidadEntrega.DOMICILIO,"Cliente","912345678","Calle 1")
        val p=Pedido("PED-001","7",1,listOf(DetallePedido("4","A",1,1000)),1000,EstadoPedido.CONFIRMADO)
        repo.guardarEntrega(p.copy(entrega=entrega))
        assertEquals(entrega,repo.completarPedido(p).entrega)
        assertNull(repo.completarPedido(p.copy(usuarioId="8")).entrega)
        textos["pedido.PED-001"]="no-json";assertEquals(p,repo.completarPedido(p))
        every { editor.commit() } returns false
        try { repo.guardarEntrega(p.copy(entrega=entrega));fail() } catch (_:IllegalStateException) { }
    }
}
