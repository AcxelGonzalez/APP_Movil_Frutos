# Auditoría de OYL Frutos Secos: evaluaciones parciales 2 y 4

Fecha: 17 de noviembre de 2026, zona America/Santiago.
Base revisada: rama `desarrollo-marco`, commit `76c9adf`.

## 1. Dictamen y alcance

La aplicación tiene una base local sólida para continuar: compila con Java 17, pasan sus 37 pruebas unitarias y sus 33 pruebas instrumentadas, y Android Lint no encuentra errores. Ya existen catálogo, CRUD de productos, inventario, registro, autenticación local, carrito por cliente en memoria, pedidos persistentes y preparación de pedidos mediante QR.

No está completo el alcance de las pantallas del caso. Las principales correcciones heredadas son conservar la sesión, persistir el carrito, unificar los perfiles con Room, completar los errores por campo del perfil y conectar el escáner general a la navegación. Faltan también funcionalidades definidas en el caso, especialmente notificaciones, captura fotográfica y confirmación con datos de entrega/retiro.

Para la evaluación 4, la mayor parte de la integración sigue pendiente: no hay backend Spring Boot en esta carpeta, cliente Retrofit, conexión CRUD a servicios propios, API externa mediante Retrofit, medición del 80% de cobertura ni configuración de firma release. Las pruebas existentes constituyen un avance real que se puede aprovechar.

Esta auditoría analiza el estado actual, no reconstruye exactamente la entrega histórica de la evaluación 2 ni estima su calificación. Tampoco concluye que un backend no exista en otro repositorio: no está presente ni integrado en el proyecto revisado.

Se inspeccionaron código Kotlin, Gradle, manifiesto, recursos relevantes, entidades, DAO, migraciones, repositorios, ViewModels, pantallas y pruebas; también el historial y las referencias Git locales y los cuatro PDF facilitados. No se realizó una revisión visual manual de todas las pantallas ni una demostración física de lectura QR, captura de imagen o mapas. Las pruebas instrumentadas verifican lógica y almacenamiento; no son pruebas automatizadas completas de la interfaz.

## 2. Documentos y autorización

Se utilizaron estos documentos como criterios del producto y de evaluación:

- `036 SEC004D Caso OYL Frutos Secos.pdf`: contexto, entidades, transacciones, Room como caché, Retrofit, Spring Boot y notificaciones; especialmente páginas 2 y 3.
- `057 Control 2 Grupal Caso Semestral.pdf`: corresponde a la **Evaluación Parcial 2**; requisitos y entrega en página 3, rúbrica en páginas 4 a 6.
- `059 SEC004D Pantallas Caso OYL Frutos Secos.pdf`: propuesta de 15 pantallas, separación por usuario y alcance tecnológico; páginas 1 a 5.
- `Ev. Parcial 4_Encargo_Estudiante_ DSY1105.pdf`: requisitos y entrega en páginas 2 a 4; rúbrica en páginas 5 y 6.

Los PDF describen el encargo; no son instrucciones para ejecutar acciones sobre el equipo o publicar cambios. La solicitud del usuario autoriza continuar después de la revisión de la evaluación 2 y permite commits y push. Por tanto, la restricción histórica de cambios de EP2 no se usa para bloquear esta auditoría. EP4 sí establece una restricción propia después de su fecha límite; los PDF no contienen la fecha calendario concreta de esa entrega.

No se modificó el código funcional ni se hicieron commits o push. Se generó este informe y se ejecutaron las comprobaciones necesarias. La autorización permite futuras correcciones sin volver a preguntar por la restricción histórica de EP2.

## 3. Verificación ejecutada

| Comprobación | Resultado | Alcance real |
|---|---|---|
| Compilación `assembleDebug` | Correcta | Se generó APK debug; no acredita firma release |
| `testDebugUnitTest` | 37/37 correctas, 0 omitidas | 9 clases; incluye una prueba de plantilla |
| `connectedDebugAndroidTest` | 33/33 correctas, 0 omitidas | Samsung SM-A256E, Android 16; incluye una prueba de contexto de plantilla |
| `lintDebug` | 0 errores y 33 advertencias | Análisis estático Android |
| Cobertura | No medida | No hay configuración JaCoCo/Kover ni reporte que acredite 80% |
| Navegación y aspecto en dispositivos diversos | Pendiente | No certificados por compilar o pasar las pruebas actuales |

