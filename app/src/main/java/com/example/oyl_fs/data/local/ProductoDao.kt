package com.example.oyl_fs.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import androidx.room.Transaction

/**
 * Representa las unidades de un producto
 * que deben descontarse del inventario.
 */
data class DescuentoStock(
    val productoId: Int,
    val cantidad: Int,
    val precioUnitario: Int
)

/**
 * Indica que una operación de descuento
 * no cumple las condiciones del inventario.
 *
 * Lanzar esta excepción dentro de una
 * transacción permite revertir los cambios.
 */
class StockNoDisponibleException :
    IllegalStateException(
        "Stock insuficiente o producto modificado"
    )

/**
 * DAO: Data Access Object.
 *
 * Define las operaciones disponibles sobre
 * la tabla productos de Room.
 */
@Dao
interface ProductoDao {

    /**
     * Observa todos los productos.
     *
     * Flow emite nuevas listas cuando cambia
     * la información almacenada.
     */
    @Query("SELECT * FROM productos ORDER BY id ASC")
    fun observarProductos(): Flow<List<ProductoEntity>>

    /**
     * Recupera todos los productos almacenados
     * en SQLite mediante una consulta puntual.
     *
     * Se utilizará para inicializar el catálogo.
     */
    @Query("SELECT * FROM productos ORDER BY id ASC")
    suspend fun listarProductos(): List<ProductoEntity>

    /**
     * Consulta un producto mediante su ID.
     */
    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Int): ProductoEntity?

    /**
     * Agrega un producto nuevo.
     *
     * Devuelve el identificador insertado.
     * ABORT evita reemplazar datos existentes.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(producto: ProductoEntity): Long

    /**
     * Actualiza un producto existente.
     *
     * Devuelve el número de filas actualizadas.
     */
    @Update
    suspend fun actualizar(producto: ProductoEntity): Int

    /**
     * Elimina un producto.
     */
    @Delete
    suspend fun eliminar(producto: ProductoEntity): Int

    /**
     * Cuenta los productos almacenados.
     */
    @Query("SELECT COUNT(*) FROM productos")
    suspend fun contar(): Int

    /**
     * Descuenta stock solo cuando:
     * - El producto existe.
     * - Está disponible.
     * - El precio sigue siendo el esperado.
     * - Existe stock suficiente.
     *
     * Devuelve 1 si se actualizó una fila,
     * o 0 si no se cumplen las condiciones.
     */
    @Query(
        """
    UPDATE productos
    SET stock = stock - :cantidad
    WHERE id = :productoId
      AND disponible = 1
      AND precio = :precioUnitario
      AND stock >= :cantidad
      AND :cantidad > 0
    """
    )
    suspend fun descontarSiDisponible(
        productoId: Int,
        cantidad: Int,
        precioUnitario: Int
    ): Int

    /**
     * Descuenta varios productos dentro de
     * una única transacción de SQLite.
     *
     * Si falla uno de los descuentos, se lanza
     * una excepción y Room revierte todos los
     * descuentos anteriores de la transacción.
     */
    @Transaction
    suspend fun descontarStockLote(
        descuentos: List<DescuentoStock>
    ) {

        if (descuentos.isEmpty()) {
            throw StockNoDisponibleException()
        }

        for (descuento in descuentos) {

            if (
                descuento.productoId <= 0 ||
                descuento.cantidad <= 0 ||
                descuento.precioUnitario <= 0
            ) {
                throw StockNoDisponibleException()
            }

            val filasActualizadas = descontarSiDisponible(
                productoId = descuento.productoId,
                cantidad = descuento.cantidad,
                precioUnitario = descuento.precioUnitario
            )

            if (filasActualizadas != 1) {
                throw StockNoDisponibleException()
            }
        }
    }

}
