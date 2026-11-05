# Guía para agregar módulos

Esta guía ayuda a ampliar el proyecto sin mezclar una funcionalidad de negocio con el núcleo reutilizable.

## 1. Definir el límite

Antes de programar, identifica:

- Qué entidad administra el módulo.
- Qué roles pueden verla o modificarla.
- Quién es propietario de cada registro.
- Qué acciones necesitan auditoría.
- Si requiere archivos, correo o preferencias.

## 2. Backend

Agrega, según corresponda:

```text
model/       Entidades y enumeraciones
repository/  Consultas de persistencia
dto/         Contratos de entrada y salida
service/     Reglas de negocio y autorización
controller/  Endpoints REST
```

Después:

1. Crea una migración incremental en `backend/src/main/resources/db/migration`.
2. Valida entradas con Jakarta Validation.
3. Devuelve DTO, no entidades JPA directamente.
4. Usa el manejador uniforme de errores.
5. Protege el endpoint en Spring Security y también en el servicio.
6. Documenta el contrato con OpenAPI.
7. Agrega pruebas de integración para éxito, validación y permisos.

## 3. Frontend

1. Crea una página o componente de la funcionalidad.
2. Define modelos TypeScript para solicitudes y respuestas.
3. Encapsula HTTP en un servicio.
4. Registra rutas y guards necesarios.
5. Agrega la navegación solo para usuarios autorizados.
6. Reutiliza diálogos, estados de carga y tratamiento de errores.
7. Comprueba tema claro, oscuro, accesibilidad y diseño adaptable.
8. Agrega pruebas de componentes y servicios.

## 4. Archivos y configuración

- Reutiliza el sistema general de archivos; no crees almacenamiento paralelo.
- Los secretos deben ser variables de entorno y figurar sin valores reales en `.env.example`.
- Una función opcional deberá poder activarse mediante configuración cuando se implemente el sistema de módulos del punto 12.

## 5. Lista de verificación

- [ ] Migración Flyway versionada.
- [ ] Permisos comprobados en backend.
- [ ] Errores uniformes y mensajes útiles.
- [ ] Pruebas frontend y backend.
- [ ] Builds exitosos.
- [ ] Imágenes Docker construidas.
- [ ] Documentación y `.env.example` actualizados.
- [ ] Prueba manual en escritorio y pantalla angosta.
