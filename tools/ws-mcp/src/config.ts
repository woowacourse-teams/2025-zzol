export interface ServerConfig {
  catalogUrl: string;
  brokerUrl: string;
  cachePath: string | undefined;
}

export function loadConfig(): ServerConfig {
  const catalogUrl = process.env.WS_MCP_CATALOG_URL ?? 'http://localhost:8080/dev/ws-catalog';
  return {
    catalogUrl,
    brokerUrl: process.env.WS_MCP_BROKER_URL ?? deriveBrokerUrl(catalogUrl),
    cachePath: process.env.WS_MCP_CACHE_PATH,
  };
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
