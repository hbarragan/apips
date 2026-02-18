import http from 'node:http';
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { PATHS, SERVER_CONSTANTS, APP_NAME, APP_VERSION } from '../config/constants.mjs';
import { RUNTIME_CONFIG } from '../config/runtime-config.mjs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const isBuildRuntime = __dirname.includes(`${path.sep}build${path.sep}`);
const staticDir = isBuildRuntime
  ? path.resolve(__dirname, '..', 'public')
  : PATHS.STATIC_SRC;

const CONTENT_TYPES = Object.freeze({
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.ico': 'image/x-icon'
});

const sendJson = (res, statusCode, payload) => {
  res.writeHead(statusCode, { 'content-type': CONTENT_TYPES['.json'] });
  res.end(JSON.stringify(payload));
};

const resolveStaticPath = (pathname) => {
  const cleanPath = pathname === '/' ? `/${SERVER_CONSTANTS.INDEX_FILE}` : pathname;
  const localPath = decodeURIComponent(cleanPath).replace(/\.\./g, '');
  return path.join(staticDir, localPath);
};

const serveStatic = async (pathname, res) => {
  try {
    const filePath = resolveStaticPath(pathname);
    const stat = await fs.stat(filePath);
    if (stat.isDirectory()) {
      const indexPath = path.join(filePath, SERVER_CONSTANTS.INDEX_FILE);
      const buffer = await fs.readFile(indexPath);
      res.writeHead(200, { 'content-type': CONTENT_TYPES['.html'] });
      res.end(buffer);
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = CONTENT_TYPES[ext] || 'application/octet-stream';
    const buffer = await fs.readFile(filePath);
    res.writeHead(200, { 'content-type': contentType });
    res.end(buffer);
  } catch {
    try {
      const indexPath = path.join(staticDir, SERVER_CONSTANTS.INDEX_FILE);
      const buffer = await fs.readFile(indexPath);
      res.writeHead(200, { 'content-type': CONTENT_TYPES['.html'] });
      res.end(buffer);
    } catch {
      sendJson(res, 404, { code: 'NOT_FOUND', message: 'Resource not found' });
    }
  }
};

const proxyRecipes = async (requestUrl, res) => {
  try {
    const proxyUrl = new URL(RUNTIME_CONFIG.ODATA_BASE_URL);
    requestUrl.searchParams.forEach((value, key) => {
      proxyUrl.searchParams.set(key, value);
    });

    const response = await fetch(proxyUrl, {
      headers: { Accept: 'application/json;odata.metadata=minimal' }
    });

    const body = await response.text();
    res.writeHead(response.status, {
      'content-type': response.headers.get('content-type') || CONTENT_TYPES['.json']
    });
    res.end(body);
  } catch (error) {
    sendJson(res, 500, {
      code: 'NODE_PROXY_ERROR',
      message: 'Error while requesting OData endpoint.',
      detail: error instanceof Error ? error.message : String(error)
    });
  }
};

const server = http.createServer(async (req, res) => {
  const requestUrl = new URL(req.url || '/', `http://${req.headers.host}`);

  if (req.method === 'GET' && requestUrl.pathname === SERVER_CONSTANTS.HEALTH_PATH) {
    sendJson(res, 200, {
      ok: true,
      app: APP_NAME,
      version: APP_VERSION,
      mode: isBuildRuntime ? 'build' : 'source'
    });
    return;
  }

  if (req.method === 'GET' && requestUrl.pathname === SERVER_CONSTANTS.ODATA_PROXY_ROUTE) {
    await proxyRecipes(requestUrl, res);
    return;
  }

  if (req.method === 'GET') {
    await serveStatic(requestUrl.pathname, res);
    return;
  }

  sendJson(res, 405, { code: 'METHOD_NOT_ALLOWED', message: 'Only GET is supported.' });
});

server.listen(RUNTIME_CONFIG.PORT, RUNTIME_CONFIG.HOST, () => {
  console.log(`[${APP_NAME}] running on http://${RUNTIME_CONFIG.HOST}:${RUNTIME_CONFIG.PORT}`);
  console.log(`[${APP_NAME}] using OData base URL: ${RUNTIME_CONFIG.ODATA_BASE_URL}`);
});
