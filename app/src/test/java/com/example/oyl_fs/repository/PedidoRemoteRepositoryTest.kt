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
        override suspend fun crear(clave: String, pedido: CrearPedidoRequest): SolicitudCompraDto {
            envios++; solicitud = pedido
            error?.let { throw it }
            return SolicitudCompraDto("COMPLETADA", null, this.pedido)
        }
        override suspend fun consultarSolicitud(clave: String) = SolicitudCompraDto("COMPLETADA", null, pedido)
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
        val resultado = repo.crearPedido(7, listOf(item), "ef074633-20ac-4b27-b54c-3b73a095ed66") as ResultadoPedido.Exito
        assertEquals(CrearPedidoRequest(7, listOf(ItemPedidoRequest(3, 2, 1000))), api.solicitud)
        val json = Gson().toJson(api.solicitud)
        assertTrue(json.contains("precioEsperado"))
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
        assertTrue(repo.crearPedido(7, emptyList(), "ef074633-20ac-4b27-b54c-3b73a095ed66") is ResultadoPedido.Error)
        assertTrue(repo.crearPedido(0, listOf(item), "ef074633-20ac-4b27-b54c-3b73a095ed66") is ResultadoPedido.Error)
        assertTrue(repo.crearPedido(7, listOf(item.copy(productoId = "local")), "ef074633-20ac-4b27-b54c-3b73a095ed66") is ResultadoPedido.Error)
        assertTrue(repo.crearPedido(7, listOf(item.copy(productoId = "-1")), "ef074633-20ac-4b27-b54c-3b73a095ed66") is ResultadoPedido.Error)
        assertTrue(repo.crearPedido(7, listOf(item.copy(cantidad = 0)), "ef074633-20ac-4b27-b54c-3b73a095ed66") is ResultadoPedido.Error)
        assertEquals(0, api.envios)
    }

    @Test fun respuestaPerdidaNoReintentaElPostYAdvierteRevisarPedidos() = runBlocking {
        val api = Api().apply { error = IOException("sin respuesta") }
        val resultado = PedidoRemoteRepository(api).crearPedido(7, listOf(item), "ef074633-20ac-4b27-b54c-3b73a095ed66") as ResultadoPedido.Error
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
            PedidoRemoteRepository(api).crearPedido(7, listOf(item), "ef074633-20ac-4b27-b54c-3b73a095ed66")
            fail("Debe propagar la cancelación")
        } catch (_: CancellationException) { }
    }
}
