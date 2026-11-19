# Revisión visual y de recorridos — 19 de noviembre de 2026

## Resultado

Se ejecutó Chrome sin interfaz gráfica contra la compilación de producción de Angular, en un servidor temporal limitado a `127.0.0.1`. Se generaron **108 capturas** y se comprobaron **40 interacciones** en anchos de **1440, 1280, 1024, 390 y 320 píxeles**.

La ejecución final terminó sin errores de JavaScript, sin peticiones API ajenas a las respuestas de prueba y sin desbordamiento horizontal del documento en los casos revisados. Se inspeccionaron capturas representativas de escritorio y celular, incluidos los defectos encontrados y su corrección. No se realizó una comparación automática píxel a píxel con imágenes de referencia.

## Cómo se hizo y qué significa

Las herramientas de navegador integrado y control de Windows fallaron al inicializar sus archivos locales. Como alternativa se utilizó Playwright con una instancia aislada del Chrome instalado, sin abrir el perfil del navegador del usuario.

**Todas las respuestas de `/api` se simulan con datos sintéticos.** Las tareas y comentarios creados durante estas pruebas solo existen en la memoria del proceso de prueba. No se utilizaron contraseñas, sesiones, fotos ni registros reales; tampoco se modificó la base de datos.

Esto verifica la representación de la interfaz, la navegación y el comportamiento del cliente ante respuestas controladas. No demuestra que se guarden datos en el backend real. La revisión técnica anterior contiene las comprobaciones de integración de la API, y queda pendiente un recorrido adicional que una el navegador con una sesión real y dispositivos físicos.

## Problemas encontrados y corregidos

1. **Barra superior fuera de la pantalla a 1024 píxeles.** Se reprodujo un ancho de documento de 1120 píxeles en una pantalla de 1024. El botón de cerrar sesión quedaba parcialmente fuera. Se adelantó el uso del menú compacto hasta 1360 píxeles y se limitó el ancho de los nombres de usuario, conservando el nombre completo en el título del elemento.
2. **Títulos largos con líneas demasiado juntas.** El nombre del grupo se superponía visualmente al dividirse en varias líneas en celular. Se estableció una altura de línea proporcional para los encabezados.
3. **Nombre del archivo en el visor móvil.** Un nombre largo se repartía en una etiqueta estrecha y muy alta. En pantallas pequeñas, el título del álbum y el nombre del archivo ocupan filas separadas, y el archivo puede abreviarse con puntos suspensivos.
4. **Formato de fecha inconsistente en objetivos.** La fecha de creación utilizaba el formato predeterminado estadounidense. Se cambió a `dd/MM/yyyy`, como otras fechas de la interfaz.

## Pantallas revisadas

- Inicio y navegación con cuentas sintéticas de usuario y administrador.
- Lista de tareas, Kanban, calendario y estadísticas.
- Lista de grupos, resumen, miembros y objetivos.
- Formulario de objetivos y controles visibles para un miembro sin permisos de administración.
- Eventos, álbumes, visor de fotos y comentarios.
- Guardados, estado vacío y búsqueda del grupo.
- Perfil público.
- Inicio de sesión, registro, recuperación de contraseña y error de credenciales.
- Tema oscuro y una comprobación del tema claro en celular.

## Interacciones comprobadas

- Crear una tarea y verla aparecer en su lista.
- Cambiar entre Kanban, calendario y estadísticas.
- Buscar una tarea dentro del grupo y verificar el enlace al recurso.
- Abrir y cerrar el visor de fotos.
- Ocultar comentarios para ampliar la foto y volver a mostrarlos.
- Publicar un comentario y verlo en el visor.
- Quitar un evento de guardados y mostrar el estado vacío.
- Navegar mediante el menú compacto en cuatro anchos.
- Comprobar que un miembro no vea controles de administración de objetivos.
- Mostrar un error de credenciales.
- Recuperar la lista de objetivos después de una respuesta 503 al pulsar Actualizar.

Los controles de permiso comprobados en esta fase son los de la interfaz con respuestas simuladas; la autorización del servidor se comprueba por separado en las pruebas de integración.

## Repetir la revisión

Desde la raíz del repositorio:

```powershell
npm ci
npm run test:visual
```

El comando compila Angular y ejecuta `scripts/visual-review.mjs`. Requiere Chrome instalado en la ubicación habitual de Windows. Para otra ubicación puede indicarse la variable `CHROME_PATH`. No instala navegadores ni utiliza el servidor real de la aplicación.

Se pueden limitar los anchos durante una comprobación puntual:

```powershell
$env:VISUAL_WIDTHS = '390,1024'
npm run test:visual
Remove-Item Env:VISUAL_WIDTHS
```

El resultado completo se escribe en `tmp/visual-review/results/report.json` y las capturas en el mismo directorio, excluido de Git. El script falla si detecta errores de ejecución, peticiones API sin respuesta de prueba configurada o desbordamiento horizontal.

Capturas representativas generadas:

- [Objetivos en celular, tema claro](../tmp/visual-review/results/390-tema-claro.png).
- [Visor de fotos en celular](../tmp/visual-review/results/390-visor.png).
- [Menú compacto a 320 píxeles](../tmp/visual-review/results/320-menu.png).
- [Calendario a 320 píxeles](../tmp/visual-review/results/320-calendario.png).

## Comprobaciones adicionales

- **106 pruebas del frontend correctas**, en 47 archivos.
- Compilación de producción correcta.
- **0 alertas de npm**, incluyendo Playwright, que solo se añadió como dependencia de desarrollo.
- Se mantienen las advertencias de tamaño del paquete inicial y de algunos estilos. La optimización de galerías grandes y de la carga inicial corresponde a la próxima etapa.

No se modificaron servicios del backend ni migraciones en esta fase. La revisión en Safari, Firefox, dispositivos físicos y con una sesión conectada al backend real sigue pendiente.
