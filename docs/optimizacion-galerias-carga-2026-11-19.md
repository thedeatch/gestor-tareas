# Optimización de galerías y arranque — 19 de noviembre de 2026

## Cambios aplicados

### Galería del grupo

Antes se descargaban los originales de todas las imágenes del álbum mediante peticiones concurrentes y se esperaba a que terminaran para presentar la colección. También se descargaban los originales para las portadas.

Ahora:

- Las portadas y el mosaico utilizan **miniaturas JPEG de hasta 480 píxeles por lado**, conservando la proporción y sin ampliar imágenes pequeñas.
- El servidor genera cada miniatura cuando se solicita por primera vez y la conserva en una tabla de caché. Las solicitudes posteriores reutilizan esos bytes.
- Se comprueba la membresía y la correspondencia entre grupo, álbum e imagen **antes de acceder a la caché**. La respuesta conserva `Cache-Control: no-store`.
- El cliente solicita una miniatura cuando se acerca a la pantalla, con un margen de anticipación de 240 píxeles.
- Las descargas de miniaturas comparten una cola con un máximo de **cuatro solicitudes activas**. Las pendientes y activas se cancelan cuando deja de utilizarse su componente.
- Se presentan **24 fotos** o **12 álbumes** inicialmente. Los botones Ver más añaden otro bloque.
- Una miniatura que no puede generarse muestra un icono alternativo; el botón de la foto sigue permitiendo abrir el original.

La miniatura se almacena junto a su imagen mediante una clave foránea con eliminación en cascada. Los originales existentes no se reemplazan ni se recomprimen. No se genera una copia anticipada de toda la biblioteca.

La generación consulta las dimensiones y utiliza submuestreo antes de decodificar. Rechaza imágenes de más de 80 millones de píxeles para limitar el trabajo de generación. Los PNG transparentes reciben un fondo blanco al convertirse en JPEG. Las imágenes corruptas producen una respuesta 422 y no se almacenan como miniaturas válidas.

### Visor de fotos

La galería entrega los metadatos de todo el álbum al visor. Este descarga el original de la imagen seleccionada, sin precargar todos los demás. Al pasar a otra foto, se cancela la descarga anterior y se libera la URL temporal del original que ya no se utiliza.

Se conservan la navegación por todas las imágenes, los comentarios, las reacciones, los guardados, los datos de fecha y los controles para ampliar la foto. Si el original falla, aparece una opción para reintentarlo.

### Arranque

La barra de navegación se obtiene mediante un bloque de carga diferida inmediata. El contenedor inicial reserva sus 64 píxeles de altura mientras se carga, e incluye una alternativa si falla. El código de la barra sigue descargándose tras el arranque; esta separación reduce el archivo inicial, no elimina ese código del producto.

## Medidas obtenidas

| Medida | Antes | Después |
|---|---:|---:|
| Paquete inicial de producción | 573,18 kB | **354,58 kB** |
| Transferencia inicial estimada por Angular | 135,39 kB | **93,27 kB** |
| Originales pedidos al abrir el álbum simulado de 120 fotos | Antes se solicitaban todos los del álbum | **0** |
| Originales pedidos al abrir una foto en los recorridos visuales | Estaban precargados | **1 seleccionado** |
| Fotos representadas inicialmente en el álbum grande | Todas | **24** |

El paquete inicial es aproximadamente un **38 % menor**. Esto no equivale a afirmar que toda la aplicación cargue un 38 % más rápido: faltaría medir tiempos reales con caché fría, red controlada y servidor real. Desapareció la advertencia del presupuesto inicial de 500 kB. Se mantienen las advertencias de tamaño de algunos estilos.

La prueba de integración usa un PNG sintético de 900×600 con ruido. Su miniatura conserva la proporción, cabe en el límite de 480 píxeles y ocupa **menos del 25 % de los bytes del original**. Es una comprobación sobre esa imagen de prueba, no un porcentaje garantizado para cualquier archivo.

## Verificación

- **110 pruebas del backend** correctas mediante `mvnw.cmd verify`, con JAR construido.
- **109 pruebas del frontend** correctas.
- Compilación de producción correcta.
- Revisión visual aislada: **109 capturas y 42 comprobaciones de interacción**, sin errores de ejecución ni desbordamiento horizontal del documento en los casos probados.
- Pruebas nuevas de cola de descargas, carga por proximidad, cancelación, liberación de URL, bloques de fotos y originales bajo demanda.
- Pruebas de integración de permisos, generación, reutilización, conservación del original, errores y eliminación de la caché.
- `npm audit`: **0 alertas**.

Las pruebas visuales utilizan respuestas simuladas, como se explica en la [revisión visual](revision-visual-2026-11-19.md). Las pruebas de API y migración usan H2; no se midió carga concurrente de producción ni se ejecutaron los recorridos con una sesión real.

## Archivos principales

- `backend/src/main/resources/db/migration/V55__cache_gallery_thumbnails.sql`: tabla de miniaturas.
- `GalleryThumbnailService`: generación y reutilización de miniaturas.
- `CommunityAlbumService` y `CommunityAlbumController`: endpoint autenticado `/{albumId}/images/{imageId}/thumbnail`.
- `src/app/services/image-downloads.ts`: cola compartida de miniaturas.
- `src/app/components/lazy-image`: proximidad a la pantalla y ciclo de vida de las imágenes.
- `src/app/components/community-gallery`: bloques de fotos y álbumes, y visor bajo demanda.
- `src/app/app.html`: carga diferida de la barra.

## Aplicar y probar

```powershell
docker compose up -d --build
```

El backend aplicará **V55**. Después, abrir un grupo, entrar en Eventos y galería y abrir un álbum. Las miniaturas se generan conforme se necesitan; no requiere volver a subir las fotos.

Para repetir las comprobaciones visuales:

```powershell
npm run test:visual
```

## Límites y próximos trabajos

- El listado de metadatos de las fotos sigue llegando completo desde la API. Los bloques limitan los elementos representados y la descarga de binarios; la paginación de metadatos en el servidor sigue pendiente.
- Los cambios de mosaico y portadas se aplican a la galería del grupo. Los previews de la página Guardados no se refactorizaron en esta etapa.
- La caché de miniaturas ocupa espacio adicional en la base de datos, proporcional a las fotos cuyas miniaturas se hayan solicitado.
- Sigue pendiente optimizar los listados grandes de publicaciones, eventos y tareas, y el cálculo de recuerdos que actualmente ordena datos en memoria.
- La versión estable continúa aplazada.
