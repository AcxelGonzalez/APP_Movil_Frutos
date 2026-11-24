# Primer bloque: sesión, carrito y perfil

Implementado sobre la auditoría del commit `76c9adf`, en la rama `desarrollo-marco`.

## Cambios

- **Sesión local:** guarda únicamente la identidad en preferencias privadas. Al abrir la app vuelve a consultar la cuenta en Room; las cuentas demo conservan sus roles definidos. La interfaz espera la restauración antes de mostrar pantallas de la cuenta. Cerrar sesión elimina la identidad persistida.
- **Carrito:** Room v4 incorpora una tabla identificada por cliente y producto. Agregar y cambiar cantidades se confirma en SQLite; stock e imágenes se observan desde el catálogo actual. No se cambia silenciosamente el precio aceptado al agregar.
- **Compra:** pedido, detalles, descuento de stock y retiro de los artículos comprados del carrito se realizan dentro de la misma transacción. Solo se modifica el carrito del dueño; si falla el stock se revierte la operación completa.
- **Perfil registrado:** editar actualiza la cuenta en Room y conserva contraseña, sal, alias e historial. Se comprueban correo y RUT duplicados. Las cuentas demo mantienen sus preferencias independientes.
- **Compatibilidad:** la migración 3→4 añade carrito y un marcador de migración del perfil sin borrar datos. Las preferencias antiguas de un cliente se incorporan a Room una sola vez si son válidas. Si hay errores o duplicados, no se sobrescribe la cuenta; las preferencias antiguas permanecen disponibles y la edición exige corregir los datos.
- **Validación:** registro y perfil comparten validación de RUT/teléfono; el teléfono rechaza letras. Perfil muestra errores junto a cada campo. Para cuentas antiguas y demo se admite RUT vacío; un RUT ingresado debe tener dígito válido.

Los carritos que solo existían en memoria en la versión anterior no pueden recuperarse después de cerrar aquel proceso: nunca habían sido escritos en almacenamiento. Los carritos nuevos sí persisten.

## Verificación

Con Java 17:

```powershell
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot'
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest --offline
```

Resultado:

- 40 pruebas unitarias correctas.
- 40 pruebas instrumentadas correctas en Samsung SM-A256E con Android 16.
- Compilación debug correcta.
- Android Lint: 0 errores y 34 advertencias. La advertencia adicional es el uso de resolución de recursos por nombre para restaurar imágenes del carrito; sigue el almacenamiento estable de nombres ya utilizado por el catálogo.
- `git diff --check` correcto.

Las nuevas pruebas verifican persistencia después de cerrar y reabrir Room, separación de carritos, agregados simultáneos, productos desactivados, rollback de compras, actualización de perfil sin cambiar credenciales, duplicados, migración única de preferencias y restauración/cierre de sesión.

La prueba de interfaz abre una sesión demo, recrea la Activity, cierra y vuelve a abrir la Activity, cierra sesión desde Perfil y verifica que una apertura posterior muestre el login. Restaura la preferencia de sesión previa del dispositivo al finalizar. No equivale a una prueba completa de muerte del proceso Android; la persistencia se comprueba también mediante instancias nuevas de repositorios y base de datos.

## Siguiente bloque

Conectar el escáner general a la navegación por rol y completar los datos de confirmación de pedidos. Siguen pendientes backend Spring Boot, Retrofit, API externa, notificaciones, captura fotográfica, medición de cobertura y firma release descritos en la auditoría.

El informe de auditoría conserva su carácter histórico: describe el estado previo del commit `76c9adf`. Este documento registra las correcciones posteriores.
