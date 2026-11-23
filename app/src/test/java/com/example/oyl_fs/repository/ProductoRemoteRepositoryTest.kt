package com.example.oyl_fs.repository

import android.content.Context
import android.content.res.Resources
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.network.ProductoDto
import com.example.oyl_fs.network.ProductosApi
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

/* Revisa que los productos que envíe ms-productos
se muestren bien en la app: que se vea la imagen correcta y no se
rompa si falta descripción.
 */
class ProductoRemoteRepositoryTest {

    private val api: ProductosApi = mockk()

    private fun repositorio(): ProductoRemoteRepository {
        val resources: Resources = mockk()
        val contexto: Context = mockk()
        every { contexto.applicationContext } returns contexto
        every { contexto.resources } returns resources
        every { contexto.packageName } returns "com.example.oyl_fs"
        every { resources.getIdentifier("almendras", "drawable", any()) } returns 123
        return ProductoRemoteRepository(contexto, api)
    }

    private fun dto(imagenUrl: String?) = ProductoDto(
        1, "Almendras", "OYL001", "Frutos secos", null,
        "Bolsa de 500 g", 6990, 60, 2, true, imagenUrl
    )

    @Test
    fun unaImagenDrawableSeTraduceAlIdDelRecurso() = runBlocking {
        coEvery { api.listar() } returns listOf(dto("drawable:almendras"))

        val producto = repositorio().listar().single()

        assertEquals(123, producto.imagen)
        assertNull(producto.imagenUri)
        assertEquals("OYL001", producto.codigo)
    }

    @Test
    fun unaFotoSubidaPorElAdminSeConservaComoUri() = runBlocking {
        coEvery { api.listar() } returns listOf(dto("content://fotos/almendras.jpg"))

        val producto = repositorio().listar().single()

        assertEquals(0, producto.imagen)
        assertEquals("content://fotos/almendras.jpg", producto.imagenUri)
    }

    @Test
    fun unaDescripcionNulaLlegaComoTextoVacio() = runBlocking {
        coEvery { api.listar() } returns listOf(dto(null))

        assertEquals("", repositorio().listar().single().descripcion)
    }

    @Test
    fun alCrearUnProductoNoSeEnviaElIdPorqueLoAsignaElBackend() = runBlocking {
        val enviado = slot<ProductoDto>()
        coEvery { api.crear(capture(enviado)) } returns dto(null)

        repositorio().crear(
            ProductoInfo(
                0, "Pistachos", "OYL006", "Frutos secos", "Tostados",
                "Bolsa de 250 g", 8990, 20, 2, true
            )
        )

        assertNull(enviado.captured.id)
        assertEquals("OYL006", enviado.captured.codigo)
    }

    @Test
    fun eliminarDevuelveFalsoSiElProductoYaNoExistia() = runBlocking {
        val respuesta: Response<Unit> = mockk()
        every { respuesta.isSuccessful } returns false
        coEvery { api.eliminar(9L) } returns respuesta

        assertFalse(repositorio().eliminar(9))
    }
}