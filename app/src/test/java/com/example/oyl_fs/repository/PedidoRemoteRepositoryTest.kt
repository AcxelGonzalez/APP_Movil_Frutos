package com.example.oyl_fs.repository

import com.example.oyl_fs.model.ItemCarrito
import com.example.oyl_fs.network.CrearPedidoRequest
import com.example.oyl_fs.network.PedidosApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.UnknownHostException

/*
 Revisa que al comprar, los productos del carrito lleguen bien al servidor con su cantidad correcta.
 también revisa que si el servidor está apagado, el usuario vea un aviso claro y no un error raro.
 */
class PedidoRemoteRepositoryTest {

    private val api: PedidosApi = mockk()

    private fun item(productoId: String, cantidad: Int = 1) =
        ItemCarrito(productoId, "Almendras", "500 g", 6990, cantidad, 60)

    @Test
    fun elCarritoSeConvierteEnLaSolicitudQueEsperaElBackend() = runBlocking {
        PedidoRemoteRepository.api = api
        val enviado = slot<CrearPedidoRequest>()
        coEvery { api.crear(capture(enviado)) } returns mockk()

        val resultado = PedidoRemoteRepository.crearPedido(7L, listOf(item("3", 2)))

        assertTrue(resultado is ResultadoPedido.Exito)
        assertEquals(7L, enviado.captured.usuarioId)
        assertEquals(3L, enviado.captured.items.single().productoId)
        assertEquals(2, enviado.captured.items.single().cantidad)
    }

    @Test
    fun unIdDeProductoNoNumericoSeRechazaSinLlamarAlBackend() = runBlocking {
        PedidoRemoteRepository.api = api

        val resultado = PedidoRemoteRepository.crearPedido(7L, listOf(item("OYL001")))

        assertTrue(resultado is ResultadoPedido.Error)
        assertTrue((resultado as ResultadoPedido.Error).mensaje.contains("Almendras"))
        coVerify(exactly = 0) { api.crear(any()) }
    }

    @Test
    fun sinConexionDevuelveUnMensajeLegibleParaElUsuario() = runBlocking {
        PedidoRemoteRepository.api = api
        coEvery { api.crear(any()) } throws UnknownHostException("sin red")

        val resultado = PedidoRemoteRepository.crearPedido(7L, listOf(item("3")))

        assertTrue(resultado is ResultadoPedido.Error)
        assertTrue((resultado as ResultadoPedido.Error).mensaje.contains("conectar con el servidor"))
    }
}