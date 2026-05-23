import type { CatalogCache } from '../catalog/cache.js';
import { OpenApiSpecSchema, type OpenApiSpec } from './types.js';

export interface OpenApiFetcherOptions {
  baseUrl: string;
  cache?: CatalogCache;
}

interface FetchState {
  etag: string | null;
  lastModified: string | null;
}

export class OpenApiFetcher {
  private readonly url: string;
  private readonly cache: CatalogCache | undefined;
  private state: FetchState = { etag: null, lastModified: null };
  private cached: OpenApiSpec | null = null;

  constructor({ baseUrl, cache }: OpenApiFetcherOptions) {
    this.url = `${baseUrl.replace(/\/$/, '')}/v3/api-docs`;
    this.cache = cache;
  }

  async load(): Promise<OpenApiSpec> {
    const headers: Record<string, string> = {};
    if (this.state.etag != null) headers['If-None-Match'] = this.state.etag;
    if (this.state.lastModified != null) headers['If-Modified-Since'] = this.state.lastModified;

    try {
      const res = await fetch(this.url, {
        headers,
        signal: AbortSignal.timeout(10_000),
      });

      if (res.status === 304 && this.cached) return this.cached;

      if (!res.ok) throw new Error(`OpenAPI spec 로드 실패: HTTP ${String(res.status)}`);

      const raw: unknown = await res.json();
      const spec = OpenApiSpecSchema.parse(raw);

      this.cached = spec;
      this.state = {
        etag: res.headers.get('etag'),
        lastModified: res.headers.get('last-modified'),
      };

      if (this.cache) {
        await this.cache.write({
          body: raw,
          etag: this.state.etag,
          lastModified: this.state.lastModified,
          fetchedAt: new Date().toISOString(),
        });
      }

      return spec;
    } catch (error) {
      if (this.cached) return this.cached;

      if (this.cache) {
        const disk = await this.cache.read();
        if (disk) {
          const spec = OpenApiSpecSchema.parse(disk.body);
          this.cached = spec;
          this.state = { etag: disk.etag, lastModified: disk.lastModified };
          return spec;
        }
      }

      throw error;
    }
  }

  getUrl(): string {
    return this.url;
  }
}
