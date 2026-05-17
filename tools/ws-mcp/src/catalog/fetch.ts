import { CatalogCache, type CachedCatalog } from './cache.js';
import { WsCatalogSchema, type WsCatalog } from './types.js';

export type CatalogSource = 'live' | 'cache';

export interface CatalogResult {
  catalog: WsCatalog;
  source: CatalogSource;
  fetchedAt: string;
  fallbackReason?: string;
}

export interface CatalogFetcherOptions {
  url: string;
  cache?: CatalogCache;
  fetchImpl?: typeof fetch;
  timeoutMs?: number;
}

export class CatalogFetcher {
  private readonly url: string;
  private readonly cache: CatalogCache;
  private readonly fetchImpl: typeof fetch;
  private readonly timeoutMs: number;

  constructor(options: CatalogFetcherOptions) {
    this.url = options.url;
    this.cache = options.cache ?? new CatalogCache();
    this.fetchImpl = options.fetchImpl ?? fetch;
    this.timeoutMs = options.timeoutMs ?? 10_000;
  }

  async load(): Promise<CatalogResult> {
    let previous: CachedCatalog | null = null;
    try {
      previous = await this.cache.read();
    } catch (error) {
      process.stderr.write(
        `catalog cache 손상 — live fetch 로 폴백합니다: ${String(error)}\n`
      );
    }

    const headers: Record<string, string> = { accept: 'application/json' };
    if (previous?.etag) {
      headers['if-none-match'] = previous.etag;
    }
    if (previous?.lastModified) {
      headers['if-modified-since'] = previous.lastModified;
    }

    const controller = new AbortController();
    const timeoutId = setTimeout(() => {
      controller.abort();
    }, this.timeoutMs);
    try {
      const response = await this.fetchImpl(this.url, { headers, signal: controller.signal });

      if (response.status === 304 && previous) {
        return toResult(previous, 'cache');
      }
      if (!response.ok) {
        return this.fallback(previous, `HTTP ${String(response.status)} ${response.statusText}`);
      }

      const body: unknown = await response.json();
      const catalog = WsCatalogSchema.parse(body);
      const cached: CachedCatalog = {
        body,
        etag: response.headers.get('etag'),
        lastModified: response.headers.get('last-modified'),
        fetchedAt: new Date().toISOString(),
      };
      await this.cache.write(cached);
      return { catalog, source: 'live', fetchedAt: cached.fetchedAt };
    } catch (error) {
      return this.fallback(previous, errorMessage(error));
    } finally {
      clearTimeout(timeoutId);
    }
  }

  private fallback(previous: CachedCatalog | null, reason: string): CatalogResult {
    if (!previous) {
      throw new Error(
        `카탈로그를 ${this.url} 에서 가져오지 못했고 캐시도 비어 있습니다. 원인: ${reason}`
      );
    }
    return { ...toResult(previous, 'cache'), fallbackReason: reason };
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
