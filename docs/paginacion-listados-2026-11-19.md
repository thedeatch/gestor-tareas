# Paginación de publicaciones, eventos, tareas y álbumes

Fecha: 19 de noviembre de 2026.

## Comportamiento

| Listado | Tamaño | Orden |
|---|---:|---|
| Publicaciones | 20 | Fecha de creación descendente; identificador para desempatar |
| Eventos | 20 | Próximos primero; fecha e identificador ascendentes |
| Tareas personales y del grupo, vista Lista | 20 | Identificador descendente |
| Álbumes | 12 | Identificador descendente |

Los botones Anterior y Siguiente muestran la página y el total. Se ocultan si solo existe una página y se desactivan durante la solicitud. Una página vacía después de eliminar contenido se ajusta a la última disponible.

El servidor obtiene primero los identificadores de la página mediante LIMIT/OFFSET y después los datos de esos elementos. El conteo total no requiere descargar la colección completa al navegador. En álbumes solo se agregan las imágenes de los álbumes de la página; en eventos solo se agregan sus inscripciones; en publicaciones solo se obtienen las reacciones de la página.

## Tareas y filtros

La vista Lista filtra en la base de datos por texto, estado, prioridad y fecha. Las tareas personales permiten además proyecto y categoría; las del grupo, persona asignada. La búsqueda ignora tildes, busca todas las palabras y trata los caracteres `%` y `_` como texto literal.

Cambiar un filtro vuelve a la primera página. Cambiar de página conserva los filtros. Las solicitudes anteriores se cancelan para que una respuesta antigua no sustituya una búsqueda nueva. Se envía la fecha local del navegador para mantener los filtros Hoy, Vencidas y Próximos siete días alineados con las otras vistas.

El total filtrado y el total general se calculan sobre toda la colección autorizada. Las listas de pendientes y completadas muestran los elementos de la página actual, indicándolo en pantalla. El selector para clasificar tareas utiliza las tareas visibles.

Kanban, calendario y estadísticas conservan la colección completa y los filtros existentes. Esa colección se solicita al abrir una de esas vistas. **La paginación no limita las estadísticas a los veinte elementos de una página.** Optimizar esas vistas con agregados o ventanas independientes queda como trabajo posterior.

Las opciones del filtro de responsables incluyen los usuarios con tareas asignadas, aunque ya no sean miembros y sus tareas estén en otra página. Esta consulta devuelve únicamente identificador y nombre, no las tareas completas.

## Endpoints y enlaces

- `GET /api/communities/{groupId}/posts/page`
- `GET /api/communities/{groupId}/events/page`
- `GET /api/communities/{groupId}/albums/page`
- `GET /api/communities/{groupId}/tasks/page`
- `GET /api/tasks/page`
- `GET /api/communities/{groupId}/tasks/assignees`

Los endpoints paginados aceptan `page` desde cero y devuelven `items`, `total`, `page`, `size` y `allTotal`. Los de grupo aceptan además `target`: localiza la página de una publicación, evento, álbum o tarea enlazada desde búsquedas, guardados o notificaciones. No es necesario recorrer las páginas anteriores para encontrarla. Los enlaces antiguos de publicaciones con `#publicacion-ID` abren la sección correspondiente.

Se verifica la membresía antes de consultar listados del grupo. Las tareas personales se limitan al usuario autenticado. Los endpoints anteriores que devuelven arreglos se conservan para las vistas que aún los necesitan y para compatibilidad.

La migración **V57** añade índices compuestos para tareas personales y publicaciones. Eventos, tareas del grupo y álbumes reutilizan los índices existentes.

## Aplicación local

Reconstruir y reiniciar el backend para incorporar los endpoints y aplicar V57 con Flyway. Actualizar también el frontend. Si ambos se sirven con Docker Compose: `docker compose up -d --build backend frontend`.

No se requiere editar datos ni ejecutar manualmente los índices. La migración anterior V56, si aún no se aplicó, se ejecutará antes de V57.

## Validación

- Backend: `mvnw.cmd verify`, 115 pruebas correctas y ejecutable construido.
- PostgreSQL 17: tres pruebas de integración adicionales del controlador paginado, incluyendo migraciones hasta V57, filtros sobre elementos antiguos, aislamiento entre usuarios, membresía, localización de enlaces y límites de página. Base temporal aislada y eliminada al terminar.
- Frontend: 116 pruebas correctas en 50 archivos. Incluye cancelación de búsquedas, conservación de filtros, creación desde páginas posteriores, actualización después de eliminar y notificaciones antiguas de publicaciones.
- Chrome con respuestas simuladas: 127 capturas y 63 comprobaciones. Incluye avanzar y retroceder páginas de los cinco listados y buscar una tarea fuera de la primera página, a 1440, 390 y 320 píxeles. Sin errores de ejecución, solicitudes inesperadas ni desbordamiento horizontal en esos recorridos.
- Compilación de producción correcta; paquete inicial de 354,58 kB. Continúan los avisos de presupuesto CSS previamente existentes en barra de navegación, galería y detalle del grupo.

Los recorridos de Chrome verifican el frontend con datos sintéticos; las pruebas de integración verifican por separado el backend. Queda pendiente la comprobación manual completa con la sesión y los datos reales, además de Safari, Firefox y dispositivos físicos.
