package com.example.oyl_fs.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.oyl_fs.data.local.*
import com.example.oyl_fs.model.*
import com.example.oyl_fs.network.*
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CompraRemotaUnitTest {
    private val db=mockk<OylDatabase>()
    private val carrito=mockk<CarritoDao>(relaxed=true)
    private val aplicadas=mockk<CompraAplicadaDao>(relaxed=true)
    private val productos=mockk<ProductoRemoteRepository>()
    private val pedidos=PedidoRemoteRepository(mockk())
    private val extras=mockk<DatosLocalesRemotosRepository>(relaxed=true)
    private val intentos=mockk<IntentoCompraRepository>(relaxed=true)
    private val api=mockk<PedidosApi>()
    private val p=ProductoInfo(4,"Almendras","ALM","Frutos secos","D","500 g",1000,10,2,true)
    private val item=ItemCarrito("4","Almendras","500 g",1000,2,10)
    private val entrega=EntregaPedido(ModalidadEntrega.DOMICILIO," Cliente ","912345678"," Calle 1 ")
    private val dto=PedidoDto(9,7,"2026-11-24T10:00:00","PENDIENTE_VALIDACION",2000,listOf(DetallePedidoDto(4,"Almendras",1000,2,2000)))
    private var pendiente:IntentoCompra?=null
    private var activo:Long?=7
    private lateinit var repo:CompraRemotaRepository
    @Before fun preparar() {
        every { db.carritoDao() } returns carrito;every { db.compraAplicadaDao() } returns aplicadas
        every { db.puntoRetiroDao() } returns mockk(relaxed=true)
        coEvery { productos.listar() } returns listOf(p)
        coEvery { carrito.obtener("7",4) } returns CarritoEntity("7",4,"Almendras","500 g",1000,2,null,null)
        coEvery { intentos.obtener("7") } answers { pendiente }
        coEvery { intentos.guardar(any()) } answers { pendiente=firstArg() }
        coEvery { intentos.quitar(any()) } answers { pendiente=null }
        coEvery { api.crear(any(),any()) } returns SolicitudCompraDto("COMPLETADA",null,dto)
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Any?>()) } coAnswers { secondArg<suspend () -> Any?>().invoke() }
        repo=CompraRemotaRepository(mockk<Context>(),db,productos,pedidos,extras,intentos,api) { activo }
    }
    @After fun limpiar() { unmockkAll() }
    private suspend fun falla(bloque:suspend ()->Unit) {
        try { bloque();fail("Debía conservar el intento o rechazar los datos") } catch (_:IllegalArgumentException) { } catch (_:IllegalStateException) { }
    }
    @Test fun confirmacionPersisteAntesDeEnviarYAplicaCarritoUnaVez()=runTest {
        coEvery { api.crear(any(),any()) } coAnswers {
            assertEquals(pendiente?.clave,firstArg<String>())
            assertEquals("Calle 1",pendiente?.entrega?.direccion)
            SolicitudCompraDto("COMPLETADA",null,dto)
        }
        val resultado=repo.confirmar("7",listOf(item),entrega)
        assertEquals("PED-009",resultado.pedido.id);assertNull(resultado.advertencia);assertNull(pendiente)
        coVerifyOrder { intentos.guardar(any());api.crear(any(),any());extras.guardarEntrega(any());aplicadas.guardar(any());intentos.quitar(any()) }
        coVerify { carrito.descontarComprados("7",4,2) }
    }
    @Test fun datosInvalidosOCarritoModificadoNuncaEnvian()=runTest {
        falla { repo.confirmar("8",listOf(item),entrega) }
        falla { repo.confirmar("7",emptyList(),entrega) }
        falla { repo.confirmar("7",listOf(item,item),entrega) }
        falla { repo.confirmar("7",listOf(item),entrega.copy(direccion="")) }
        for(malo in listOf(item.copy(productoId="99"),item.copy(cantidad=11),item.copy(cantidad=0),item.copy(precio=5)))
            falla { repo.confirmar("7",listOf(malo),entrega) }
        coEvery { carrito.obtener("7",4) } returns null
        falla { repo.confirmar("7",listOf(item),entrega) }
        coVerify(exactly=0) { api.crear(any(),any()) }
    }
    @Test fun falloDeDiscoPrevioImpidePostYPerdidaHttpConservaIntento()=runTest {
        coEvery { intentos.guardar(any()) } throws IOException()
        try { repo.confirmar("7",listOf(item),entrega);fail() } catch (_:IOException) { }
        coVerify(exactly=0) { api.crear(any(),any()) }
        coEvery { intentos.guardar(any()) } answers { pendiente=firstArg() }
        coEvery { api.crear(any(),any()) } throws IOException()
        try { repo.confirmar("7",listOf(item),entrega);fail() } catch (_:IOException) { }
        assertNotNull(repo.pendiente("7"));falla { repo.confirmar("7",listOf(item),entrega) }
        val clave=pendiente!!.clave
        coEvery { api.consultarSolicitud(clave) } returns SolicitudCompraDto("NO_RECIBIDA",null,null)
        assertNull(repo.consultar("7").confirmada);assertEquals(clave,pendiente?.clave)
        coEvery { api.crear(clave,any()) } returns SolicitudCompraDto("COMPLETADA",null,dto)
        assertNotNull(repo.continuar("7").confirmada)
        coVerify(exactly=1) { api.consultarSolicitud(clave) }
    }
    @Test fun comprobantesInvalidosNoBorranIntentoNiCarrito()=runTest {
        val intento=IntentoCompra("7","clave",listOf(item),entrega)
        for(malo in listOf(dto.copy(usuarioId=8),dto.copy(total=1999),dto.copy(detalles=emptyList()),dto.copy(detalles=listOf(dto.detalles.single().copy(cantidad=1))))) {
            pendiente=intento;coEvery { api.consultarSolicitud("clave") } returns SolicitudCompraDto("COMPLETADA",null,malo)
            falla { repo.consultar("7") };assertNotNull(pendiente)
        }
        coVerify(exactly=0) { carrito.descontarComprados(any(),any(),any()) }
        coVerify(exactly=0) { intentos.quitar(any()) }
    }
    @Test fun estadosDurablesFinalizanYEstadosInciertosConservanElIntento()=runTest {
        val intento=IntentoCompra("7","clave",listOf(item),entrega)
        for(estado in listOf("EN_REVISION","NO_RECIBIDA")) {
            pendiente=intento;coEvery { api.consultarSolicitud("clave") } returns SolicitudCompraDto(estado,null,null)
            assertNotNull(repo.consultar("7").mensaje);assertNotNull(pendiente)
        }
        coEvery { api.consultarSolicitud("clave") } returns SolicitudCompraDto("DESCONOCIDO",null,null)
        falla { repo.consultar("7") };assertNotNull(pendiente)
        for(estado in listOf("RECHAZADA","FINALIZADA_SIN_PEDIDO")) {
            pendiente=intento;coEvery { api.consultarSolicitud("clave") } returns SolicitudCompraDto(estado,null,null)
            assertNotNull(repo.consultar("7").mensaje);assertNull(pendiente)
        }
    }
    @Test fun falloLocalTrasConfirmacionConservaComprobanteYPermiteCompletarSinDuplicarCarrito()=runTest {
        pendiente=IntentoCompra("7","clave",listOf(item),entrega)
        coEvery { api.consultarSolicitud("clave") } returns SolicitudCompraDto("COMPLETADA",null,dto)
        coEvery { extras.guardarEntrega(any()) } throws IOException()
        assertNotNull(repo.consultar("7").confirmada?.advertencia);assertNotNull(pendiente)
        coEvery { extras.guardarEntrega(any()) } returns Unit
        coEvery { aplicadas.existe("clave","7") } returns true
        assertNull(repo.consultar("7").confirmada?.advertencia);assertNull(pendiente)
        coVerify(exactly=0) { carrito.descontarComprados(any(),any(),any()) }
        pendiente=IntentoCompra("7","clave",listOf(item),entrega)
        coEvery { extras.guardarEntrega(any()) } throws CancellationException()
        try { repo.consultar("7");fail() } catch (_:CancellationException) { }
        assertNotNull(pendiente)
    }
}
