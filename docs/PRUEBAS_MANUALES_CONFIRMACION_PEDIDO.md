# Pruebas funcionales de confirmación de pedidos

Paso de desarrollo: confirmación con entrega o retiro, contacto, comprobante e historial. Fecha: 19 de noviembre de 2026.

Esta guía permite comprobar el comportamiento visible en el teléfono. Los pedidos, contactos y direcciones son ficticios. La app todavía funciona con Room local: no realiza cobros, envíos reales ni sincronización entre teléfonos.

## Preparación

1. En Android Studio abre este proyecto y comprueba que estés en `desarrollo-marco`.
2. Usa Java 17 para Gradle y ejecuta `app` en el teléfono o emulador.
3. Actualiza la instalación existente con Run. **No desinstales ni borres los datos** si quieres comprobar que se conservan los pedidos anteriores.
4. Entra como `admin` / `admin`, abre Inventario y anota el stock y precio del producto que usarás. Elige uno activo con al menos dos unidades. Para los ejemplos se usa Almendras, pero el stock y precio pueden haber cambiado en pruebas previas.
5. Cierra sesión y entra como `cliente` / `cliente`, o usa una cuenta registrada de prueba.
6. Agrega dos unidades del producto al carrito. Revisa que el total corresponda al precio actual multiplicado por dos.

Anota el stock inicial **S**, precio **P**, hora de la prueba y número de solicitud cuando aparezca. Si repites un caso, vuelve a anotar el stock de partida: las compras de prueba sí descuentan inventario local.

## Caso 1 Formulario y errores obligatorios

1. Abre el carrito y toca **Confirmar pedido**.
2. Verifica que aparezcan los artículos, cantidades, precios unitarios y el total de productos.
3. Mantén la modalidad **Retiro en tienda o punto de retiro**.
4. Borra Nombre de contacto y Teléfono de contacto; no selecciones un punto.
5. Baja hasta **Finalizar pedido** y tócalo.
6. Revisa los errores junto al nombre, teléfono y selector de retiro. Desplázate hacia arriba si es necesario.
7. Escribe un nombre ficticio y prueba un teléfono con letras, por ejemplo `abc12345678`. Vuelve a finalizar.

**Resultado esperado:** no se registra ningún pedido; los errores indican qué corregir. El teléfono con letras se rechaza. Al volver al carrito siguen las dos unidades. El stock no cambia.

## Caso 2 Pedido con retiro

1. Desde el carrito, abre Confirmar pedido.
2. Ingresa `Cliente de prueba` y `912345678` como contacto ficticio.
3. Selecciona **OYL Frutos Secos - Tienda principal**. Comprueba su dirección y horario de referencia.
4. Revisa el total y toca **Finalizar pedido**.
5. Anota el número de solicitud mostrado.
6. Revisa **Pedido registrado**, estado **Pendiente de Validación**, productos, total, contacto y punto de retiro.
7. Toca **Ver mis pedidos**, ubica ese número y abre **Ver detalle**.
8. Vuelve al inicio y abre el carrito.

**Resultado esperado:** hay una sola solicitud por el total `2 × P`; el historial conserva el mismo contacto y retiro; el carrito queda vacío. El stock del producto baja de `S` a `S − 2`. La solicitud no se presenta como pagada.

## Caso 3 Datos visibles para el administrador

1. Después del caso 2, cierra sesión desde Perfil.
2. Entra como `admin` / `admin`.
3. Abre Pedidos y busca el número que anotaste.
4. Toca Ver detalle y desplázate hasta revisar todos los campos.
5. Comprueba también el stock del producto en Inventario.

**Resultado esperado:** el administrador ve los mismos artículos, total, contacto y lugar de retiro que el cliente. El stock corresponde al descuento de la compra. Si el pedido es largo, el detalle permite desplazarse y cerrar la ventana.

## Caso 4 Entrega a domicilio y cambio de modalidad

1. Entra como cliente y agrega un producto al carrito.
2. Abre Confirmar pedido y completa nombre y teléfono ficticios.
3. Selecciona primero un punto de retiro.
4. Cambia a **Entrega a domicilio** y deja vacía la dirección.
5. Toca Finalizar pedido: debe aparecer el error de dirección y no crearse la solicitud.
6. Escribe `Calle de Prueba 123, Santiago (ficticia)`.
7. Finaliza y revisa el comprobante y después el detalle del historial.

**Resultado esperado:** la solicitud guarda la dirección escrita y modalidad domicilio. No conserva el punto de retiro seleccionado antes del cambio. El total corresponde a los productos; se informa que la entrega es de demostración y que no se calculan tarifas ni pagos.

## Caso 5 Cancelación y botón Atrás

1. Con artículos en el carrito, abre Confirmar pedido.
2. Completa algunos campos, pero no finalices.
3. Toca **Volver al carrito**.
4. Repite el ingreso y prueba el botón o gesto Atrás de Android.

