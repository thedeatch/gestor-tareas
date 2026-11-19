# Paginación y vistas previas — 19 de noviembre de 2026

## Cambios completados

### Datos de las fotos en el servidor

La galería solicita los metadatos en páginas de **24 fotos**, en lugar de recibir todos los datos del álbum al abrirlo. El botón Ver más fotos obtiene la página siguiente. El contador muestra el total del álbum.

El visor recibe una página de metadatos. Al llegar al borde, consulta la página anterior o siguiente; también puede pasar de la primera foto a la última y volver al comienzo. Solo descarga el original seleccionado. Si falla una consulta, mantiene la foto actual y permite reintentar con las flechas.

Se añadieron dos endpoints autenticados:

- `GET /api/communities/{groupId}/albums/{albumId}/images/page?page=0`: items, total, page y size, con tamaño fijo de 24.
- `GET /api/communities/{groupId}/albums/{albumId}/images/{imageId}/details`: metadatos de una única imagen.

Ambos verifican la membresía y el álbum del grupo. El endpoint anterior que devuelve la lista completa se conserva por compatibilidad, pero la galería, Guardados y Recuerdos ya no necesitan utilizarlo para abrir una foto.

La migración **V56** añade un índice por álbum e identificador descendente para apoyar el recorrido paginado. No altera los archivos ni las miniaturas existentes.

### Recuerdos

La base de datos calcula ahora el filtro de antigüedad, los aniversarios, el orden y los límites de página. Java solo materializa los **20 resultados de la página solicitada**, sin crear y ordenar una colección con todas las fotos del grupo.

Se conservan las reglas anteriores:

- Fecha completa: años cumplidos desde la fecha real.
- Solo año: diferencia entre años del calendario.
- Sin fecha histórica: fecha de subida en la zona configurada, por defecto America/Santiago.
- El 29 de febrero cumple aniversario el 28 de febrero en años no bisiestos.
- Primero aparecen los aniversarios, después los recuerdos más antiguos, y el identificador resuelve los empates.

La consulta sigue necesitando calcular y contar candidatos dentro de la base de datos; la mejora elimina la materialización completa en la memoria de Java, no todo el trabajo de consulta.

### Miniaturas en Guardados y Recuerdos

Ambas vistas utilizan el componente de miniaturas por proximidad y la cola compartida de cuatro descargas. Dejaron de obtener todos los originales de la página para mostrarlos como tarjetas.

Al abrir una tarjeta, consultan los datos de esa foto y entregan el control al visor, que obtiene el original. No descargan la lista completa de su álbum ni muestran una miniatura como si fuera la imagen original.

## Comprobaciones

| Comprobación | Resultado |
|---|---|
| Suite completa del backend con H2, Maven verify | **112 pruebas correctas**, ejecutable construido |
| Pruebas adicionales contra PostgreSQL 17.11 | **7 pruebas correctas**, incluidas las migraciones hasta V56 |
| Suite del frontend | **111 pruebas correctas**, en 49 archivos |
| Recorridos visuales aislados | **112 capturas y 45 comprobaciones**, sin errores ni desbordamiento horizontal en los casos probados |
| Compilación de Angular | Correcta; paquete inicial alrededor de **354,58 kB** |
| npm audit | **0 alertas** |

La paginación de imágenes se comprueba con 50 filas: primera página de 24, segunda de 24 y última de 2. Se verifican el orden, el total, la consulta individual y la denegación de acceso a usuarios ajenos al grupo.

Las reglas de Recuerdos se comparan contra el cálculo de calendario anterior utilizando **55 fotos**, cuatro fechas de referencia, ambos filtros y cuatro páginas por combinación. Incluyen subidas cercanas a medianoche UTC y aniversarios del 29 de febrero.

La prueba del visor comprueba el cruce de páginas, la vuelta al inicio, el mantenimiento de la foto ante un error y la cancelación de consultas al cerrarlo. El recorrido visual incluye un álbum simulado de 120 fotos y tarjetas de Guardados y Recuerdos que no descargan originales hasta abrirse.

## PostgreSQL aislado

Las pruebas adicionales se ejecutaron en un contenedor temporal de PostgreSQL, sin volúmenes, con un puerto aleatorio limitado a 127.0.0.1. No se utilizó la base de datos del proyecto. El contenedor se detuvo y eliminó al terminar.

Se ejecutaron `GalleryThumbnailControllerIntegrationTests`, `CommunityMemoryControllerIntegrationTests` y `CommunityMemoryPaginationIntegrationTests`, con las propiedades de datasource del proceso Maven dirigidas a esa instancia temporal.

Esto verifica los endpoints y las consultas contra PostgreSQL. Los recorridos del navegador continúan usando respuestas simuladas: no equivalen a un recorrido con una sesión real conectada al backend ni a una prueba en dispositivos físicos.

## Aplicar los cambios

```powershell
docker compose up -d --build
```

El backend aplicará V56. No requiere volver a subir las fotos.

Para repetir las comprobaciones habituales:

```powershell
npm test -- --watch=false
npm run test:visual
```

Desde el directorio backend:

```powershell
./mvnw.cmd verify
```

## Pendientes posteriores

- Paginar los listados de publicaciones, eventos y tareas. Esos cambios deben preservar filtros, estadísticas, calendarios y enlaces a recursos concretos.
- Revisar el listado de álbumes en el servidor, cuyo bloque visual de 12 todavía utiliza el listado completo de álbumes.
- Evaluar un cursor estable para álbumes que cambien mucho durante la navegación; la paginación actual utiliza posiciones y puede cambiar si otros usuarios añaden o eliminan fotos entre solicitudes.
- Completar los recorridos con una sesión real, dispositivos físicos y otros navegadores.
- Actualizar el informe general de funciones. Las tareas recurrentes y las menciones siguen como propuestas pendientes.

La versión estable permanece aplazada. No se hizo commit ni push en esta etapa.
