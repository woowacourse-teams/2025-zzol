import { CatalogCache, type CachedCatalog } from "./cache.js";
import { WsCatalogSchema, type WsCatalog } from "./types.js";

export type CatalogSource = "live" | "cache";

export type CatalogResult = {
  catalog: WsCatalog;
  source: CatalogSource;
  fetchedAt: string;
};

export type CatalogFetcherOptions = {
  url: string;
  cache?: CatalogCache;
  fetchImpl?: typeof fetch;
};

export class CatalogFetcher {
  private readonly url: string;
  private readonly cache: CatalogCache;
  private readonly fetchImpl: typeof fetch;

  constructor(options: CatalogFetcherOptions) {
    this.url = options.url;
    this.cache = options.cache ?? new CatalogCache();
    this.fetchImpl = options.fetchImpl ?? fetch;
  }

  async load(): Promise<CatalogResult> {
    const previous = await this.cache.read();
    const headers: Record<string, string> = { accept: "application/json" };
    if (previous?.etag) {
      headers["if-none-match"] = previous.etag;
    }
    if (previous?.lastModified) {
      headers["if-modified-since"] = previous.lastModified;
    }

    try {
      const response = await this.fetchImpl(this.url, { headers });

      if (response.status === 304 && previous) {
        return toResult(previous, "cache");
      }
      if (!response.ok) {
        return await this.fallback(previous, `HTTP ${response.status} ${response.statusText}`);
      }

      const body = await response.json();
      const catalog = WsCatalogSchema.parse(body);
      const cached: CachedCatalog = {
        body,
        etag: response.headers.get("etag"),
        lastModified: response.headers.get("last-modified"),
        fetchedAt: new Date().toISOString(),
      };
      await this.cache.write(cached);
      return { catalog, source: "live", fetchedAt: cached.fetchedAt };
    } catch (error) {
      return await this.fallback(previous, errorMessage(error));
    }
  }

  private async fallback(previous: CachedCatalog | null, reason: string): Promise<CatalogResult> {
    if (!previous) {
      throw new Error(
        `카탈로그를 ${this.url} 에서 가져오지 못했고 캐시도 비어 있습니다. 원인: ${reason}`,
      );
    }
    return toResult(previous, "cache");
  }
}

function toResult(cached: CachedCatalog, source: CatalogSource): CatalogResult {
  return {
    catalog: WsCatalogSchema.parse(cached.body),
    source,
    fetchedAt: cached.fetchedAt,
  };
}

function errorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  return String(error);
}
