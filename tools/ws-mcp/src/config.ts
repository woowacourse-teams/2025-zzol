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
  const wsProtocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${wsProtocol}//${url.host}/ws`;
}
