# Node Server - OData Recipes Helper

Aplicación Node.js para testear casuísticas OData de **Recipes** con una pantalla de filtros + ordenación.

## Objetivo
- Servir estáticos HTML/CSS/JS para testing funcional.
- Exponer el servidor en un puerto configurable.
- Consumir OData del proyecto principal (`/odata/Recipes`) vía proxy local.
- Incluir estáticos dentro del artefacto de `build`.

## Configuración por constantes
Todo está centralizado en:
- `src/config/constants.mjs`
- `src/config/runtime-config.mjs`

Variables de entorno soportadas:
- `NODE_SERVER_PORT` (default: `9301`)
- `NODE_SERVER_HOST` (default: `0.0.0.0`)
- `ODATA_RECIPES_URL` (default: `http://localhost:9102/odata/Recipes`)
- `ODATA_DEFAULT_TOP` (default: `10`)
- `ODATA_DEFAULT_SKIP` (default: `0`)
- `ODATA_DEFAULT_COUNT` (default: `true`)

## Scripts
```bash
npm install
npm run build
npm run start
```

### Build
`npm run build` genera:
- `build/public` (estáticos HTML/CSS/JS)
- `build/server` (backend Node HTTP)
- `build/config` (constantes/config runtime)

Ejecutar build:
```bash
npm run start:build
```

## Endpoints Node
- `GET /health`
- `GET /api/recipes?...` (proxy a OData)
- `GET /` (UI)

