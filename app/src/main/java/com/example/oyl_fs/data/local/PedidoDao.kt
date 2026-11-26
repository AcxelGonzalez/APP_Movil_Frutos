package com.example.oyl_fs.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

import kotlinx.coroutines.flow.Flow

/**
 * DAO encargado de las operaciones
 * de almacenamiento de pedidos.
 *
 * Los pedidos y sus detalles se
 * almacenarán en SQLite mediante Room.
 */
@Dao
interface PedidoDao {

    /**
     * Observa todos los pedidos.
     *
     * Se utilizará principalmente
     * para la vista del administrador.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM pedidos
        ORDER BY fecha DESC, id DESC
        """
    )
    fun observarPedidos():
            Flow<List<PedidoConDetalles>>

    /**
     * Observa únicamente los pedidos
     * pertenecientes a un cliente.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM pedidos
        WHERE usuarioId = :usuarioId
        ORDER BY fecha DESC, id DESC
        """
    )
    fun observarPedidosUsuario(
        usuarioId: String
    ): Flow<List<PedidoConDetalles>>

    /**
     * Busca un pedido específico junto
     * con todos sus detalles.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM pedidos
        WHERE id = :pedidoId
        LIMIT 1
        """
    )
    suspend fun obtenerPedidoPorId(
        pedidoId: String
    ): PedidoConDetalles?

    /**
     * Inserta un pedido.
     *
     * ABORT impide reemplazar un pedido
     * existente con el mismo ID.
     */
    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertarPedido(
        pedido: PedidoEntity
    )

    /**
     * Inserta los productos incluidos
     * en un pedido.
     *
     * Cada detalle contiene su pedidoId.
     */
    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertarDetalles(
        detalles: List<DetallePedidoEntity>
    )


    /**
     * Actualiza el estado únicamente si el
     * pedido conserva el estado anterior esperado.
     *
     * Esto evita saltos de estado y cambios
     * simultáneos inconsistentes.
     *
     * Devuelve:
     * 1 -> actualización realizada.
     * 0 -> pedido inexistente o estado incorrecto.
     */
    @Query(
        """
    UPDATE pedidos
    SET estado = :nuevoEstado
    WHERE id = :pedidoId
      AND estado = :estadoAnterior
    """
    )
    suspend fun actualizarEstado(
        pedidoId: String,
        estadoAnterior: String,
        nuevoEstado: String
    ): Int

    /**
     * Permite comprobar cuántos pedidos
     * existen en SQLite.
     */
    @Query("SELECT COUNT(*) FROM pedidos")
    suspend fun contarPedidos(): Int
}
