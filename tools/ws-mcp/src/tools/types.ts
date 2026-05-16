import type { CatalogFetcher } from "../catalog/fetch.js";

export type ToolContext = {
  catalog: CatalogFetcher;
  brokerUrl: string;
};

export type ToolDefinition = {
  name: string;
  description: string;
  inputSchema: object;
  handler: (args: Record<string, unknown>, ctx: ToolContext) => Promise<ToolResult>;
};

export type ToolResult = {
  content: Array<{ type: "text"; text: string }>;
  isError?: boolean;
};

export function ok(payload: unknown): ToolResult {
  return {
    content: [{ type: "text", text: JSON.stringify(payload, null, 2) }],
  };
}

export function fail(message: string): ToolResult {
  return {
    content: [{ type: "text", text: message }],
    isError: true,
  };
}
