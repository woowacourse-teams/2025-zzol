import { tmpdir } from "node:os";
import { join } from "node:path";
import { mkdtemp, rm } from "node:fs/promises";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { CatalogCache } from "../cache.js";
import { CatalogFetcher } from "../fetch.js";

const VALID_CATALOG = {
  stompEndpoint: "/ws",
  app: "/app",
  topicPrefix: "/topic",
  queuePrefix: "/queue",
  envelope: {
    type: "WebSocketResponse<T>",
    fields: [
      { name: "success", type: "boolean" },
      { name: "data", type: "T" },
      { name: "errorMessage", type: "String" },
      { name: "id", type: "String" },
    ],
    note: "envelope",
  },
  topics: [],
  queues: [],
  sends: [],
  schemas: {},
  errors: { topic: "/queue/errors", payloadType: "WebSocketResponse<String>" },
};

let cacheDir: string;

beforeEach(async () => {
  cacheDir = await mkdtemp(join(tmpdir(), "ws-mcp-test-"));
});

afterEach(async () => {
  await rm(cacheDir, { recursive: true, force: true });
});

function makeCache(): CatalogCache {
  return new CatalogCache(join(cacheDir, "catalog.json"));
}

describe("CatalogFetcher", () => {
  it("성공 응답을 zod 로 파싱하고 캐시에 저장한다", async () => {
    const fetchImpl = (async () =>
      new Response(JSON.stringify(VALID_CATALOG), {
        status: 200,
        headers: { "content-type": "application/json", etag: "W/\"abc\"" },
      })) as unknown as typeof fetch;
    const fetcher = new CatalogFetcher({
      url: "http://localhost:8080/dev/ws-catalog",
      cache: makeCache(),
      fetchImpl,
    });

    const result = await fetcher.load();

    expect(result.source).toBe("live");
    expect(result.catalog.envelope.type).toBe("WebSocketResponse<T>");
  });

  it("HTTP 실패 시 캐시가 있으면 cache 로 폴백한다", async () => {
    const cache = makeCache();
    await cache.write({
      body: VALID_CATALOG,
      etag: null,
      lastModified: null,
      fetchedAt: "2026-05-15T00:00:00.000Z",
    });
    const fetchImpl = (async () =>
      new Response("server error", { status: 500 })) as unknown as typeof fetch;
    const fetcher = new CatalogFetcher({
      url: "http://localhost:8080/dev/ws-catalog",
      cache,
      fetchImpl,
    });

    const result = await fetcher.load();

    expect(result.source).toBe("cache");
    expect(result.catalog.envelope.type).toBe("WebSocketResponse<T>");
  });

  it("HTTP 실패 + 캐시도 없으면 명확한 에러를 던진다", async () => {
    const fetchImpl = (async () => {
      throw new Error("ECONNREFUSED");
    }) as unknown as typeof fetch;
    const fetcher = new CatalogFetcher({
      url: "http://localhost:8080/dev/ws-catalog",
      cache: makeCache(),
      fetchImpl,
    });

    await expect(fetcher.load()).rejects.toThrow(/캐시도 비어 있습니다/);
  });

  it("304 Not Modified 응답이면 캐시를 그대로 사용한다", async () => {
    const cache = makeCache();
    await cache.write({
      body: VALID_CATALOG,
      etag: "W/\"abc\"",
      lastModified: null,
      fetchedAt: "2026-05-15T00:00:00.000Z",
    });
    const fetchImpl = (async () => new Response(null, { status: 304 })) as unknown as typeof fetch;
    const fetcher = new CatalogFetcher({
      url: "http://localhost:8080/dev/ws-catalog",
      cache,
      fetchImpl,
    });

    const result = await fetcher.load();

    expect(result.source).toBe("cache");
  });
});