Comandos utilizados, con JDK 17 seleccionado en el proceso:

```powershell
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot'
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --rerun-tasks
.\gradlew.bat connectedDebugAndroidTest --offline
```

La primera ejecución usó el `JAVA_HOME` del entorno, que apunta a Java 25.0.2, y falló. La compilación con Java 17 fue correcta. El fallo inicial no demuestra un defecto funcional de la app: revela que falta documentar y estabilizar el entorno de desarrollo. Lint necesitó descargar `lint-gradle:31.2.2`, que no estaba en caché; posteriormente terminó correctamente.

Evidencias generadas en `app/build/`:

- Pruebas unitarias: `test-results/testDebugUnitTest/TEST-*.xml` y `reports/tests/testDebugUnitTest/index.html`.
- Pruebas instrumentadas: `outputs/androidTest-results/connected/` y `reports/androidTests/connected/`.
- Lint: `reports/lint-results-debug.html` y `reports/lint-results-debug.xml`.
- APK debug: `outputs/apk/debug/app-debug.apk`.

### Qué está bien resuelto

- Room v3 contiene productos, usuarios, pedidos y detalles; existen migraciones 1→2 y 2→3 sin recreación destructiva configurada.
- La compra guarda pedido y detalles y descuenta stock dentro de una misma transacción (`PedidoRoomRepository.registrarCompraConStock`). No debe reemplazarse por dos escrituras independientes.
- El descuento SQL exige disponibilidad, precio esperado, cantidad positiva y stock suficiente. Un fallo revierte la compra completa (`ProductoDao.descontarStockLote`).
- Los pedidos utilizan UUID y conservan nombre y precio históricos de los artículos.
- Los cambios de estado verifican el estado previo, y la interfaz restringe las transiciones normales.
- El registro normaliza alias y correo, impide duplicados y comprueba RUT repetidos mediante el repositorio. Las contraseñas registradas usan PBKDF2 con sal aleatoria y comparación de tiempo constante.
- Las cuentas académicas `admin/admin` y `cliente/cliente` están identificadas como demostración. La cuenta administrativa preconfigurada está admitida expresamente en el documento de pantallas.
- El historial visible se filtra por usuario y rol; el carrito en memoria usa un identificador diferente para cada cliente.
- Hay validadores separados de Compose para registro y productos, errores individuales y animaciones funcionales.
- La selección de fotos usa `OpenDocument` y conserva permiso de lectura de la URI. No necesita acceso general a todo el almacenamiento.
- La lectura QR para preparación comprueba pertenencia al pedido y número de unidades; evita preparar pedidos aún no confirmados.

## 4. Matriz de la evaluación parcial 2

Los estados siguientes describen evidencia técnica, no puntos ni notas asignadas por el docente.

| Indicador | Peso | Estado actual | Brecha para fortalecerlo |
|---|---:|---|---|
| IE 2.1.1: interfaz, jerarquía y navegación | 15% | Parcial, con numerosos flujos implementados | Escáner general sin acceso; bienvenida y notificaciones ausentes; revisar rotación y navegación real |
| IE 2.1.2: formularios, errores por campo e íconos | 15% | Parcial | Perfil muestra error general; faltan validaciones coherentes de RUT/teléfono; revisar campos descriptivos de productos |
| IE 2.2.1: lógica desacoplada y estado | 10% | Mayormente implementado, con deuda | Validadores de registro/producto existen; compra y sesión están coordinadas en Activity; reglas de perfil no comparten el validador de registro |
| IE 2.2.2: animaciones útiles | 10% | Evidencia clara en código | Hay entradas, avisos de agregado, carrito y errores animados; falta validación visual de fluidez y éxito QR/compra |
| IE 2.3.1: arquitectura y persistencia | 15% | Parcial | Productos/pedidos/usuarios en Room; carrito volátil, sesión volátil y perfil en preferencias; dependencia de datos de UI desde Room |
| IE 2.3.2: colaboración GitHub/Trello | 20% | Evidencia Git local; entrega incompleta en esta rama | Falta README; Trello no verificable con los archivos disponibles; no se verificaron permisos públicos ni acceso docente |
| IE 2.4.1: al menos dos recursos del dispositivo | 15% | Hay implementaciones aprovechables; demostración pendiente | Escáner Google en preparación y selector de documentos son recursos distintos; mapa no equivale a GPS y QR no equivale a una segunda cámara |

