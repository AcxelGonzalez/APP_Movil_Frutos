package com.example.oyl_fs.screens

import com.example.oyl_fs.R

// Información de cada producto.
data class ProductoInfo(
    val id: Int,
    val nombre: String,
    val codigo: String,
    val categoria: String,
    val descripcion: String,
    val presentacion: String,
    val precio: Int,
    val stock: Int,
    val stockMinimo: Int,
    val disponible: Boolean,
    val imagen: Int
)

// Datos temporales
object ProductosDatos {

    val almendras = ProductoInfo(
        id = 1,
        nombre = "Almendras",
        codigo = "OYL001",
        categoria = "Frutos secos",
        descripcion = "Semillas comestibles del almendro, originarias de Asia. Son crujientes, de sabor suave y ricas en proteínas, fibra y grasas saludables.",
        presentacion = "Bolsa de 500 g",
        precio = 6900,
        stock = 60,
        stockMinimo = 2,
        disponible = true,
        imagen = R.drawable.almendras
    )

    val nueces = ProductoInfo(
        id = 2,
        nombre = "Nueces",
        codigo = "OYL002",
        categoria = "Frutos secos",
        descripcion = "Son frutos del nogal, originarios de Asia. Se caracterizan por su textura crujiente y son ricas en proteínas, fibra y grasas saludables.",
        presentacion = "Bolsa de 500 g",
        precio = 5990,
        stock = 60,
        stockMinimo = 2,
        disponible = true,
        imagen = R.drawable.nueces
    )

    val pasas = ProductoInfo(
        id = 3,
        nombre = "Pasas",
        codigo = "OYL003",
        categoria = "Deshidratados",
        descripcion = "Son uvas deshidratadas, originarias de Asia. Se caracterizan por su sabor dulce y textura suave. Son ricas en fibra, antioxidantes y minerales.",
        presentacion = "Bolsa de 500 g",
        precio = 6000,
        stock = 30,
        stockMinimo = 2,
        disponible = true,
        imagen = R.drawable.pasas
    )

    val mix = ProductoInfo(
        id = 4,
        nombre = "Mix de frutos secos",
        codigo = "OYL004",
        categoria = "Frutos secos",
        descripcion = "Mezcla de frutos secos que combina nueces y almendras. Tiene una textura crujiente y aporta proteínas, fibra y grasas saludables.",
        presentacion = "Bolsa de 1 kg",
        precio = 16000,
        stock = 30,
        stockMinimo = 2,
        disponible = true,
        imagen = R.drawable.mix
    )

    val semillasGirasol = ProductoInfo(
        id = 5,
        nombre = "Semillas de girasol",
        codigo = "OYL005",
        categoria = "Semillas",
        descripcion = "Son semillas comestibles provenientes de la flor del girasol. Tienen un sabor suave, una textura crujiente y son ricas en proteínas, fibra y grasas saludables.",
        presentacion = "Bolsa de 500 g",
        precio = 7990,
        stock = 60,
        stockMinimo = 2,
        disponible = true,
        imagen = R.drawable.semillas_girasol
    )

    // Productos que aparecerán en el catálogo.
    val productos = listOf(
        almendras,
        nueces,
        pasas,
        mix,
        semillasGirasol
    )
}