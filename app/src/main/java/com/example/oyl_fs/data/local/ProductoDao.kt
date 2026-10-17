package com.example.oyl_fs.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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
}
