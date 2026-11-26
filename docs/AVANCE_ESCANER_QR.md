# Escáner de productos y navegación por rol

Base del cambio: `7033d65`, rama `desarrollo-marco`.

## Comportamiento implementado

1. Ambos inicios ofrecen el botón **Escanear producto**.
2. La lectura utiliza el formato QR de OYL existente: `{"tipo":"producto","id":1}`. El código identifica el producto; precio y stock se consultan en el catálogo actual.
3. El cliente puede abrir la ficha y continuar con el carrito. Un producto inactivo se informa y no permite abrir la ficha de venta; uno activo sin stock puede consultarse, pero su detalle impide agregar unidades inexistentes.
4. El administrador puede abrir directamente la edición de inventario del producto, incluso si está inactivo. Al guardar vuelve al detalle y desde allí puede regresar al escáner.
5. Una lectura inválida, un producto eliminado o una cancelación no conservan la acción del resultado anterior. Cambios de stock/disponibilidad se reflejan en el resultado visible.
6. Mientras se abre la cámara se bloquean solicitudes repetidas. Las respuestas que llegan después de abandonar la pantalla se ignoran.
7. La lectura correcta presenta una confirmación visual animada.

La selección inicial del inventario y la comprobación de productos eliminados se coordinan en un único efecto para que una lectura válida no se descarte durante la apertura del formulario.

## Datos y compatibilidad

Este bloque utiliza el esquema Room v4 del bloque anterior; no añade migraciones ni borra registros. La aplicación usa el catálogo persistente y las operaciones de edición existentes. La lectura con cámara sigue delegada al escáner de Google, también utilizado en la preparación de pedidos.

La dependencia del lanzador de cámara se puede sustituir en las pruebas. Esta sustitución se utiliza únicamente al construir las pantallas de prueba; el flujo normal abre el escáner de Google.

## Verificación reproducible

Con Java 17 y un dispositivo Android conectado:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest --offline
```

Resultado: compilación correcta, 40/40 pruebas unitarias y 45/45 instrumentadas
correctas en Samsung SM-A256E con Android 16. Ninguna omitida. Android Lint
terminó sin errores y con 34 advertencias, la misma cantidad del bloque anterior.
`git diff --check` también terminó correctamente.

Las cinco pruebas nuevas de interfaz cubren:

- Inicio del cliente → escáner → ficha → retorno.
- Inicio del administrador → QR de producto inactivo → edición de stock → guardado y retorno, sin modificar el stock de otro producto.
- Cancelación, código inválido y producto desconocido después de una lectura válida.
- Actualización del resultado al cambiar stock, desactivar y eliminar el producto.
- Solicitud pendiente, bloqueo de doble lectura y respuesta tardía al abandonar la pantalla.

Las pruebas de navegación usan lecturas simuladas y un catálogo de prueba en memoria, para no cambiar el inventario del dispositivo. No acreditan una lectura óptica con cámara física ni la disponibilidad del módulo de Google Play Services; esa demostración manual sigue pendiente.

## Siguiente paso

Completar la confirmación del pedido con modalidad de entrega/retiro y datos de contacto, persistirlos en Room y mostrarlos al cliente y administrador. Posteriormente se incorporará su envío al backend Spring Boot mediante Retrofit.
