# Hoja de ruta de la base reutilizable

## Base transversal

- [x] Autenticación y recuperación de contraseña.
- [x] Perfiles y avatares.
- [x] Roles y permisos RBAC.
- [x] Administración básica de usuarios.
- [x] Estados de cuenta y auditoría administrativa.
- [x] Manejo uniforme de errores.
- [x] Sistema reutilizable de archivos.
- [x] Configuración y preferencias básicas.
- [x] Refresh tokens, sesiones y cierre remoto.
- [x] Pruebas, Docker, CI/CD y documentación.
- [x] Panel modular y navegación adaptable.
- [x] Configuración para activar o desactivar módulos.

## Hito de separación

Hito aplazado mientras se continúa mejorando el producto. Cuando se retome y se complete la validación de la base:

1. Validar toda la aplicación y crear `template-v1.0`.
2. Crear la rama estable `template/base-v1`.
3. Separar un repositorio `fullstack-starter-angular-spring`.
4. Mantener en el starter un módulo mínimo de demostración.
5. Continuar el gestor de tareas como producto independiente.

## Fuera del núcleo

Chat, rankings, eventos, galerías, Kanban, calendario, publicaciones y comentarios serán módulos de aplicaciones construidas sobre la plantilla; no bloquean el hito `template-v1.0`.

## Próximas funciones de la comunidad

- [x] Encuestas del grupo con opciones, un voto por miembro, resultados y cierre opcional.
- [x] Recuerdos con fecha de referencia opcional en opciones avanzadas. Usar la fecha de subida como valor predeterminado y permitir indicar la fecha o el año real en que se tomó la foto. Calcular los aniversarios desde esa referencia, por ejemplo, 2016 a 2027 corresponde a 11 años.
- [x] Guardados personales de fotos y publicaciones, con filtros y acceso al contenido original.
- [x] Ampliar los guardados a eventos, con marcador personal, filtro y acceso al evento original, independiente de la inscripción.
- [x] Búsqueda conjunta de publicaciones, tareas, eventos y álbumes del grupo, con filtros y acceso al resultado.
- [x] Objetivos compartidos vinculados con tareas y progreso. Creación y edición por administradores y moderadores, fecha límite opcional y avance calculado desde el estado real de las tareas.

## Revisión y mejoras por etapas

- [x] Revisión técnica transversal inicial, correcciones de permisos de creación de tareas, renovación de sesiones, navegación y avatares. Informe: [revisión del 19 de noviembre de 2026](docs/revision-tecnica-2026-11-19.md).
- [x] Carga bajo demanda de los paneles del grupo y cancelación de descargas de álbumes que dejan de utilizarse.
- [x] Actualizar dependencias y resolver las alertas de auditoría con pruebas de regresión. Angular/Material 22.3.0, Spring Boot 4.1.2 y AWS S3 2.57.13. npm sin alertas y consulta OSV de las dependencias Maven de ejecución sin hallazgos al 19-11-2026.
- [x] Revisar la interfaz en escritorio y tamaños de celular con Chrome aislado y respuestas simuladas: 108 capturas y 40 comprobaciones. Informe: [revisión visual](docs/revision-visual-2026-11-19.md).
- [ ] Completar los recorridos con una sesión conectada al backend real y dispositivos físicos; ampliar a Safari y Firefox.
- [x] Optimizar la galería del grupo con miniaturas reutilizables, cargas por proximidad, cola de cuatro solicitudes y bloques de fotos; descargar originales solo al abrirlos.
- [x] Reducir el paquete inicial mediante carga diferida de la barra: de 573,18 kB a 354,58 kB. Informe: [optimización](docs/optimizacion-galerias-carga-2026-11-19.md).
- [ ] Paginar los metadatos de imágenes y los listados grandes en el servidor; optimizar recuerdos y previews de Guardados.
- [ ] Actualizar el informe general de funciones.
- [ ] Evaluar tareas recurrentes.
- [ ] Evaluar menciones a usuarios en comentarios.
- [ ] Retomar la versión estable más adelante, según lo acordado con el usuario.
