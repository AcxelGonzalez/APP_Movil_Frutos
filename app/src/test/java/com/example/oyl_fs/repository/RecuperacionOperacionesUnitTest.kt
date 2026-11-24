package com.example.oyl_fs.repository

import androidx.room.withTransaction
import com.example.oyl_fs.data.local.*
import com.example.oyl_fs.model.*
import com.example.oyl_fs.network.*
import com.example.oyl_fs.viewmodel.SesionIdentificada
import com.google.gson.Gson
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.UUID

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RecuperacionOperacionesUnitTest {
    private val db=mockk<PerfilRemotoDatabase>()
    private val dao=mockk<MovimientoStockLocalDao>(relaxed=true)
    private val cancelaciones=mockk<CancelacionLocalDao>(relaxed=true)
    private val intentos=mockk<IntentoCompraDao>(relaxed=true)
    private val api=mockk<MovimientosStockApi>()
    private val apiCancelacion=mockk<CancelacionesApi>()
    private val producto=ProductoInfo(4,"Almendras","ALM","Frutos secos","D","500 g",1000,10,2,true)
    private val pedido=Pedido("PED-009","7",1,listOf(DetallePedido("4","Almendras",1,1000)),1000,EstadoPedido.PENDIENTE_VALIDACION)
    private val gson=Gson()
    private val clave=UUID.randomUUID().toString()
    private var sesion:SesionActiva?=SesionActiva(SesionIdentificada("1","Admin",true,false),"sesion",Long.MAX_VALUE)
    private var fila:MovimientoStockLocalEntity?=null
    private lateinit var repo:MovimientoInventarioRepository
    @Before fun preparar() {
        every { db.movimientoStockDao() } returns dao
        every { db.cancelacionDao() } returns cancelaciones
        every { db.intentoCompraDao() } returns intentos
        coEvery { dao.obtener(any()) } answers { fila }
        coEvery { dao.insertar(any()) } answers { fila=firstArg() }
        coEvery { dao.actualizar(any(),any(),any(),any(),any(),any()) } answers {
            fila=fila!!.copy(estado=arg(2),stockAnterior=arg(3),stockResultante=arg(4),mensaje=arg(5))
        }
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Any?>()) } coAnswers { secondArg<suspend () -> Any?>().invoke() }
        repo=MovimientoInventarioRepository(db,api) { sesion }
    }
    @After fun limpiar() { unmockkAll() }
    private fun local(estado:String="EN_REVISION")=MovimientoStockLocalEntity("1",clave,4,3,gson.toJson(producto),estado,
        if(estado=="APLICADA")10 else null,if(estado=="APLICADA")13 else null,null)
    private fun respuesta(estado:String="APLICADA")=MovimientoStockDto(1,clave,4,3,estado,
        if(estado=="APLICADA")10 else null,if(estado=="APLICADA")13 else null,null)
    private suspend fun falla(bloque:suspend ()->Unit) {
        try { bloque(); fail("Debía conservar el registro y rechazar la operación") } catch (_: IllegalArgumentException) { } catch (_: IllegalStateException) { }
    }
    @Test fun guardaAntesDelHttpYNoGeneraOtraClaveSiYaHayPendiente() = runTest {
        coEvery { api.continuar(any(),any()) } coAnswers {
            assertNotNull(fila)
            respuesta().copy(clave=firstArg())
        }
        val resultado=repo.iniciar("1",producto,3)
        assertTrue(resultado.finalizado);assertEquals(13,resultado.stockResultante)
        coVerifyOrder { dao.insertar(any());api.continuar(any(),MovimientoStockRequest(4,3));dao.actualizar(any(),any(),"APLICADA",10,13,null) }
        falla { repo.iniciar("1",producto,3) }
        repo.cerrar("1",resultado.clave);coVerify { dao.cerrar("1",resultado.clave) }
    }
    @Test fun perdidaDeRespuestaConservaClaveYConsultaSoloHaceGet() = runTest {
        coEvery { api.continuar(any(),any()) } throws IOException("Respuesta perdida")
        try { repo.iniciar("1",producto,3);fail() } catch (_:IOException) { }
        val guardada=requireNotNull(fila)
        coEvery { api.consultar(guardada.clave) } returns respuesta().copy(clave=guardada.clave)
        val recuperada=repo.recuperar("1",guardada.clave,false)
        assertEquals(guardada.clave,recuperada.clave);assertTrue(recuperada.finalizado)
        coVerify(exactly=1) { api.continuar(any(),any()) }
        repo.recuperar("1",guardada.clave,true)
        coVerify(exactly=1) { api.consultar(any()) }
    }
    @Test fun respuestaNoRegistradaMantieneRevisionYContinuarUsaMismaClave() = runTest {
        fila=local()
        coEvery { api.consultar(clave) } returns MovimientoStockDto(1,clave,null,null,"NO_REGISTRADA",null,null,null)
        assertFalse(repo.recuperar("1",clave,false).finalizado)
        assertEquals("EN_REVISION",fila!!.estado)
        coEvery { api.continuar(clave,MovimientoStockRequest(4,3)) } returns respuesta("RECHAZADA")
        assertTrue(repo.recuperar("1",clave,true).finalizado)
    }
    @Test fun noSobrescribeRegistroConComprobantesAjenosOCantidadesImposibles() = runTest {
        for(mala in listOf(respuesta().copy(actorId=7),respuesta().copy(clave=UUID.randomUUID().toString()),
            respuesta().copy(productoId=9),respuesta().copy(cambio=2),respuesta().copy(estado="OTRO"),
            respuesta().copy(stockResultante=12),respuesta().copy(stockAnterior=-1),
            respuesta("RECHAZADA").copy(stockResultante=13),respuesta().copy(estado="NO_REGISTRADA"))) {
            fila=local();coEvery { api.consultar(clave) } returns mala
            falla { repo.recuperar("1",clave,false) }
            assertEquals("EN_REVISION",fila!!.estado)
        }
        coVerify(exactly=0) { dao.actualizar(any(),any(),any(),any(),any(),any()) }
    }
    @Test fun permisosYDatosLocalesCorruptosImpidenOperar() = runTest {
        assertNull(repo.obtener("1"))
        for(cambio in listOf(0,Int.MIN_VALUE)) falla { repo.iniciar("1",producto,cambio) }
        falla { repo.iniciar("1",producto.copy(id=0),1) }
        fila=local();falla { repo.recuperar("1",UUID.randomUUID().toString(),true) }
        fila=local().copy(contenido=gson.toJson(producto.copy(id=5)))
        falla { repo.obtener("1") }
        fila=local("APLICADA").copy(stockResultante=12);falla { repo.obtener("1") }
        sesion=sesion!!.copy(identidad=SesionIdentificada("1","Cliente",false,false));falla { repo.obtener("1") }
        sesion=null;falla { repo.cerrar("1",clave) }
    }
    @Test fun cancelacionPersisteAntesDeConsultarYRepetirTerminalNoHaceHttp() = runTest {
        var guardada:CancelacionLocalEntity?=null
        coEvery { cancelaciones.insertar(any()) } answers { guardada=firstArg() }
        coEvery { cancelaciones.obtener("7",9) } answers { guardada }
        coEvery { cancelaciones.actualizar(any(),any(),any(),any()) } answers { guardada=guardada!!.copy(estado=arg(2),mensaje=arg(3)) }
        coEvery { apiCancelacion.consultar(9) } returns CancelacionDto(9,7,"NO_SOLICITADA",null)
        coEvery { apiCancelacion.continuar(9) } returns CancelacionDto(9,7,"COMPLETADA",null)
        val r=CancelacionPedidoRepository(db,apiCancelacion) { 7 }
        assertEquals("NO_SOLICITADA",r.iniciar("7",pedido,false).estado)
        assertEquals("EN_REVISION",guardada!!.estado)
        assertEquals("COMPLETADA",r.recuperar("7",9,true).estado)
        assertEquals("COMPLETADA",r.recuperar("7",9,true).estado)
        coVerify(exactly=1) { apiCancelacion.continuar(9) }
        every { cancelaciones.observar("7") } returns flowOf(listOf(guardada!!))
        val observado=r.observar("7").first().single()
        assertTrue(observado.finalizada);assertEquals(9L,observado.pedidoId)
        r.cerrar("7",9);coVerify { cancelaciones.cerrar("7",9) }
    }
    @Test fun cancelacionRechazaRespuestasAjenaYEstadosDesconocidos() = runTest {
        val fila=CancelacionLocalEntity("7",9,"7",gson.toJson(pedido),"EN_REVISION",null)
        coEvery { cancelaciones.obtener("7",9) } returns fila
        val r=CancelacionPedidoRepository(db,apiCancelacion) { 7 }
        for(mala in listOf(CancelacionDto(10,7,"COMPLETADA",null),CancelacionDto(9,8,"COMPLETADA",null),CancelacionDto(9,7,"OTRA",null))) {
            coEvery { apiCancelacion.consultar(9) } returns mala
            falla { r.recuperar("7",9,false) }
        }
        falla { r.iniciar("7",pedido.copy(id="x"),true) }
        falla { r.cerrar("8",9) }
        every { cancelaciones.observar("7") } returns flowOf(listOf(fila.copy(propietarioId="8")))
        falla { r.observar("7").first() }
        coVerify(exactly=0) { cancelaciones.actualizar(any(),any(),any(),any()) }
    }
    @Test fun intentoCompraSeRecuperaSoloParaSuCuentaYClaveOriginal() = runTest {
        val r=IntentoCompraRepository(db)
        val intento=IntentoCompra("7",clave,listOf(ItemCarrito("4","Almendras","500 g",1000,1,10)),
            EntregaPedido(ModalidadEntrega.DOMICILIO,"Cliente","912345678","Calle 1"))
        var fila:IntentoCompraEntity?=null
        coEvery { intentos.obtener("7") } answers { fila }
        coEvery { intentos.guardar(any()) } answers { fila=firstArg() }
        assertNull(r.obtener("7"));r.guardar(intento);assertEquals(intento,r.obtener("7"))
        r.quitar(intento);coVerify { intentos.quitar("7",clave) }
        for(malo in listOf(intento.copy(usuarioId="8"),intento.copy(clave=UUID.randomUUID().toString()),intento.copy(items=emptyList()))) {
            fila=IntentoCompraEntity("7",clave,gson.toJson(malo));falla { r.obtener("7") }
        }
    }
}
