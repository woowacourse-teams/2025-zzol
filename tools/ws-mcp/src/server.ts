#!/usr/bin/env node
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { CallToolRequestSchema, ListToolsRequestSchema } from "@modelcontextprotocol/sdk/types.js";
import { CatalogFetcher } from "./catalog/fetch.js";
import { wsConnectTool } from "./tools/connect.js";
import { wsDescribeTool } from "./tools/describe.js";
import { wsListTopicsTool } from "./tools/list-topics.js";
import { wsSendTool } from "./tools/send.js";
import { wsSourceTool } from "./tools/source.js";
import { wsSubscribeTool } from "./tools/subscribe.js";
import type { ToolContext, ToolDefinition } from "./tools/types.js";

const TOOLS: ToolDefinition[] = [
  wsDescribeTool,
  wsListTopicsTool,
  wsSourceTool,
  wsConnectTool,
  wsSubscribeTool,
  wsSendTool,
];

async function main(): Promise<void> {
  const catalogUrl = process.env.WS_MCP_CATALOG_URL ?? "http://localhost:8080/dev/ws-catalog";
  const brokerUrl = process.env.WS_MCP_BROKER_URL ?? deriveBrokerUrl(catalogUrl);

  const context: ToolContext = {
    catalog: new CatalogFetcher({ url: catalogUrl }),
    brokerUrl,
  };

  const server = new Server(
    { name: "ws-mcp", version: "0.1.0" },
    { capabilities: { tools: {} } },
  );

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
        content: [{ type: "text", text: `unknown tool: ${request.params.name}` }],
        isError: true,
      };
    }
    try {
      return await tool.handler((request.params.arguments ?? {}) as Record<string, unknown>, context);
    } catch (error) {
      return {
        content: [
          { type: "text", text: error instanceof Error ? error.message : String(error) },
        ],
        isError: true,
      };
    }
  });

  const transport = new StdioServerTransport();
  await server.connect(transport);
}

function deriveBrokerUrl(catalogUrl: string): string {
  const url = new URL(catalogUrl);
  const wsProtocol = url.protocol === "https:" ? "wss:" : "ws:";
  return `${wsProtocol}//${url.host}/ws`;
}

main().catch((error: unknown) => {
  process.stderr.write(`ws-mcp 시작 실패: ${error instanceof Error ? error.message : String(error)}\n`);
  process.exit(1);
});
