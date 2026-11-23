package com.example.oyl_fs.repository

import com.example.oyl_fs.data.local.OylDatabase
import com.example.oyl_fs.data.local.PuntosRetiroIniciales
import com.example.oyl_fs.data.local.aPuntoRetiro
import com.example.oyl_fs.model.PuntoRetiro
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class PuntoRetiroRepository(database: OylDatabase) {
    private val dao = database.puntoRetiroDao()

    fun observarPuntos(): Flow<List<PuntoRetiro>> = flow {
        dao.inicializarSiVacio(PuntosRetiroIniciales.entidades)
        emitAll(dao.observar().map { registros -> registros.map { it.aPuntoRetiro() } })
    }

    suspend fun obtenerPuntos(): List<PuntoRetiro> {
        dao.inicializarSiVacio(PuntosRetiroIniciales.entidades)
        return dao.listar().map { it.aPuntoRetiro() }
    }
}