**Resultado esperado:** ambas acciones regresan al carrito sin crear pedido ni descontar stock. El carrito mantiene sus artículos. Volver a abrir el formulario es una nueva revisión de la compra; no se exige conservar un borrador después de cancelarlo.

## Caso 6 Rotación y pulsaciones repetidas

1. Abre Confirmar pedido, elige domicilio y completa todos los campos.
2. Gira el teléfono. Si la app no gira, activa rotación automática.
3. Revisa que sigan el contacto, modalidad y dirección.
4. Toca Finalizar pedido dos veces rápidamente.
5. Comprueba el número de solicitud, el historial y el stock.
6. Si alcanzas a ver **Registrando pedido…**, comprueba que no permita finalizar nuevamente ni regresar mientras se guarda.

**Resultado esperado:** la rotación conserva los datos del formulario. Se registra una sola compra y se descuenta una sola vez. La operación es local y puede terminar demasiado rápido para observar el estado de guardado; confirma la unicidad por el historial y el stock.

## Caso 7 Reinicio y perfil posterior

1. Registra un pedido y anota contacto, dirección/retiro e identificador.
2. Abre los ajustes de Android de esta app y usa **Forzar detención**. No uses Borrar datos.
3. Reabre la app, entra a Mis pedidos y abre el mismo identificador.
4. Edita el nombre o teléfono de tu perfil con otros datos ficticios.
5. Vuelve a abrir el pedido anterior.

**Resultado esperado:** el pedido conserva sus datos después del reinicio. Editar el perfil no modifica el contacto ni lugar acordados para esa solicitud.

## Caso 8 Stock modificado antes de confirmar

1. Como cliente, deja dos unidades en el carrito y vuelve al inicio sin comprar.
2. Cierra sesión, entra como administrador y anota el stock actual.
3. Cambia el stock del producto a una unidad.
4. Vuelve a entrar como el mismo cliente y abre el carrito.
5. Intenta confirmar.

**Resultado esperado:** se informa falta de stock y no se crea el pedido. El carrito conserva la selección para corregirla. El inventario nunca queda negativo. Este caso se hace en el mismo teléfono, porque todavía no hay sincronización con servidor. Al terminar puedes devolver el stock al valor anotado antes de este caso.

## Caso 9 Precio modificado antes de confirmar

1. Como cliente, agrega el producto y anota el total del carrito.
2. Sin comprar, cierra sesión; como administrador cambia el precio y anota ambos valores.
3. Vuelve al mismo cliente, revisa el formulario e intenta finalizar con datos válidos.
4. Vuelve al carrito, quita el artículo y agrégalo nuevamente desde el catálogo.
5. Confirma otra vez revisando el precio actualizado.

**Resultado esperado:** el intento con precio antiguo no genera pedido ni descuenta stock. Aparece la indicación de revisar el precio y quitar/agregar el producto. El nuevo intento utiliza el precio vigente. Nunca se acepta silenciosamente un total distinto al revisado.

## Caso 10 Pedidos anteriores y cuentas diferentes

1. Abre como administrador un pedido demo o anterior a esta actualización.
2. Revisa el detalle.
3. Si tienes dos cuentas registradas de prueba, registra un pedido con A; cierra sesión y entra con B.

**Resultado esperado:** los pedidos antiguos siguen presentes y muestran **Pedido anterior sin datos de entrega registrados**. No se inventan contactos o direcciones. El cliente B no ve el pedido de A; el administrador sí puede consultarlo.

## Registro de resultados

| Caso | Correcto o fallo | Número de pedido | Observación |
|---|---|---|---|
| 1 Validaciones | | | |
| 2 Retiro | | | |
| 3 Administrador | | | |
| 4 Domicilio | | | |
| 5 Cancelación | | | |
| 6 Rotación y duplicados | | | |
| 7 Reinicio y perfil | | | |
| 8 Stock | | | |
| 9 Precio | | | |
| 10 Históricos y cuentas | | | |

Si aparece un fallo, registra el paso exacto, mensaje visible, cuenta de prueba, identificador y si el stock/carrito cambiaron. Eso permite reproducirlo sin borrar datos ni repetir compras a ciegas.

## Verificación automática de este paso

Con Java 17, Samsung SM-A256E conectado y Android 16:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest --offline
```

Resultado: 44 pruebas unitarias y 53 instrumentadas correctas, sin omisiones.
Compilación correcta; Lint terminó con cero errores y 34 advertencias existentes.
El teléfono debe estar desbloqueado y con la pantalla activa durante las pruebas
de interfaz. Esta ejecución automatizada no marca como realizados los casos
manuales de la tabla anterior.

La base pasa a Room v5 mediante la migración 4→5: añade seis campos opcionales
de entrega a los pedidos y conserva los registros anteriores. Los contactos
guardados pertenecen a cada pedido y no se actualizan al editar el perfil.
