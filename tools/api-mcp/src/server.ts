#!/usr/bin/env node
/* eslint-disable @typescript-eslint/require-await --
 * MCP SDK 의 Server / setRequestHandler 시그니처를 직접 사용하는 thin shim.
 * McpServer 마이그레이션은 별도 후속 작업, 핸들러는 SDK 가 async 콜백을 요구한다. */
import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { CallToolRequestSchema, ListToolsRequestSchema } from '@modelcontextprotocol/sdk/types.js';
import { CatalogCache } from './catalog/cache.js';
import { CatalogFetcher } from './catalog/fetch.js';
import { OpenApiFetcher } from './openapi/fetch.js';
import { wsConnectTool } from './tools/connect.js';
import { wsDescribeTool } from './tools/describe.js';
import { wsListTopicsTool } from './tools/list-topics.js';
import { wsSendTool } from './tools/send.js';
import { wsSourceTool } from './tools/source.js';
import { wsSubscribeTool } from './tools/subscribe.js';
import { httpRequestTool } from './tools/http-request.js';
import { httpListEndpointsTool } from './tools/http-list-endpoints.js';
import { httpDescribeTool } from './tools/http-describe.js';
import { httpValidateTool } from './tools/http-validate.js';
import { loadConfig } from './config.js';
import type { ToolContext, ToolDefinition } from './tools/types.js';

const TOOLS: ToolDefinition[] = [
  // WebSocket / STOMP
  wsDescribeTool,
  wsListTopicsTool,
  wsSourceTool,
  wsConnectTool,
  wsSubscribeTool,
  wsSendTool,
  // HTTP / REST
  httpRequestTool,
  httpListEndpointsTool,
  httpDescribeTool,
  httpValidateTool,
];

async function main(): Promise<void> {
  const { catalogUrl, brokerUrl, cachePath, baseUrl } = loadConfig();

  const openApiCachePath = cachePath ? cachePath.replace(/\.json$/, '-openapi.json') : undefined;

  const context: ToolContext = {
    catalog: new CatalogFetcher({
      url: catalogUrl,
      ...(cachePath ? { cache: new CatalogCache(cachePath) } : {}),
    }),
    brokerUrl,
    openapi: new OpenApiFetcher({
      baseUrl,
      ...(openApiCachePath ? { cache: new CatalogCache(openApiCachePath) } : {}),
    }),
    baseUrl,
  };

  // eslint-disable-next-line @typescript-eslint/no-deprecated -- 위 thin shim 주석 참조. McpServer 마이그레이션 전까지 저수준 Server 사용 유지.
  const server = new Server({ name: 'api-mcp', version: '0.1.0' }, { capabilities: { tools: {} } });

  server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: TOOLS.map((t) => ({
      name: t.name,
      description: t.description,
      inputSchema: t.inputSchema,
    })),
  }));

  server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const tool = TOOLS.find((t) => t.name === request.params.name);
    if (!tool) {
      return {
        content: [{ type: 'text', text: `unknown tool: ${request.params.name}` }],
        isError: true,
      };
    }
    try {
      return (await tool.handler(request.params.arguments ?? {}, context)) as never;
    } catch (error) {
      return {
        content: [{ type: 'text', text: error instanceof Error ? error.message : String(error) }],
        isError: true,
      };
    }
  });

  const transport = new StdioServerTransport();
  await server.connect(transport);
}

main().catch((error: unknown) => {
  process.stderr.write(
    `api-mcp 시작 실패: ${error instanceof Error ? error.message : String(error)}\n`
  );
  process.exit(1);
});
