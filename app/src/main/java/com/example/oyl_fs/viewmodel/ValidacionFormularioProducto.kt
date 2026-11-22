package com.example.oyl_fs.viewmodel

data class DatosFormularioProducto(
    val nombre: String,
    val codigo: String,
    val categoria: String,
    val precio: String,
    val stock: String,
    val stockMinimo: String,
    val imagenPresente: Boolean,
    val presentacion: String,
    val descripcion: String = ""
)

data class ErroresFormularioProducto(
    val nombre: String? = null,
    val codigo: String? = null,
    val categoria: String? = null,
    val precio: String? = null,
    val stock: String? = null,
    val stockMinimo: String? = null,
    val imagen: String? = null,
    val presentacion: String? = null,
    val descripcion: String? = null
) {
    fun primerError(): String? = listOfNotNull(
        nombre, codigo, categoria,
        descripcion, precio, stock, stockMinimo, presentacion, imagen
    ).firstOrNull()

    val hayErrores: Boolean
        get() = primerError() != null
}

object ValidacionFormularioProducto {

    private val categoriasPermitidas = setOf(
        "Frutos secos",
        "Semillas",
        "Deshidratados"
    )

    fun validar(
        datos: DatosFormularioProducto,
        requerirImagen: Boolean = false
    ): ErroresFormularioProducto {

        val precio = datos.precio.toIntOrNull()
        val stock = datos.stock.toIntOrNull()
        val stockMinimo = datos.stockMinimo.toIntOrNull()

        return ErroresFormularioProducto(
            nombre = when {
                datos.nombre.isBlank() -> "Escribe el nombre del producto."
                datos.nombre.trim().length > 100 -> "El nombre admite hasta 100 caracteres."
                else -> null
            },

            codigo = when {
                datos.codigo.isBlank() -> "Escribe el código del producto."
                datos.codigo.trim().length > 30 -> "El código admite hasta 30 caracteres."
                else -> null
            },

            presentacion = if (datos.presentacion.isBlank()) {
                "Escribe el peso o la presentación."
            } else null,

            descripcion = if (datos.descripcion.length > 500) {
                "La descripción admite hasta 500 caracteres."
            } else null,

            categoria = if (
                datos.categoria !in categoriasPermitidas
            ) {
                "Selecciona una categoría válida."
            } else null,

            precio = if (precio == null || precio <= 0) {
                "Ingresa un precio mayor que cero."
            } else null,

            stock = if (stock == null || stock < 0) {
                "Ingresa un stock válido."
            } else null,

            stockMinimo = if (
                stockMinimo == null || stockMinimo < 0
            ) {
                "Ingresa un stock mínimo válido."
            } else null,

            imagen = if (
                requerirImagen && !datos.imagenPresente
            ) {
                "Selecciona una fotografía."
            } else null
        )
    }
}
