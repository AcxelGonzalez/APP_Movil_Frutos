package com.example.oyl_fs.viewmodel

import androidx.lifecycle.ViewModelStore
import com.example.oyl_fs.model.*
import com.example.oyl_fs.repository.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class InventarioRecuperacionUnitTest {
    private val dispatcher=StandardTestDispatcher()
    private val store=ViewModelStore()
    private val remoto=mockk<ProductoRemoteRepository>()
    private val local=mockk<ProductoRoomRepository>()
    private val movimientos=mockk<MovimientoInventarioRepository>(relaxed=true)
    private val p=ProductoInfo(4,"Almendras","ALM","Frutos secos","Descripción","500 g",1000,10,2,true,imagen=1)
    private var sesion:SesionActiva?=SesionActiva(SesionIdentificada("1","Admin",true,false),"s1",Long.MAX_VALUE)
    private fun registro(estado:String="EN_REVISION")=MovimientoInventarioGuardado("1","clave",p,3,estado,
        if(estado=="APLICADA")10 else null,if(estado=="APLICADA")13 else null,"Aviso")
    private fun vm(room:Boolean=false)=ProductoViewModel(if(room)local else null,if(room)null else remoto,movimientos).also { store.put("vm",it) }
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher);mockkObject(SesionRemotaRepository)
        every { SesionRemotaRepository.sesionActual() } answers { sesion }
        coEvery { remoto.listar() } returns listOf(p)
        coEvery { remoto.cargarCache() } returns listOf(p)
        every { local.productos } returns flowOf(listOf(p))
        coEvery { local.cargarProductos() } returns listOf(p)
        coEvery { movimientos.obtener("1") } returns null
    }
    @After fun limpiar() { store.clear();unmockkAll();Dispatchers.resetMain() }
    @Test fun restauraPendienteSinReenviarYCierraSoloResultadosFinales() = runTest {
        coEvery { movimientos.obtener("1") } returns registro()
        val vm=vm();vm.configurarSesionInventario();advanceUntilIdle()
        assertTrue(vm.movimientoStock.value.resultadoIncierto)
        vm.cerrarMovimientoStock();coVerify(exactly=0) { movimientos.cerrar(any(),any()) }
        coEvery { movimientos.recuperar("1","clave",false) } returns registro("APLICADA")
        vm.recuperarMovimientoStock(false);advanceUntilIdle()
        assertEquals(13,vm.movimientoStock.value.confirmado?.stock)
        assertEquals(10,vm.productos.value.single().stock)
        vm.cerrarMovimientoStock();advanceUntilIdle();assertNull(vm.movimientoStock.value.pendiente)
        coVerify(exactly=0) { movimientos.iniciar(any(),any(),any()) }
    }
    @Test fun iniciarValidaCantidadYOperacionEnCursoNoSeDuplica() = runTest {
        val vm=vm();advanceUntilIdle();vm.configurarSesionInventario();advanceUntilIdle()
        vm.iniciarMovimientoStock(4);advanceUntilIdle()
        vm.registrarMovimientoStock(4,TipoMovimientoStock.ENTRADA,"0");assertNotNull(vm.movimientoStock.value.error)
        val resultado=CompletableDeferred<MovimientoInventarioGuardado>()
        coEvery { movimientos.iniciar("1",p,3) } coAnswers { resultado.await() }
        vm.registrarMovimientoStock(4,TipoMovimientoStock.ENTRADA,"3");runCurrent()
        assertTrue(vm.movimientoStock.value.procesando)
        vm.registrarMovimientoStock(4,TipoMovimientoStock.ENTRADA,"3");vm.iniciarMovimientoStock(4)
        resultado.complete(registro("APLICADA"));advanceUntilIdle()
        coVerify(exactly=1) { movimientos.iniciar("1",p,3) }
        assertFalse(vm.movimientoStock.value.resultadoIncierto)
    }
    @Test fun errorHttpRecuperaRegistroSinCrearOtroYBloqueaNuevaOperacion() = runTest {
        val vm=vm();advanceUntilIdle();vm.configurarSesionInventario();advanceUntilIdle()
        coEvery { movimientos.iniciar(any(),any(),any()) } throws IOException()
        coEvery { movimientos.obtener("1") } returns registro()
        vm.registrarMovimientoStock(4,TipoMovimientoStock.SALIDA,"3");advanceUntilIdle()
        assertTrue(vm.movimientoStock.value.resultadoIncierto)
        assertTrue(vm.movimientoStock.value.error.orEmpty().contains("guardado"))
        vm.registrarMovimientoStock(4,TipoMovimientoStock.ENTRADA,"3")
        coVerify(exactly=1) { movimientos.iniciar(any(),any(),-3) }
    }
    @Test fun falloDeLecturaBloqueaEscrituraYPermiteReleer() = runTest {
        coEvery { movimientos.obtener("1") } throws IOException()
        val vm=vm();vm.configurarSesionInventario();advanceUntilIdle()
        assertFalse(vm.movimientoStock.value.registroLegible)
        vm.registrarMovimientoStock(4,TipoMovimientoStock.ENTRADA,"3")
        coVerify(exactly=0) { movimientos.iniciar(any(),any(),any()) }
        coEvery { movimientos.obtener("1") } returns null
        vm.recuperarMovimientoStock(false);advanceUntilIdle()
        assertTrue(vm.movimientoStock.value.registroLegible)
        coEvery { movimientos.iniciar(any(),any(),any()) } throws IOException()
        vm.registrarMovimientoStock(4,TipoMovimientoStock.ENTRADA,"3");advanceUntilIdle()
        assertTrue(vm.movimientoStock.value.error.orEmpty().contains("No se envió"))
    }
    @Test fun cambioDeSesionDescartaRespuestaTardia() = runTest {
        val pendiente=CompletableDeferred<MovimientoInventarioGuardado?>()
        coEvery { movimientos.obtener("1") } coAnswers { pendiente.await() }
        val vm=vm();vm.configurarSesionInventario();runCurrent()
        sesion=null;vm.configurarSesionInventario();pendiente.complete(registro("APLICADA"));advanceUntilIdle()
        assertEquals(MovimientoStockUiState(),vm.movimientoStock.value)
        vm.registrarMovimientoStock(4,TipoMovimientoStock.ENTRADA,"3")
        assertTrue(vm.movimientoStock.value.error.orEmpty().contains("conexión"))
    }
    @Test fun resultadoFinalConFalloDeCatalogoOCierreConservaComprobante() = runTest {
        coEvery { movimientos.obtener("1") } returns registro("APLICADA")
        coEvery { movimientos.recuperar(any(),any(),true) } returns registro("APLICADA")
        val vm=vm();vm.configurarSesionInventario();advanceUntilIdle()
        coEvery { remoto.listar() } throws IOException()
        vm.recuperarMovimientoStock(true);advanceUntilIdle()
        assertNotNull(vm.movimientoStock.value.advertencia)
        coEvery { movimientos.cerrar(any(),any()) } throws IOException()
        vm.cerrarMovimientoStock();advanceUntilIdle()
        assertNotNull(vm.movimientoStock.value.pendiente);assertNotNull(vm.movimientoStock.value.advertencia)
        assertFalse(vm.movimientoStock.value.procesando)
    }
    @Test fun crudLocalRespetaDuplicadosExistenciaYErroresDeAlmacenamiento() = runTest {
        val vm=vm(true);advanceUntilIdle()
        coEvery { local.insertarYObtener(any()) } returns p
        assertFalse(vm.agregarProductoPersistente(p))
        coEvery { local.cargarProductos() } returns emptyList()
        assertTrue(vm.agregarProductoPersistente(p))
        coEvery { local.obtenerPorId(4) } returns null
        assertFalse(vm.editarProductoPersistente(p))
        coEvery { local.obtenerPorId(4) } returns p
        coEvery { local.cargarProductos() } returns listOf(p.copy(id=5))
        assertFalse(vm.editarProductoPersistente(p))
        coEvery { local.cargarProductos() } returns listOf(p)
        coEvery { local.actualizar(any()) } returnsMany listOf(false,true)
        assertFalse(vm.editarProductoPersistente(p));assertTrue(vm.editarProductoPersistente(p))
        coEvery { local.eliminar(4) } returnsMany listOf(false,true)
        assertFalse(vm.eliminarProductoPersistente(4));assertTrue(vm.eliminarProductoPersistente(4))
        coEvery { local.insertarYObtener(any()) } throws IOException()
        coEvery { local.cargarProductos() } returns emptyList()
        assertFalse(vm.agregarProductoPersistente(p))
        coEvery { local.actualizar(any()) } throws IOException()
        assertFalse(vm.editarProductoPersistente(p))
        coEvery { local.eliminar(4) } throws IOException()
        assertFalse(vm.eliminarProductoPersistente(4))
    }
    @Test fun stockPersistenteRefrescaSoloAlConfirmarYFalloDelFlowTerminaCarga() = runTest {
        val vm=vm(true);advanceUntilIdle()
        val item=ItemCarrito("4","A","500 g",1000,1,10)
        coEvery { local.descontarStockPorCompra(any()) } returnsMany listOf(true,false)
        coEvery { local.cargarProductos() } returns listOf(p.copy(stock=9))
        assertTrue(vm.descontarStockPorCompraPersistente(listOf(item)));assertEquals(9,vm.productos.value.single().stock)
        assertFalse(vm.descontarStockPorCompraPersistente(listOf(item)));assertNotNull(vm.error.value)
        coEvery { local.descontarStockPorCompra(any()) } throws IOException()
        assertFalse(vm.descontarStockPorCompraPersistente(listOf(item)))
        every { local.productos } returns flow { throw IOException() }
        val fallido=vm(true);advanceUntilIdle();assertFalse(fallido.cargandoProductos.value);assertNotNull(fallido.error.value)
    }
}
