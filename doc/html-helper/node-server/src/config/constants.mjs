import path from 'node:path';
import { fileURLToPath } from 'node:url';

export const APP_NAME = 'OData Recipes Helper';
export const APP_VERSION = '1.0.0';

export const DEFAULTS = Object.freeze({
  PORT: 9301,
  HOST: '0.0.0.0',
  ODATA_BASE_URL: 'http://localhost:9102/odata/Recipes',
  TOP: 10,
  SKIP: 0,
  COUNT: true,
  ORDER_DIRECTION: 'asc'
});

export const DIR_NAMES = Object.freeze({
  STATIC: 'static',
  SERVER: 'server',
  BUILD: 'build',
  PUBLIC: 'public'
});

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export const PATHS = Object.freeze({
  SRC_ROOT: path.resolve(__dirname, '..'),
  STATIC_SRC: path.resolve(__dirname, '..', DIR_NAMES.STATIC),
  SERVER_SRC: path.resolve(__dirname, '..', DIR_NAMES.SERVER),
  PROJECT_ROOT: path.resolve(__dirname, '..', '..'),
  BUILD_ROOT: path.resolve(__dirname, '..', '..', DIR_NAMES.BUILD),
  BUILD_PUBLIC: path.resolve(__dirname, '..', '..', DIR_NAMES.BUILD, DIR_NAMES.PUBLIC),
  BUILD_SERVER: path.resolve(__dirname, '..', '..', DIR_NAMES.BUILD, DIR_NAMES.SERVER)
});

export const SERVER_CONSTANTS = Object.freeze({
  HEALTH_PATH: '/health',
  INDEX_FILE: 'index.html',
  STATIC_ROUTE: '/',
  ODATA_PROXY_ROUTE: '/api/recipes',
  JSON_LIMIT: '1mb'
});

export const UI_CONSTANTS = Object.freeze({
  PAGE_TITLE: 'Recipes OData Explorer',
  FETCH_TIMEOUT_MS: 15000,
  DATE_PLACEHOLDER: 'YYYY-MM-DDTHH:mm:ssZ'
});

export const ORDER_FIELDS = Object.freeze([
  'name',
  'description',
  'revision',
  'creationDate',
  'effectivityStartTime',
  'effectivityEndTime',
  'accessPrivilege'
]);