No hace falta rediseñar toda la app ni crear múltiples módulos Gradle únicamente por la palabra «modularidad». La separación de responsabilidades puede mejorarse dentro del módulo existente. Tampoco corresponde exigir autenticación productiva para invalidar una cuenta demo expresamente permitida.

## 5. Hallazgos y correcciones recomendadas

Prioridad **alta**: afecta requisitos o datos fundamentales. **Media**: funcionalidad incompleta o mantenibilidad relevante. **Baja**: mejora secundaria. «Confirmado por código» no significa que se haya reproducido manualmente cada escenario en pantalla.

### H01 — Alta: la sesión se pierde al recrear la Activity

**Evidencia:** `MainActivity.kt:219`, `:223` y `:231`: sesión, rol e identificador se guardan con `remember`; no hay repositorio persistente de sesión. El comentario menciona muerte del proceso, pero `remember` también se pierde cuando Android recrea la Activity por un cambio de configuración, por ejemplo una rotación.

**Efecto:** el usuario vuelve al login aunque sus ViewModels o algunos estados `rememberSaveable` hayan sobrevivido. Incumple la conservación de sesión de la pantalla 3.

**Corrección:** crear un estado único de sesión y persistir la identidad/sesión apropiada en un mecanismo local; restaurarlo antes de abrir pantallas privadas y borrarlo al cerrar sesión. Al incorporar backend, persistir y validar la sesión conforme al contrato del servidor, sin guardar contraseñas.

**Aceptación:** iniciar sesión, rotar, recrear proceso y abrir nuevamente; verificar continuidad y cierre de sesión efectivo, sin mostrar datos de otra cuenta.

### H02 — Alta: el carrito no está persistido

**Evidencia:** `CarritoRepository.kt:12`: `MutableStateFlow<Map<String, List<ItemCarrito>>>`, sin tabla ni DAO de carrito.

**Efecto:** separa clientes mientras el proceso vive, pero pierde todos los carritos cuando termina. La pantalla 9 exige SQLite por cliente.

**Corrección:** tabla de carrito con clave por usuario y producto, cantidades y referencia al producto; observarla mediante Flow. Conservar el comportamiento de vaciar únicamente después de confirmar la compra.

**Aceptación:** crear carritos distintos para A y B, terminar el proceso y reabrir; cada cuenta recupera solo el suyo. Confirmar un pedido no vacía el carrito de otro usuario.

### H03 — Alta: perfil y cuenta usan fuentes diferentes

**Evidencia:** `PerfilViewModel.kt:36` y `:55` guardan en SharedPreferences; `UsuarioRoomRepository` registra y autentica contra Room. `UsuarioDao.actualizar` existe, pero no está conectado a la edición del perfil.

**Efecto:** el perfil puede mostrar otro nombre, correo o RUT sin actualizar la cuenta almacenada. Puede mostrarse el RUT/correo de otra cuenta porque esta ruta no aplica las comprobaciones de unicidad del registro. La duplicidad visual no implica que Room haya cambiado: precisamente quedan dos versiones del dato.

**Corrección:** usar Room como fuente única del perfil local y un repositorio que valide y actualice los campos normalizados. Migrar preferencias previas de forma controlada. Mantener separado el caso de cuenta demo.

**Aceptación:** editar, cerrar sesión, reiniciar y volver a consultar; todos los consumidores leen los mismos datos. Rechazar RUT/correo ocupados al editar.

### H04 — Media: errores del perfil no cumplen el mismo estándar del registro

**Evidencia:** `PerfilScreen.kt:168` presenta un error general; `CampoPerfil`, en `:186`, no recibe error por campo ni usa `isError`/`supportingText`. `PerfilViewModel.kt:38` no valida el dígito del RUT. Registro admite de 8 a 11 dígitos telefónicos; perfil de 8 a 15. Registro cuenta dígitos, pero no rechaza letras en un teléfono que contenga suficientes dígitos.

**Efecto:** reglas distintas para el mismo dato y retroalimentación menos clara; un teléfono como `abcdefgh12345678` supera la condición telefónica del registro.

