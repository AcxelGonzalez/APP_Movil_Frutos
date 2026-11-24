package com.example.oyl_fs.repository
import android.content.Context
import com.example.oyl_fs.data.local.*
import com.example.oyl_fs.model.*
import com.google.gson.Gson
import java.util.UUID

data class IntentoCompra(val usuarioId: String, val clave: String, val items: List<ItemCarrito>, val entrega: EntregaPedido)

/** Base privada excluida de copias y transferencia: contiene la entrega del intento. */
class IntentoCompraRepository(database: PerfilRemotoDatabase) {
    constructor(context: Context) : this(PerfilRemotoDatabase.obtener(context))
    private val dao = database.intentoCompraDao()
    private val gson = Gson()
    suspend fun obtener(usuarioId: String): IntentoCompra? = dao.obtener(usuarioId)?.let { fila ->
        val intento = requireNotNull(gson.fromJson(fila.contenido, IntentoCompra::class.java))
        check(intento.usuarioId == usuarioId && intento.clave == fila.clave && intento.items.isNotEmpty()) {
            "No se pudo recuperar el intento guardado. No vuelvas a comprar hasta revisar Mis pedidos."
        }
        UUID.fromString(intento.clave)
        intento
    }
    suspend fun guardar(intento: IntentoCompra) = dao.guardar(IntentoCompraEntity(intento.usuarioId, intento.clave, gson.toJson(intento)))
    suspend fun quitar(intento: IntentoCompra) = dao.quitar(intento.usuarioId, intento.clave)
}
