package com.example.oyl_fs.repository

import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.ItemCarrito
import com.example.oyl_fs.network.*
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class PedidoRemoteRepositoryTest {
    private class Api : PedidosApi {
        var solicitud: CrearPedidoRequest? = null
        var envios = 0
        var error: Exception? = null
        var pedido = PedidoDto(12, 7, "2026-11-21T12:30:00.123456", "PENDIENTE_VALIDACION", 2000,
            listOf(DetallePedidoDto(3, "Almendras", 1000, 2, 2000)))
        var cambioId: Long? = null
        override suspend fun crear(pedido: CrearPedidoRequest): PedidoDto {
            envios++; solicitud = pedido
            error?.let { throw it }
            return this.pedido
        }
        override suspend fun listar() = listOf(pedido)
        override suspend fun listarPorUsuario(usuarioId: Long) = listOf(pedido)
        override suspend fun obtener(id: Long) = pedido
        override suspend fun cambiarEstado(id: Long, request: CambiarEstadoRequest): PedidoDto {
            cambioId = id
            return pedido.copy(estado = request.estado)
        }
        override suspend fun cancelar(id: Long): Response<Unit> = Response.success(Unit)
    }
    private val item = ItemCarrito("3", "Almendras", "500 g", 1000, 2, 10)

    @Test fun crearEnviaIdentidadYCantidadesYElServidorDevuelveElNumero() = runBlocking {
        val api = Api()
        val repo = PedidoRemoteRepository(api)
        val resultado = repo.crearPedido(7, listOf(item)) as ResultadoPedido.Exito
        assertEquals(CrearPedidoRequest(7, listOf(ItemPedidoRequest(3, 2))), api.solicitud)
        val json = Gson().toJson(api.solicitud)
        assertFalse(json.contains("precio"))
        assertFalse(json.contains("total"))
        val convertido = with(repo) { resultado.pedido.aPedido() }
        assertEquals("PED-012", convertido.id)
        assertEquals("7", convertido.usuarioId)
        assertEquals(2000, convertido.total)
        assertTrue(convertido.fecha > 0)
        assertEquals(1, api.envios)
    }

    @Test fun rechazaCarritoVacioIdentidadesLocalesYCantidadesInvalidasSinEnviar() = runBlocking {
        val api = Api(); val repo = PedidoRemoteRepository(api)
        assertTrue(repo.crearPedido(7, emptyList()) is ResultadoPedido.Error)
        assertTrue(repo.crearPedido(0, listOf(item)) is ResultadoPedido.Error)
        assertTrue(repo.crearPedido(7, listOf(item.copy(productoId = "local"))) is ResultadoPedido.Error)
        assertTrue(repo.crearPedido(7, listOf(item.copy(productoId = "-1"))) is ResultadoPedido.Error)
        assertTrue(repo.crearPedido(7, listOf(item.copy(cantidad = 0))) is ResultadoPedido.Error)
        assertEquals(0, api.envios)
    }

    @Test fun respuestaPerdidaNoReintentaElPostYAdvierteRevisarPedidos() = runBlocking {
        val api = Api().apply { error = IOException("sin respuesta") }
        val resultado = PedidoRemoteRepository(api).crearPedido(7, listOf(item)) as ResultadoPedido.Error
        assertTrue(resultado.mensaje.contains("Revisa Mis pedidos"))
        assertEquals(1, api.envios)
    }

    @Test fun datosFueraDeRangoYEstadosDesconocidosNoSeMuestranComoPendientes() = runBlocking {
        val api = Api(); val repo = PedidoRemoteRepository(api)
        api.pedido = api.pedido.copy(total = Int.MAX_VALUE.toLong() + 1)
        assertTrue(repo.listarTodos().isFailure)
        api.pedido = api.pedido.copy(total = 2000, estado = "INVENTADO")
        assertTrue(repo.listarPorUsuario(7).isFailure)
    }

    @Test fun cambioDeEstadoUsaElIdNumericoDelServidor() = runBlocking {
        val api = Api()
        val actualizado = PedidoRemoteRepository(api).cambiarEstado("PED-012", EstadoPedido.CONFIRMADO).getOrThrow()
        assertEquals(12L, api.cambioId)
        assertEquals(EstadoPedido.CONFIRMADO, actualizado.estado)
    }

    @Test fun cancelacionDeCorrutinaNoSeConvierteEnErrorDeNegocio() = runBlocking {
        val api = Api().apply { error = CancellationException("cancelada") }
        try {
            PedidoRemoteRepository(api).crearPedido(7, listOf(item))
            fail("Debe propagar la cancelación")
        } catch (_: CancellationException) { }
    }
}
