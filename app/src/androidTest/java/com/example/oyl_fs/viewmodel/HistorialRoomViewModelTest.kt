package com.example.oyl_fs.viewmodel

/*
Prueba desactivada.

Se escribió cuando el historial leía los pedidos desde Room. Tras la
migración a microservicios, HistorialViewModel obtiene los pedidos de
ms-pedidos y ya no usa PedidoRoomRepository, por lo que esta prueba
verifica un comportamiento que dejó de existir.

Se conserva el archivo como referencia. Para probar el historial actual
hay que simular PedidoRemoteRepository, no la base de datos local.
 */
class HistorialRoomViewModelTest