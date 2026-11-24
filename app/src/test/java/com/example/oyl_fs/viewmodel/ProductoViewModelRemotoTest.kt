package com.example.oyl_fs.viewmodel

import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.repository.ProductoRemoteRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/*
Revisa que el inventario del administrador funcione contra el servidor:
que cargue los productos al abrirse, que avise cuando el servidor no
responde, y que al agregar, editar o eliminar un producto se lo mande
al backend y vuelva a pedir la lista actualizada.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductoViewModelRemotoTest {

    private val almacen = ViewModelStore()
    private var siguiente = 0
    private fun viewModel() = ProductoViewModel(remoteRepository = remoto).also { almacen.put((siguiente++).toString(), it) }

    private val remoto: ProductoRemoteRepository = mockk()

    private val almendras = producto(1, "Almendras", "OYL001")
    private val nueces = producto(2, "Nueces", "OYL002")

    @Before
    fun prepararHiloPrincipal() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun liberarHiloPrincipal() {
        almacen.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun alAbrirseCargaLosProductosDelServidor() = runBlocking {
        coEvery { remoto.listar() } returns listOf(almendras, nueces)

        val vm = viewModel()

        assertEquals(2, vm.productos.value.size)
        assertFalse(vm.cargandoProductos.value)
        assertNull(vm.error.value)
    }

    @Test
    fun siElServidorNoRespondeMuestraUnMensaje() = runBlocking {
        coEvery { remoto.listar() } throws java.net.ConnectException()
        coEvery { remoto.cargarCache() } returns emptyList()

        val vm = viewModel()

        assertTrue(vm.productos.value.isEmpty())
        assertFalse(vm.cargandoProductos.value)
        assertNotNull(vm.errorCatalogo.value)
    }

    @Test
    fun agregarUnProductoLoEnviaAlServidorYRecargaLaLista() = runBlocking {
        coEvery { remoto.listar() } returns listOf(almendras)
        val vm = viewModel()

        coEvery { remoto.crear(any()) } returns nueces
        coEvery { remoto.listar() } returns listOf(almendras, nueces)

        val agregado = vm.agregarProductoPersistente(nueces)

        assertTrue(agregado)
        coVerify { remoto.crear(any()) }
        assertEquals(2, vm.productos.value.size)
    }

    @Test
    fun unProductoConPrecioInvalidoNoLlegaAlServidor() = runBlocking {
        coEvery { remoto.listar() } returns listOf(almendras)
        val vm = viewModel()

        val agregado = vm.agregarProductoPersistente(
            nueces.copy(precio = 0)
        )

        assertFalse(agregado)
        assertEquals("Ingresa un precio mayor que cero.", vm.error.value)
        coVerify(exactly = 0) { remoto.crear(any()) }
    }

    @Test
    fun editarUnProductoLoEnviaAlServidor() = runBlocking {
        coEvery { remoto.listar() } returns listOf(almendras)
        val vm = viewModel()

        val editado = almendras.copy(precio = 7990)
        coEvery { remoto.actualizar(any()) } returns editado
        coEvery { remoto.listar() } returns listOf(editado)

        val resultado = vm.editarProductoPersistente(editado)

        assertTrue(resultado)
        coVerify { remoto.actualizar(any()) }
        assertEquals(7990, vm.productos.value.single().precio)
    }

    @Test
    fun eliminarUnProductoLoQuitaDeLaLista() = runBlocking {
        coEvery { remoto.listar() } returns listOf(almendras)
        val vm = viewModel()

        coEvery { remoto.eliminarRegistrado(1) } returns ProductoRemoteRepository.EliminacionRegistrada(true, true)
        coEvery { remoto.listar() } returns emptyList()

        val eliminado = vm.eliminarProductoPersistente(1)

        assertTrue(eliminado)
        assertTrue(vm.productos.value.isEmpty())
    }

    @Test
    fun eliminarUnProductoAusenteConfirmaElRetiroYQuitaLaFichaObsoleta() = runBlocking {
        coEvery { remoto.listar() } returns listOf(almendras)
        val vm = viewModel()

        coEvery { remoto.eliminarRegistrado(1) } returns ProductoRemoteRepository.EliminacionRegistrada(false, true)
        coEvery { remoto.listar() } returns emptyList()

        val eliminado = vm.eliminarProductoPersistente(1)

        assertTrue(eliminado)
        assertTrue(vm.productos.value.isEmpty())
        assertNull(vm.error.value)
    }

    private fun producto(id: Int, nombre: String, codigo: String) = ProductoInfo(
        id = id,
        nombre = nombre,
        codigo = codigo,
        categoria = "Frutos secos",
        descripcion = "Bolsa de 500 g",
        presentacion = "500 g",
        precio = 6990,
        stock = 60,
        stockMinimo = 2,
        disponible = true
    )
}
