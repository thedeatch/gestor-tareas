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

Cuando los dos puntos pendientes estén completos:

1. Validar toda la aplicación y crear `template-v1.0`.
2. Crear la rama estable `template/base-v1`.
3. Separar un repositorio `fullstack-starter-angular-spring`.
4. Mantener en el starter un módulo mínimo de demostración.
5. Continuar el gestor de tareas como producto independiente.

## Fuera del núcleo

Chat, rankings, eventos, galerías, Kanban, calendario, publicaciones y comentarios serán módulos de aplicaciones construidas sobre la plantilla; no bloquean el hito `template-v1.0`.

## Próximas funciones de la comunidad

- [x] Encuestas del grupo con opciones, un voto por miembro, resultados y cierre opcional.
- [ ] Recuerdos con fecha de referencia opcional en opciones avanzadas. Usar la fecha de subida como valor predeterminado y permitir indicar la fecha o el año real en que se tomó la foto. Calcular los aniversarios desde esa referencia, por ejemplo, 2016 a 2027 corresponde a 11 años.
- [ ] Elementos guardados para recuperar fotos, publicaciones y eventos.
- [ ] Búsqueda conjunta dentro del grupo.
- [ ] Objetivos compartidos vinculados con tareas y progreso.
