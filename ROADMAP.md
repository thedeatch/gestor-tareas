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
- [ ] Panel modular y navegación adaptable.
- [ ] Configuración para activar o desactivar módulos.

## Hito de separación

Cuando los dos puntos pendientes estén completos:

1. Validar toda la aplicación y crear `template-v1.0`.
2. Crear la rama estable `template/base-v1`.
3. Separar un repositorio `fullstack-starter-angular-spring`.
4. Mantener en el starter un módulo mínimo de demostración.
5. Continuar el gestor de tareas como producto independiente.

## Fuera del núcleo

Chat, rankings, eventos, galerías, Kanban, calendario, publicaciones y comentarios serán módulos de aplicaciones construidas sobre la plantilla; no bloquean el hito `template-v1.0`.