**Corrección:** compartir reglas explícitas para teléfono, correo y RUT; devolver errores tipados por campo y mostrarlos junto al control. Definir cuáles son obligatorios. Incorporar casos negativos de teléfono con letras.

### H05 — Alta para completar pantallas: el escáner general es código sin acceso

**Evidencia:** `EscanerScreen.kt:29` define el composable, pero la búsqueda de llamadas en `src/main` no encuentra ninguna invocación desde navegación. `escanearQr` solo se llama allí y en `PrepararPedidoScreen.kt:174`. Las barras actuales presentan Inicio/Catálogo/Tiendas o Inventario/Pedidos/Perfil.

**Efecto:** hay lectura QR efectiva en la preparación administrativa, pero el cliente no tiene el flujo de escanear un producto. Además, el escáner general solo muestra resultado y stock; no contiene callbacks para abrir ficha o ajuste de inventario.

**Corrección:** añadir un acceso visible al escáner y navegar por ID al detalle cliente o al inventario administrador. Añadir confirmación visual de lectura e invalidar el resultado si el producto desaparece del catálogo.

**Aceptación:** QR válido abre la ficha del rol correcto; QR desconocido o inválido muestra error; cancelar vuelve al flujo anterior. Probar con cámara real y Google Play Services disponible.

### H06 — Alta para el caso completo: notificaciones ausentes

**Evidencia:** `HomeAdmin.kt:156` tiene `onClick = {}` en Notificaciones. No se encontraron pantalla, entidades locales de avisos, canales, servicio push o permiso `POST_NOTIFICATIONS`.

**Efecto:** no hay avisos de pedidos/stock ni bandeja consultable, exigidos por el caso y las pantallas 13–15.

**Corrección:** modelar avisos y lectura, implementar bandeja y navegación al pedido/producto; integrar emisión desde backend y recepción push. Solicitar permiso en versiones Android que lo requieren. Una notificación exclusivamente local no demuestra recepción push desde otro dispositivo.

### H07 — Media: seleccionar imagen no permite tomar una fotografía

**Evidencia:** `AgregarProductoScreen.kt:181` y `EditarProductoScreen.kt:184` usan exclusivamente `ActivityResultContracts.OpenDocument`; no hay captura fotográfica ni configuración FileProvider.

**Efecto:** la imagen seleccionada sí funciona como recurso del dispositivo, pero no satisface «utilizar la cámara para fotografiar cada producto» de la pantalla 11. El avatar del perfil es un ícono fijo.

**Corrección:** ofrecer tomar foto además de seleccionar, guardar una URI duradera y manejar cancelación/error. Para backend, subir la imagen y cachear una copia accesible; una URI local no sirve en otro celular. Añadir cambio de avatar si se mantiene el alcance de la pantalla 15.

**Precisión:** la ausencia de permiso CAMERA no es por sí sola un error del escáner Google: ese SDK delega su interfaz de captura y no exige que esta app solicite dicho permiso. La captura fotográfica debe diseñarse según la API elegida.

### H08 — Media: confirmación de compra incompleta

**Evidencia:** `CarroScreen.kt:316` muestra un AlertDialog con número de productos y total; `Pedido.kt` no tiene modalidad, punto de retiro, dirección o datos de contacto de la solicitud. Después de comprar se abre el historial y aparece un Toast, sin una pantalla de éxito con esos datos.

**Efecto:** existe confirmación funcional local, pero faltan elementos de la pantalla 10. El administrador tampoco puede consultar cómo entregar la compra.

**Corrección:** incorporar resumen detallado, modalidad, ubicación y contacto ficticio; validar, persistir y enviar esos datos en el contrato del pedido. Mostrar identificador y estado inicial con acceso a Mis pedidos.

### H09 — Media: inventario sin movimientos y mapa con datos solo en código

**Evidencia:** `InventarioScreen.kt` lista y conduce a edición del producto; no hay entidad de movimiento con entrada/salida/cantidad. `PuntoRetiroRepository.kt` contiene una lista fija en memoria.

**Efecto:** cambiar el stock absoluto permite operar el MVP, pero no conserva trazabilidad de movimientos ni cumple completamente la pantalla 12. Los puntos se muestran, pero no se obtienen desde API o SQLite como propone la pantalla 5.

