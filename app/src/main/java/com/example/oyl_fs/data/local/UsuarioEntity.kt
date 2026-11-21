package com.example.oyl_fs.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Representa un cliente registrado
 * localmente en OYL Frutos Secos.
 *
 * Cada usuario tiene un identificador
 * único que posteriormente permitirá
 * separar sus pedidos, carrito y perfil.
 *
 * Esta tabla almacena solamente clientes.
 * El administrador seguirá utilizando
 * una cuenta demo preconfigurada.
 */
@Entity(
    tableName = "usuarios",
    indices = [
        Index(
            value = ["aliasNormalizado"],
            unique = true
        ),
        Index(
            value = ["correoNormalizado"],
            unique = true
        )
    ]
)
data class UsuarioEntity(

    // Identificador único del cliente.
    // Se generará mediante UUID.
    @PrimaryKey
    val id: String,

    // Alias visible elegido por el cliente.
    val alias: String,

    // Alias sin espacios exteriores y en
    // minúsculas para evitar duplicados.
    val aliasNormalizado: String,

    // Datos personales de demostración.
    val nombre: String,
    val apellido: String,
    val rut: String,
    val correo: String,

    // Permite impedir registros duplicados
    // por diferencias de mayúsculas.
    val correoNormalizado: String,

    val telefono: String,
    val direccion: String,

    // Nunca almacenaremos contraseñas
    // directamente en texto plano.
    //
    // En el paso de Registro generaremos
    // una sal aleatoria y un hash seguro.
    val claveHash: String,
    val claveSalt: String,

    // Fecha de creación en milisegundos.
    val creadoEn: Long
)
