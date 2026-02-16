# Release Notes

Listado curado y cronológico de cambios relevantes por versión.

## [1.1.1.1] - 2026-01-13

### Added
- PFIPSAPI-48: generación de “how to install” y activación de endpoints de workflow.
- PFIPSAPI-25: endpoint de *transaction history* (normal y paginado).
- PFIPSAPI-25: endpoint de detalle de pedido (*order detail*).
- Añadido `.gitignore`.

### Changed
- PFIPSAPI-44: refactor del proceso de instalación/paquetización.
- PFIPSAPI-44: unificación de `build.gradle` y tareas para empaquetado (una sola build/tarea).
- PFIPSAPI-44: nueva configuración de instalación/paquete.
- PFIPSAPI-44: ajustes de despliegue (info en PS10).
- PFIPSAPI-44: cambios en copia/soporte Windows Server (instalación).
- fx-logs: limpieza y ajustes de logs + script de servicio.

### Fixed
- PFIPSAPI-44: correcciones en el *installer builder*.
- Corrección de descripción en el ejecutable de instalación.
- Correcciones relacionadas con “pool”.
- Correcciones de “autopackage”.
- PFIPSAPI-25: commits de “transaction history incomplete” (revisar si quedó completamente resuelto).

---

## [1.1.0.4] - 2025-12-22

### Changed
- Ajustes en propiedades para ignorar *subselect* en “order”.

### Fixed
- Correcciones en configuración/propiedades.

### Security
- Actualización de credenciales/contraseña en configuración (sin exponer valores).

---

## [1.1.0.3] - 2025-12-18

### Added
- PFIPSAPI-23: primera pasada de “clean code”.
- feature/integration-test: trabajo inicial de tests de integración (marcado como incompleto).

### Fixed
- PFIPSAPI-22: corrección de valor desconocido de unidad de medida (*unknown uom*).