# Revisión técnica del proyecto — 19 de noviembre de 2026

Actualización posterior: las dependencias pendientes de este informe se revisaron y actualizaron. Consulta el [resultado de la actualización](actualizacion-dependencias-2026-11-19.md). Las cifras de auditoría de abajo describen el estado anterior a esa actualización.

## Alcance y resultado

Se realizó una revisión transversal del código, las pruebas existentes, las migraciones y la compilación. Se corrigieron los fallos descritos abajo y se añadieron pruebas de regresión. Esta revisión corresponde a la primera parte técnica; quedan pendientes la revisión visual en navegador, las pruebas con volúmenes grandes y la actualización de dependencias.

Se conservaron los cambios de las funciones recientes. No se preparó una versión estable, no se creó un commit ni se publicó ningún cambio. Las tareas recurrentes y las menciones siguen como propuestas futuras.

## Problemas corregidos

| Prioridad | Problema encontrado | Corrección y evidencia |
|---|---|---|
| Alta | El POST de tareas aceptaba un identificador enviado por el cliente. Era posible reutilizar el ID de una tarea ajena y sobrescribirla. | Se reprodujo con una prueba que falló antes de la corrección. `TaskService.create` construye ahora una entidad nueva y el ID es de solo lectura en JSON. La prueba verifica también que la tarea ajena conserve su contenido y propietario. |
| Alta | Dos renovaciones simultáneas podían leer el mismo refresh token antes de su rotación. | La consulta de consumo usa bloqueo de escritura dentro de la transacción. Una prueba con dos transacciones verifica que la segunda espere y rechace el token consumido. |
| Media | Después de renovar la sesión, un error 403 o 500 del recurso solicitado cerraba también la sesión. | Se separó el manejo de errores de renovación del manejo del recurso. Las pruebas cubren errores temporales, rechazo real de la sesión y peticiones simultáneas. |
| Media | Publicaciones y comentarios pedían avatares con un `img` dirigido a la API protegida. Esa petición no pasa por el interceptor que añade el JWT. | Se añadió `UserAvatar`, que obtiene la imagen por HttpClient, cancela peticiones antiguas y libera sus URL temporales. Se conserva el icono alternativo cuando la foto no está disponible o el perfil es privado. |
| Media | Al navegar directamente entre dos grupos o perfiles, Angular podía reutilizar el componente y conservar la entidad anterior. | Ambas páginas escuchan los cambios del ID. Se cancelan las consultas antiguas y se reinicia el estado de la vista. Las pruebas verifican navegación, errores y destrucción. |
| Media | Una respuesta de una publicación iniciada en un grupo podía llegar después de cambiar a otro grupo. | Se cancela la suscripción de la vista anterior y se limpian sus borradores y estados de espera. La cancelación del cliente no equivale a deshacer una operación ya recibida por el servidor. |
| Media | Reintentar la carga de un grupo no limpiaba su error anterior. | Se limpia el error antes de la consulta. Una prueba reproduce el fallo seguido de una carga correcta. |
| Media | Al cerrar un álbum mientras se descargaban las fotos, la descarga seguía activa y podía actualizar una vista posterior. | Se cancelan las descargas de imágenes y portadas que dejan de utilizarse. Hay una prueba de cierre de álbum y salida de la vista. |

## Optimización aplicada

Los paneles de progreso, objetivos, eventos y galería, encuestas, recuerdos, búsqueda y canales se cargan bajo demanda al abrir su sección. Incluyen mensajes de carga y de error.

La compilación anterior concentraba aproximadamente **197,68 kB** en el archivo que contenía la página del grupo. Después de separar los paneles, ese archivo queda alrededor de **79 kB**. Los paneles se descargan en archivos separados cuando se necesitan.

Esta comparación es de archivos generados, no de tiempo de carga ni de tráfico total. Hay dependencias compartidas. La infraestructura de carga diferida aumentó el conjunto inicial de aproximadamente **556,98 kB a 563,40 kB**. La optimización reduce la carga anticipada de las secciones del grupo; todavía queda trabajo para reducir el arranque general.

Una prueba con el template real verifica que un enlace directo a Objetivos muestre el panel después de cargarlo y no consulte álbumes ni encuestas.

## Áreas revisadas

