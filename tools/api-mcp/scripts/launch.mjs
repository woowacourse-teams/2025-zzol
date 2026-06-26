#!/usr/bin/env node
/*
 * api-mcp self-healing launcher.
 *
 * `.mcp.json` 이 빌드 산출물(dist/server.js)을 직접 가리키면, node_modules·dist 가
 * gitignore 되어 있어 클론/클린마다 사라져 "Connection closed" 로 죽는다.
 * 이 런처는 실행 시점에 의존성 설치·빌드를 보장한 뒤 서버를 같은 프로세스에서
 * import 로 띄워 stdio(JSON-RPC) 를 그대로 넘긴다.
 *
 * ★ stdout 규율: 이 파일과 npm 자식 프로세스는 절대 stdout 에 쓰지 않는다.
 *   npm 출력은 fd 2(stderr) 로 돌린다. stdout 으로 새면 MCP JSON-RPC 가 깨진다.
 */
import { existsSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { dirname, join } from 'node:path';

const root = dirname(dirname(fileURLToPath(import.meta.url)));

function npm(args) {
  // 자식 stdout -> 부모 fd 2(stderr). stdin 무시, stderr 상속.
  const r = spawnSync('npm', args, {
    cwd: root,
    stdio: ['ignore', 2, 'inherit'],
    shell: true,
  });
  if (r.status !== 0) {
    process.stderr.write(`api-mcp launcher: 'npm ${args.join(' ')}' 실패 (exit ${r.status})\n`);
    process.exit(r.status ?? 1);
  }
}

const depMarker = join(root, 'node_modules', '@modelcontextprotocol');
const distEntry = join(root, 'dist', 'server.js');

// node_modules 가 비었거나 부분 설치된 상태(@modelcontextprotocol 부재)면 재설치.
// npm ci/install 의 `prepare` 훅이 build 까지 돌려 dist 도 함께 생성된다.
if (!existsSync(depMarker)) {
  npm(existsSync(join(root, 'package-lock.json')) ? ['ci'] : ['install']);
}
// deps 는 있는데 dist 만 없는 경우(예: 빌드만 날아감) 대비.
if (!existsSync(distEntry)) {
  npm(['run', 'build']);
}

await import(pathToFileURL(distEntry).href);
