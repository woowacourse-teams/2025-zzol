import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { describe, expect, it } from 'vitest';
import { WsCatalogSchema } from '../types.js';

const HERE = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(HERE, '../../../../..'); // src/catalog/__tests__ → repo root
const FIXTURE =
  process.env.WS_CATALOG_FIXTURE ??
  resolve(REPO_ROOT, 'backend/app/src/test/resources/__fixtures__/ws-catalog.json');

describe('contract drift', () => {
  it('BE /dev/ws-catalog 응답 fixture 가 zod 스키마와 일치한다', async () => {
    let raw: string;
    try {
      raw = await readFile(FIXTURE, 'utf-8');
    } catch (err) {
      throw new Error(
        `fixture 파일을 읽을 수 없습니다: ${FIXTURE}\n${err instanceof Error ? err.message : String(err)}`,
        { cause: err }
      );
    }
    const json = JSON.parse(raw);
    const parsed = WsCatalogSchema.safeParse(json);
    if (!parsed.success) {
      const formatted = JSON.stringify(parsed.error.format(), null, 2);
      throw new Error(`fixture 가 WsCatalog zod 스키마와 다릅니다:\n${formatted}`);
    }
    expect(parsed.data.envelope.type).toMatch(/^WebSocketResponse</);
    expect(parsed.data.errors.topic).toBe('/queue/errors');
  });
});