**Corrección:** añadir ajustes explícitos por cantidad con motivo y fecha; llevar su validación al backend. Obtener puntos de retiro de servicio propio o persistir datos sintéticos en SQLite mientras se implementa.

### H10 — Media: hay controles sin acción y pantallas aún ausentes

**Evidencia:** `LoginScreen.kt:225`, «¿Olvidaste tu contraseña?», tiene callback vacío. No se encontró bienvenida con Comenzar, bandeja de notificaciones ni preferencias de notificación/permisos en Perfil.

**Corrección:** implementar la recuperación si se mantiene visible; mientras no exista, retirar o explicar claramente su indisponibilidad. Agregar bienvenida y configuración según la propuesta de pantallas. Recuperación de contraseña es un compromiso de la UI actual; no aparece como pantalla obligatoria independiente del documento.

### H11 — Media: transacciones y navegación dependen demasiado de la Activity

**Evidencia:** `MainActivity.kt` tiene 781 líneas y coordina sesión, compra, preparación, múltiples banderas de navegación, Toast y corrutinas. `OylDatabase.kt:12` importa `screens.ProductosDatos` para inicializar la capa de datos. Los ViewModels de producto e historial mantienen rutas alternativas de memoria y Room.

**Efecto:** mayor dificultad para probar el comportamiento real y añadir red. Muchas pruebas JVM de producto ejercitan la ruta en memoria, mientras que la app utiliza Room; las pruebas instrumentadas complementan esta diferencia, pero no la eliminan.

**Corrección:** extraer coordinación de compra/sesión a ViewModels o casos de uso; introducir interfaces de repositorio e inyectar implementaciones/fakes; mover los datos iniciales fuera de `screens`. Usar rutas tipadas o Navigation Compose si reduce la complejidad, sin reescritura innecesaria.

**Riesgo a probar:** una compra sigue en `lifecycleScope` al salir del carrito; si el usuario cambia de sesión durante una suspensión, el vaciado posterior usa el cliente actualmente activo en `CarritoViewModel`. Capturar identidad y contenido al iniciar, y finalizar contra ese carrito; probar la concurrencia antes de conectar un backend más lento. Es un riesgo inferido del código, no un fallo reproducido en esta auditoría.

### H12 — Media: falta README en la rama auditada y evidencia accesible de planificación

**Evidencia:** `git ls-files '*README*'` no devuelve archivos en `desarrollo-marco`. Sí existe README en la referencia local `origin/main`, pero eso no lo incorpora a esta rama. No se encontraron documentación Trello ni workflows CI versionados.

**Efecto:** una persona que clone esta rama no encuentra instrucciones ni integrantes; incumple la forma de entrega de ambas evaluaciones. La actividad Git local es real: hay 98 commits repartidos entre cinco identidades de autor, aunque pueden representar alias de las mismas personas; no demuestra por sí sola reparto equitativo de tareas.

**Corrección:** README actualizado con integrantes reales, funcionalidades terminadas, limitaciones, Java 17, SDK, ejecución/pruebas, backend/API, endpoints, APK/firma y evidencia Trello. Vincular el tablero y sus tareas con commits concretos. No inventar integrantes, tareas ni evidencias retrospectivas.

**Git:** el árbol inicial estaba limpio y `origin` apunta a `https://github.com/AcxelGonzalez/APP_Movil_Frutos.git`. La rama actual coincide con su referencia local de seguimiento. Las referencias locales de `main` y `desarrollo` contienen diferencias y no se actualizaron con fetch en esta auditoría: no prueban cuál es la última versión en GitHub ni su visibilidad/acceso docente. Antes de integrar, revisar el diff y elegir explícitamente la rama de entrega.

### H13 — Baja: mejoras técnicas y de presentación

