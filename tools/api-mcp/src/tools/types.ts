import type { CatalogFetcher } from '../catalog/fetch.js';
import type { OpenApiFetcher } from '../openapi/fetch.js';

export interface ToolContext {
  catalog: CatalogFetcher;
  brokerUrl: string;
  openapi: OpenApiFetcher;
  baseUrl: string;
}

export interface ToolDefinition {
  name: string;
  description: string;
  inputSchema: object;
  handler: (args: Record<string, unknown>, ctx: ToolContext) => Promise<ToolResult>;
}

export interface ToolResult {
  content: { type: 'text'; text: string }[];
  isError?: boolean;
}

export function ok(payload: unknown): ToolResult {
  return {
    content: [{ type: 'text', text: JSON.stringify(payload, null, 2) }],
  };
}

export function fail(message: string): ToolResult {
  return {
    content: [{ type: 'text', text: message }],
    isError: true,
  };
}

export function tryParseJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}
