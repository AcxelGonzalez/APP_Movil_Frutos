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
class PedidoRemoteDesarrolloTest {

    private val api: PedidosApi = mockk()

    private fun item(productoId: String, cantidad: Int = 1) =
        ItemCarrito(productoId, "Almendras", "500 g", 6990, cantidad, 60)

    @Test
    fun elCarritoSeConvierteEnLaSolicitudQueEsperaElBackend() = runBlocking {
        val repo = PedidoRemoteRepository(api)
        val enviado = slot<CrearPedidoRequest>()
        coEvery { api.crear(any(), capture(enviado)) } returns com.example.oyl_fs.network.SolicitudCompraDto("COMPLETADA", null, com.example.oyl_fs.network.PedidoDto(21, 7, "2026-11-23T12:00:00", "PENDIENTE_VALIDACION", 13980, emptyList()))

        val resultado = repo.crearPedido(7L, listOf(item("3", 2)), "ef074633-20ac-4b27-b54c-3b73a095ed66")

        assertTrue(resultado is ResultadoPedido.Exito)
        assertEquals(7L, enviado.captured.usuarioId)
        assertEquals(3L, enviado.captured.items.single().productoId)
        assertEquals(2, enviado.captured.items.single().cantidad)
    }

    @Test
    fun unIdDeProductoNoNumericoSeRechazaSinLlamarAlBackend() = runBlocking {
        val repo = PedidoRemoteRepository(api)

        val resultado = repo.crearPedido(7L, listOf(item("OYL001")), "ef074633-20ac-4b27-b54c-3b73a095ed66")

        assertTrue(resultado is ResultadoPedido.Error)
        assertTrue((resultado as ResultadoPedido.Error).mensaje.contains("Almendras"))
        coVerify(exactly = 0) { api.crear(any(), any()) }
    }

    @Test
    fun sinConexionDevuelveUnMensajeLegibleParaElUsuario() = runBlocking {
        val repo = PedidoRemoteRepository(api)
        coEvery { api.crear(any(), any()) } throws UnknownHostException("sin red")

        val resultado = repo.crearPedido(7L, listOf(item("3")), "ef074633-20ac-4b27-b54c-3b73a095ed66")

        assertTrue(resultado is ResultadoPedido.Error)
        assertTrue((resultado as ResultadoPedido.Error).mensaje.contains("Revisa Mis pedidos"))
    }
}