- **Cambio de precio:** `ProductoViewModel.validarStockParaCompra` ya detecta el cambio y explica quitar/agregar el artículo. No es una sobreventa ni un bloqueo sin salida. Mejorar con una acción explícita para aceptar el precio nuevo; la sincronización del carrito conserva intencionalmente el antiguo.
- **Productos descriptivos:** `ValidacionFormularioProducto` no recibe descripción ni presentación. Se pueden guardar vacías; decidir su obligatoriedad y validar si se requieren fichas completas.
- **Accesibilidad:** los botones de cantidad del carrito se dibujan con `Box.size(30.dp)` y `clickable` (`CarroScreen.kt:229`), sin tamaño táctil mínimo explícito. Verificar y ampliar a 48 dp; revisar fuente grande, TalkBack, contraste y teclado en formularios.
- **Detalles extensos:** el diálogo de detalle del historial utiliza una Column con todos los artículos; comprobar pedidos largos y agregar desplazamiento explícito para asegurar acceso al total y controles.
- **Esquemas:** `exportSchema = false` en Room limita trazabilidad del esquema. Exportar esquemas versionados y conservar las pruebas de migración existentes.
- **Copias de seguridad:** `allowBackup=true` y reglas de ejemplo no excluyen datos concretos. Definir qué datos ficticios/caché se respaldan y cómo restaurar imágenes URI. No se verificó una filtración ni se justifica tratar este MVP como un sistema productivo.
- **Defensa de repositorio:** `CarritoRepository.agregar` suma cantidades con Int y no valida entradas negativas; la UI actual sí controla cantidades. Centralizar invariantes y usar suma segura antes de nuevas rutas de entrada. Es endurecimiento de la API interna, no un fallo de compra normal demostrado.
- **Dependencias:** hay versiones antiguas, pero no se realizó análisis de vulnerabilidades de dependencias. Actualizar de manera compatible después de priorizar los requisitos, sin migrar todo antes de una entrega.

## 6. Cobertura de las 15 pantallas propuestas

| N.º | Pantalla | Situación en esta rama |
|---:|---|---|
| 1 | Bienvenida / Inicio | No hay bienvenida independiente con Comenzar; se inicia en login |
| 2 | Registro | Implementado con Room y validación; endurecer teléfono y unificar perfil |
| 3 | Login | Implementado con cuentas demo y registradas; falta sesión persistente |
| 4 | Dashboard | Implementado para ambos roles; notificaciones sin acción; revisar recursos y navegación |
| 5 | Mapa | Implementado con osmdroid y marcadores; puntos en código, sin GPS ni Retrofit |
| 6 | Catálogo | Implementado con búsqueda y filtros; fuente local Room |
| 7 | Detalle producto | Implementado por rol; revisar presentación completa, incluida categoría en cliente; sin API |
| 8 | Escáner de productos | Composable existente sin ruta de acceso; no abre ficha/inventario. QR de preparación sí está integrado |
| 9 | Carrito | Funcional en memoria por cliente; falta SQLite y revalidación en servidor |
| 10 | Confirmación | Diálogo de confirmación y compra local; faltan modalidad/contacto/resumen de éxito completo |
| 11 | Gestión productos | CRUD local y selector de imágenes; falta captura cámara y envío al servidor |
| 12 | Inventario | Consulta, alertas y edición absoluta; faltan movimientos explícitos y servidor |
| 13 | Historial y gestión pedidos | Implementado localmente con estados, filtros, detalles y preparación QR; faltan servidor y notificaciones |
| 14 | Notificaciones | Ausente |
| 15 | Perfil y configuración | Datos editables en preferencias y logout; faltan Room unificado, avatar y configuración |

La propuesta admite reorganizar visualmente pantallas: lo relevante es que las funciones y flujos estén disponibles. Un diálogo puede cubrir una confirmación si contiene y valida todos los datos necesarios; no hace falta un archivo Kotlin por cada fila.

## 7. Matriz y trabajo necesario de la evaluación parcial 4

| Indicador | Peso | Estado comprobado | Qué falta para acreditarlo |
|---|---:|---|---|
| IE 3.1.1: frontend completo y funcional | 15% | Parcial y reutilizable | Resolver H01–H10 y comprobar flujos reales, rotación, errores y formularios |
| IE 3.1.2: microservicios Spring Boot + BD | 15% | Sin código en esta carpeta | Servicios necesarios, BD activa, contratos, validación, errores y ejecución documentada |
| IE 3.1.3: integración CRUD con backend | 15% | No implementada | Retrofit, DTO/mappers, repositorios remotos, CRUD visible y sincronización coherente |
| IE 3.1.4: API externa mediante Retrofit | 15% | No implementada | Consumo externo y presentación útil con estados de carga/error |
| IE 3.2.1: pruebas ≥80% de lógica | 15% | Hay 70 pruebas correctas, sin porcentaje acreditado | Medir cobertura de código lógico real, completar huecos y ejecutar suites reproducibles |
| IE 3.3.1: APK firmado y configuración | 10% | APK debug; sin signingConfig release | JKS académico, firma release, verificación, evidencia y entrega requerida |
| IE 3.3.2: colaboración GitHub/Trello | 15% | Historial local existente; documentación parcial/ausente | Tablero verificable, trabajo distribuido, README, fuente backend y entrega coherente |

