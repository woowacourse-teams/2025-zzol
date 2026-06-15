export interface ServerConfig {
  catalogUrl: string;
  brokerUrl: string;
  cachePath: string | undefined;
  baseUrl: string;
}

export function loadConfig(): ServerConfig {
  const catalogUrl = process.env.API_MCP_CATALOG_URL ?? 'http://localhost:8080/dev/ws-catalog';
  const baseUrl = process.env.API_MCP_BASE_URL ?? deriveBaseUrl(catalogUrl);
  return {
    catalogUrl,
    brokerUrl: process.env.API_MCP_BROKER_URL ?? deriveBrokerUrl(catalogUrl),
    cachePath: process.env.API_MCP_CACHE_PATH,
    baseUrl,
  };
}

function deriveBaseUrl(catalogUrl: string): string {
  const url = new URL(catalogUrl);
  return `${url.protocol}//${url.host}`;
}

function deriveBrokerUrl(catalogUrl: string): string {
  const url = new URL(catalogUrl);
  const wsProtocol =
    url.protocol === 'https:'
      ? 'wss:'
      : url.protocol === 'http:'
        ? 'ws:'
        : (() => {
            throw new Error(
              `지원하지 않는 프로토콜입니다: '${url.protocol}' (http: 또는 https: 만 허용)`
            );
          })();
  return `${wsProtocol}//${url.host}/ws`;
}
