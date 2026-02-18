import fs from 'node:fs/promises';
import path from 'node:path';
import { PATHS, DIR_NAMES } from '../src/config/constants.mjs';

const copyDirectory = async (from, to) => {
  await fs.mkdir(to, { recursive: true });
  await fs.cp(from, to, { recursive: true });
};

const cleanBuild = async () => {
  await fs.rm(PATHS.BUILD_ROOT, { recursive: true, force: true });
  await fs.mkdir(PATHS.BUILD_ROOT, { recursive: true });
};

const build = async () => {
  console.log('[build] cleaning build folder...');
  await cleanBuild();

  console.log('[build] copying static assets...');
  await copyDirectory(PATHS.STATIC_SRC, PATHS.BUILD_PUBLIC);

  console.log('[build] copying server files...');
  await copyDirectory(PATHS.SERVER_SRC, PATHS.BUILD_SERVER);

  console.log('[build] copying config files...');
  await fs.mkdir(path.join(PATHS.BUILD_ROOT, 'config'), { recursive: true });
  await fs.cp(path.join(PATHS.SRC_ROOT, 'config'), path.join(PATHS.BUILD_ROOT, 'config'), { recursive: true });

  console.log(`[build] done. Artifact: ${path.join(PATHS.PROJECT_ROOT, DIR_NAMES.BUILD)}`);
};

build().catch((error) => {
  console.error('[build] failed:', error);
  process.exitCode = 1;
});