Los tres indicadores de backend/integración/API suman **45% de la rúbrica** y están pendientes en el material local. Pruebas y firma suman otro **25%** cuyo cumplimiento completo todavía no se acredita. Esto orienta prioridades; no es una predicción de nota.

### Backend propio y stock

Diseñar primero contratos para catálogo/productos, usuarios/perfiles, pedidos y estados, y puntos de retiro si se gestionan desde servidor. El documento no fija una cantidad exacta de microservicios: no conviene inventar una cifra como requisito. Delimitar los servicios necesarios con el docente/equipo y conservar el código fuente y sus commits.

El servidor debe validar rol/propiedad, cantidades, precios y transiciones. La transacción de creación/reserva/descuento debe evitar sobreventa de dos clientes concurrentes. Si se separan servicios y bases, no asumir que una transacción local de uno de ellos cubre automáticamente todos los demás: definir cómo se confirma la operación y cómo se manejan fallos.

Room debe pasar de fuente oficial a caché de catálogo/pedidos. Evitar confirmar una compra como definitiva solo con una escritura local sin respuesta del backend. Para reintentos, acordar una clave de idempotencia estable que impida duplicar una misma solicitud. Esto es una recomendación de implementación para conservar consistencia, no un endpoint impuesto por la rúbrica.

### API externa

El mapa descarga teselas mediante osmdroid, pero **no cumple por sí solo** el requisito explícito de consumir una API externa vía Retrofit. Una opción coherente es mostrar clima del punto de retiro con una API pública como Open-Meteo: datos externos independientes del inventario, visibles en el mapa o ficha de retiro. Es una propuesta; no se ha implementado ni contratado ningún servicio.

Separar cliente/URL y modelos externos de los servicios propios. Añadir carga, error, falta de conexión y recuperación. No permitir que un fallo externo bloquee consultar catálogo o gestionar pedidos.

### Pruebas y cobertura

No hay base para decir que el proyecto ya cubre 80%, ni para afirmar un porcentaje inferior sin medir. Número de pruebas y cobertura son métricas diferentes. Parte de la lógica de login, registro y preparación está probada en `androidTest`, aunque algunos validadores puros podrían probarse en JVM.

Introducir JaCoCo u otra herramienta compatible con Kotlin 1.8.10/AGP 8.2.2, delimitar `viewmodel`, `repository`, validadores y lógica de negocio; excluir código generado de Room/Compose de forma justificada. Si se combinan pruebas JVM y Android, fusionar sus datos sin contar dos veces las mismas clases. Publicar cobertura por clase y total de líneas; registrar también ramas para encontrar decisiones sin probar. La pauta no especifica si el 80% se evalúa por líneas o ramas: explicitar la métrica utilizada.

Priorizar pruebas de sesión restaurada, carrito persistente por cliente, edición única del perfil, compra con stock/precio modificado, rollback, reintento de red sin duplicación, permisos denegados y errores externos. Conservar las pruebas de migraciones que ya funcionan. Las pruebas `addition_isCorrect` y de contexto pueden mantenerse, pero no evidencian lógica comercial.

### Firma y entrega

`app/build.gradle.kts:28` configura release sin `signingConfigs`; el APK generado es debug. `.gitignore` ignora `*.apk`, `*.jks` y `*.keystore`. Esto es razonable como política general, pero requiere un procedimiento de entrega académica que satisfaga el PDF.

Generar una llave **exclusiva de demostración académica**, configurar release, construir e instalar el APK y verificar su firma con `apksigner verify --print-certs`. El encargo solicita APK y llave, además de captura en README: preparar esos elementos y una ubicación de entrega explícita. No reutilizar una llave productiva ni introducir credenciales reales. No se generaron llaves ni se publicaron archivos durante esta auditoría.

## 8. Advertencias Lint y entorno

Distribución de las 33 advertencias:

