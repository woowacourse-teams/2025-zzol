import { mkdir, readFile, writeFile } from "node:fs/promises";
import { homedir } from "node:os";
import { dirname, join } from "node:path";

export type CachedCatalog = {
  body: unknown;
  etag: string | null;
  lastModified: string | null;
  fetchedAt: string;
};

const DEFAULT_CACHE_PATH = join(homedir(), ".zzol-mcp", "catalog.json");

export class CatalogCache {
  constructor(private readonly path: string = DEFAULT_CACHE_PATH) {}

  async read(): Promise<CachedCatalog | null> {
    try {
      const raw = await readFile(this.path, "utf-8");
      return JSON.parse(raw) as CachedCatalog;
    } catch (error) {
      if (isNotFound(error)) {
        return null;
      }
      throw error;
    }
  }

  async write(value: CachedCatalog): Promise<void> {
    await mkdir(dirname(this.path), { recursive: true });
    await writeFile(this.path, `${JSON.stringify(value, null, 2)}\n`, "utf-8");
  }
}

function isNotFound(error: unknown): boolean {
  return (
    typeof error === "object" &&
    error !== null &&
    "code" in error &&
    (error as { code: string }).code === "ENOENT"
  );
}
