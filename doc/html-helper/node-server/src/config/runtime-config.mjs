import { DEFAULTS } from './constants.mjs';

const parseBoolean = (value, fallback) => {
  if (value === undefined || value === null || value === '') return fallback;
  const normalized = String(value).trim().toLowerCase();
  if (['true', '1', 'yes', 'y'].includes(normalized)) return true;
  if (['false', '0', 'no', 'n'].includes(normalized)) return false;
  return fallback;
};

const parseNumber = (value, fallback) => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
};

export const RUNTIME_CONFIG = Object.freeze({
  PORT: parseNumber(process.env.NODE_SERVER_PORT, DEFAULTS.PORT),
  HOST: process.env.NODE_SERVER_HOST || DEFAULTS.HOST,
  ODATA_BASE_URL: process.env.ODATA_RECIPES_URL || DEFAULTS.ODATA_BASE_URL,
  DEFAULT_TOP: parseNumber(process.env.ODATA_DEFAULT_TOP, DEFAULTS.TOP),
  DEFAULT_SKIP: parseNumber(process.env.ODATA_DEFAULT_SKIP, DEFAULTS.SKIP),
  DEFAULT_COUNT: parseBoolean(process.env.ODATA_DEFAULT_COUNT, DEFAULTS.COUNT)
});