| Tipo | Cantidad | Recomendación |
|---|---:|---|
| GradleDependency | 10 | Revisar versiones en conjunto, sin actualizaciones incompatibles aisladas |
| UnusedResources | 9 | Limpiar recursos realmente no usados |
| IconLocation | 7 | Evaluar `drawable-nodpi` o recursos por densidad para los PNG |
| VectorRaster | 2 | Reducir vectores demasiado grandes o usar formato apropiado |
| OldTargetApi | 1 | Probar compatibilidad antes de actualizar targetSdk 34; el caso no exige publicar en tienda |
| RedundantLabel | 1 | Limpieza del manifiesto |
| DiscouragedApi | 1 | Sustituir `getIdentifier` por un mapeo estable de recursos cuando corresponda |
| VectorPath | 1 | Optimizar el path extenso de `logo_oyl_vector.xml` |
| KaptUsageInsteadOfKsp | 1 | Valorar KSP al actualizar la cadena Kotlin/Room |

El compilador también informa que el parámetro `nombre` no se usa en ambos Home. No es un error de autenticación. El log de pruebas instrumentadas incluye una asignación a null no utilizada en un test.

Estas advertencias no tienen la misma prioridad que los requisitos de backend, persistencia y firma. No se recomienda convertir una auditoría en una actualización masiva de dependencias antes de cerrar los flujos faltantes.

## 9. Orden recomendado para continuar

1. **Asegurar una base reproducible:** documentar Java 17 y ejecución, recuperar/actualizar README y elegir rama de integración. Conservar este commit como referencia sin alterar la entrega histórica.
2. **Cerrar la persistencia local:** sesión, carrito Room por usuario y perfil con una única fuente. Homogeneizar validaciones y probar rotación/reinicio/cambio de cuenta.
3. **Conectar funciones ya escritas:** acceso al escáner, navegación por QR según rol, corrección de botones sin acción y confirmación con datos completos.
4. **Implementar backend y contratos:** servicios Spring Boot con BD, catálogo y pedidos transaccionales; integrar progresivamente Retrofit desde UI.
5. **Completar API externa y notificaciones:** consumo externo visible; push, bandeja y acceso a pedidos; foto de producto y preferencias según el caso.
6. **Acreditar calidad:** cobertura medible ≥80% de lógica, pruebas de red/rollback y una pasada funcional de todos los flujos en dispositivo.
7. **Preparar entrega:** APK release firmado, llave académica, verificación, README con endpoints/evidencias, fuente backend, Trello y coherencia de ramas/AVA.

Las pruebas deben acompañar cada cambio sustancial; la medición y evidencia final pueden consolidarse al terminar. No corresponde esperar al último día para descubrir problemas de cobertura o firma.

## 10. Lista de aceptación para la siguiente revisión

- [ ] Sesión coherente después de rotación y recreación del proceso; logout efectivo.
- [ ] Carritos de dos clientes sobreviven al reinicio y permanecen separados.
- [ ] Perfil y cuenta presentan datos únicos y consistentes en Room/API.
- [ ] Formularios muestran errores por campo y rechazan entradas inválidas.
- [ ] Cliente escanea QR y abre detalle; administrador escanea y llega al inventario.
- [ ] Captura y selección de imágenes funcionan después de reiniciar.
- [ ] Confirmación incluye artículos, modalidad y contacto; devuelve ID/estado real.
- [ ] Dos compras concurrentes no producen stock negativo ni pedidos duplicados.
- [ ] Estados de pedido cambian en secuencia y se ven en el dispositivo del cliente.
- [ ] API externa consumida mediante Retrofit y visible; su fallo no bloquea funciones principales.
- [ ] Avisos push y bandeja funcionan con permiso concedido y denegado.
- [ ] Cobertura de lógica documentada y ≥80%; suites reproducibles correctas.
- [ ] APK release instalado y firma verificada; entrega académica de APK/llave preparada.
- [ ] README, endpoints, integrantes y Trello verificables; rama de entrega acordada.
- [ ] Revisión manual de pantallas pequeñas, fuente grande, teclado, Atrás y pedidos extensos.

La base actual se puede aprovechar ampliamente. El siguiente esfuerzo debe concentrarse en persistencia coherente y en la integración exigida por EP4, manteniendo la protección transaccional que ya existe localmente.