| Área | Comprobación realizada |
|---|---|
| Autenticación y sesiones | Revisión del interceptor, JWT, renovación, rotación y pruebas de autenticación. Correcciones detalladas arriba. |
| Tareas personales | Revisión de creación, consultas por propietario, actualizaciones, estados y organización. Prueba nueva contra reutilización de IDs. |
| Grupos y roles | Revisión de comprobaciones de membresía y administración. Las pruebas existentes cubren acceso de no miembros, invitaciones, roles, publicaciones y moderación. |
| Comentarios y reacciones | Ejecución de las pruebas existentes de permisos y reacciones; corrección de avatares de publicaciones y comentarios. |
| Galería y archivos | Revisión de acceso por propietario o grupo, eliminación y almacenamiento. Corrección de cancelación de descargas. Las pruebas incluyen almacenamiento externo simulado. |
| Encuestas, recuerdos, búsqueda, guardados y objetivos | Ejecución de sus pruebas de integración, validación y acceso. Se conservaron sus contratos y migraciones. |
| Perfiles | Revisión de restricciones de perfiles públicos y avatares; corrección de navegación y errores de imagen. |
| Mensajería y notificaciones | Revisión de consultas limitadas, bloqueos entre usuarios y consultas periódicas. Ejecución de las pruebas disponibles; no se midió concurrencia a gran escala. |
| Configuración y entrega | Revisión de rutas, módulos, configuración de seguridad, límites de archivos y CI. Validación de Docker Compose sin iniciar ni reconstruir los servicios. |

## Validación

- Suite inicial: **92 pruebas de frontend y 106 de backend**, correctas.
- Backend después de las correcciones: `mvnw.cmd verify`, **108 pruebas correctas** y JAR construido.
- Frontend: **106 pruebas correctas en 47 archivos** y compilación de producción correcta, con las advertencias de tamaño indicadas en este informe.
- Flyway validó las **54 migraciones** en el entorno de pruebas. Esta revisión no añade migraciones.
- `docker compose config --quiet`: correcto.
- `git diff --check`: correcto.

Las pruebas del backend utilizan H2. No se ejecutó una batería adicional contra PostgreSQL ni una prueba de carga. El intento de acceder al navegador falló porque la herramienta no pudo inicializar sus archivos locales. Por eso no se afirma haber validado visualmente el comportamiento en celular o escritorio.

## Dependencias: siguiente prioridad

`npm audit --json` informó **41 alertas**: 3 críticas, 11 altas y 27 moderadas, incluyendo herramientas de desarrollo. `npm audit --omit=dev --json` informó **7 alertas**: 2 altas y 5 moderadas.

En la segunda consulta aparecen Angular common, compiler, core, forms, platform-browser, router y zod. El registro indica correcciones disponibles. Son resultados del análisis de paquetes y no una demostración de que cada aviso sea explotable en esta aplicación. Tampoco constituyen una auditoría de las dependencias Java.

La próxima parte debería actualizar las dependencias compatibles como un cambio separado, comprobar la alineación de Angular y Material y ejecutar nuevamente las pruebas y la compilación. No se ejecutó una corrección masiva automática ni se modificó el archivo de dependencias en esta revisión.

## Trabajo pendiente, por orden

1. **Dependencias:** resolver las alertas con actualizaciones controladas y revisar también el árbol de dependencias del backend.
2. **Pruebas visuales y de recorrido:** acceso, cambio de cuenta, edición de tareas, navegación entre grupos, galería, comentarios y permisos, en escritorio y celular.
3. **Galerías grandes:** implementar miniaturas y descarga progresiva o limitada. Actualmente se obtienen los archivos originales del álbum con peticiones concurrentes.
4. **Listados grandes:** estudiar paginación de publicaciones, eventos y tareas. Recuerdos limita la respuesta a 20 elementos, pero calcula y ordena la colección completa en memoria en el servidor.
5. **Arranque y estilos:** reducir el paquete inicial y revisar los estilos de navbar, galería y detalle de grupo que superan el umbral de advertencia. No se aumentaron los presupuestos para ocultar avisos.
6. **Concurrencia adicional:** ampliar pruebas de sesiones y cambios de permisos, y medir las consultas periódicas de chat y notificaciones con varios usuarios.
7. **Documentación de estudio:** incorporar estas correcciones y las funciones nuevas al informe general de funciones.

Después de estas partes se pueden abordar las tareas recurrentes y las menciones. La preparación de una versión estable queda aplazada por decisión del usuario.
