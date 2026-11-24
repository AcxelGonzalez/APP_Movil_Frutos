package com.example.oyl_fs.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class MovimientoStockMigrationTest {
    @Test fun migracionCuatroACincoConservaPerfilAvatarCompraYCancelacion() = runBlocking {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val nombre = "migracion-inventario-${UUID.randomUUID()}.db"
        contexto.openOrCreateDatabase(nombre, Context.MODE_PRIVATE, null).use { anterior ->
            anterior.execSQL("CREATE TABLE perfiles_remotos (usuarioId INTEGER NOT NULL PRIMARY KEY, esAdmin INTEGER NOT NULL, nombre TEXT NOT NULL, correo TEXT NOT NULL, telefono TEXT NOT NULL, direccion TEXT NOT NULL, rut TEXT NOT NULL, apellido TEXT NOT NULL)")
            anterior.execSQL("CREATE TABLE avatares_perfil (perfilClave TEXT NOT NULL PRIMARY KEY, archivo TEXT NOT NULL)")
            anterior.execSQL("CREATE TABLE intentos_compra (usuarioId TEXT NOT NULL PRIMARY KEY, clave TEXT NOT NULL, contenido TEXT NOT NULL)")
            anterior.execSQL("CREATE TABLE cancelaciones_locales (actorId TEXT NOT NULL, pedidoId INTEGER NOT NULL, propietarioId TEXT NOT NULL, contenido TEXT NOT NULL, estado TEXT NOT NULL, mensaje TEXT, PRIMARY KEY(actorId,pedidoId))")
            anterior.execSQL("INSERT INTO perfiles_remotos VALUES (7,0,'Cliente','cliente@example.test','912345678','','12.345.678-5','Apellido')")
            anterior.execSQL("INSERT INTO avatares_perfil VALUES ('remoto:7','avatar-prueba.jpg')")
            anterior.execSQL("INSERT INTO intentos_compra VALUES ('7','clave-prueba','{}')")
            anterior.execSQL("INSERT INTO cancelaciones_locales VALUES ('7',55,'7','{}','EN_REVISION',NULL)")
            anterior.version = 4
        }
        val base = Room.databaseBuilder(contexto, PerfilRemotoDatabase::class.java, nombre).addMigrations(MIGRATION_PERFIL_4_5).build()
        try {
            assertEquals("12.345.678-5", base.perfilDao().obtener(7)?.rut)
            assertEquals("Apellido", base.perfilDao().obtener(7)?.apellido)
            assertEquals("avatar-prueba.jpg", base.avatarDao().obtener("remoto:7"))
            assertEquals("EN_REVISION", base.cancelacionDao().obtener("7",55)?.estado)
            base.openHelper.readableDatabase.query("SELECT clave FROM intentos_compra WHERE usuarioId='7'").use {
                assertTrue(it.moveToFirst()); assertEquals("clave-prueba",it.getString(0))
            }
            assertNull(base.movimientoStockDao().obtener("7"))
            base.movimientoStockDao().insertar(MovimientoStockLocalEntity("7",UUID.randomUUID().toString(),10,3,"{}","EN_REVISION",null,null,null))
            assertEquals(3,base.movimientoStockDao().obtener("7")?.cambio)
        } finally { base.close(); contexto.deleteDatabase(nombre) }
    }
}
