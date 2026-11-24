package com.example.oyl_fs.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.repository.MovimientoInventarioGuardado
import com.example.oyl_fs.ui.theme.OYL_FSTheme
import com.example.oyl_fs.viewmodel.MovimientoStockUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MovimientoInventarioScreenTest {
    @get:Rule val compose = createComposeRule()
    private val producto = ProductoInfo(10,"Almendras","M10","Frutos secos","","500 g",1000,10,2,true)
    private val pendiente = MovimientoInventarioGuardado("7","clave-prueba",producto,3,"EN_REVISION",null,null,"Resultado pendiente")

    @Test fun consultarNoContinuaYLosCamposDelPendienteEstanBloqueados() {
        var consultas = 0; var continuaciones = 0; var nuevos = 0
        compose.setContent { OYL_FSTheme { MovimientoInventarioScreen(producto,
            MovimientoStockUiState(10,pendiente = pendiente,resultadoIncierto = true),{}, { _,_ -> nuevos++ },{},
            onConsultar = { consultas++ }, onContinuar = { continuaciones++ }) } }
        compose.onNodeWithText("Cantidad de unidades").assertIsNotEnabled().assertTextContains("3")
        compose.onNodeWithText("Consultar resultado").performScrollTo().performClick()
        assertEquals(1,consultas); assertEquals(0,continuaciones); assertEquals(0,nuevos)
    }
    @Test fun continuarRequiereConfirmacionYNoCreaUnMovimientoNuevo() {
        var continuaciones = 0; var nuevos = 0
        compose.setContent { OYL_FSTheme { MovimientoInventarioScreen(producto,
            MovimientoStockUiState(10,pendiente = pendiente,resultadoIncierto = true),{}, { _,_ -> nuevos++ },{},
            onContinuar = { continuaciones++ }) } }
        compose.onNodeWithText("Continuar el mismo movimiento").performScrollTo().performClick()
        assertEquals(0,continuaciones)
        compose.onAllNodesWithText("Continuar el mismo movimiento").onLast().performClick()
        assertEquals(1,continuaciones); assertEquals(0,nuevos)
    }
    @Test fun comprobanteDistingueStockHistoricoYPermiteCerrarSinReenviar() {
        var cierres = 0; var continuaciones = 0
        val aplicado = pendiente.copy(estado = "APLICADA",stockAnterior = 10,stockResultante = 13)
        compose.setContent { OYL_FSTheme { MovimientoInventarioScreen(producto,
            MovimientoStockUiState(10,pendiente = aplicado,confirmado = producto.copy(stock = 13)),{}, { _,_ -> fail() },{},
            onContinuar = { continuaciones++ },onCerrarResultado = { cierres++ }) } }
        compose.onNodeWithText("Movimiento registrado").assertExists()
        compose.onNodeWithText("Stock después de este movimiento:",substring = true).assertExists()
        compose.onNodeWithText("Continuar el mismo movimiento").assertDoesNotExist()
        compose.onNodeWithText("Cerrar resultado").performScrollTo().performClick()
        assertEquals(1,cierres); assertEquals(0,continuaciones)
    }
}
