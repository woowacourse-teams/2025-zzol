import { tmpdir } from "node:os";
import { join } from "node:path";
import { mkdtemp, rm } from "node:fs/promises";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { CatalogCache } from "../cache.js";

let cacheDir: string;

beforeEach(async () => {
  cacheDir = await mkdtemp(join(tmpdir(), "ws-mcp-cache-"));
});

afterEach(async () => {
  await rm(cacheDir, { recursive: true, force: true });
});

describe("CatalogCache", () => {
  it("파일이 없으면 null 을 반환한다", async () => {
    const cache = new CatalogCache(join(cacheDir, "catalog.json"));
    expect(await cache.read()).toBeNull();
  });

  it("write 한 값을 그대로 read 한다", async () => {
    const cache = new CatalogCache(join(cacheDir, "catalog.json"));
    const value = {
      body: { hello: "world" },
      etag: "W/\"x\"",
      lastModified: "Wed, 15 May 2026 00:00:00 GMT",
      fetchedAt: "2026-05-15T00:00:00.000Z",
    };

    await cache.write(value);

    expect(await cache.read()).toEqual(value);
  });

  it("부모 디렉토리가 없어도 자동 생성한다", async () => {
    const cache = new CatalogCache(join(cacheDir, "nested", "dir", "catalog.json"));
    await cache.write({ body: {}, etag: null, lastModified: null, fetchedAt: "now" });
    expect(await cache.read()).not.toBeNull();
  });
});
