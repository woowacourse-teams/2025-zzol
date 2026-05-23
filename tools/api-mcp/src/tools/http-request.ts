import { z } from 'zod';
import type { ToolDefinition, ToolContext } from './types.js';
import { ok, fail } from './types.js';

const InputSchema = z.object({
  method: z.enum(['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS']).default('GET'),
  path: z.string(),
  headers: z.record(z.string()).optional(),
  body: z.unknown().optional(),
  queryParams: z.record(z.string()).optional(),
});

export const httpRequestTool: ToolDefinition = {
  name: 'http_request',
  description: '백엔드 서버로 HTTP 요청을 보내고 { request, response } 형태의 JSON을 반환합니다.',
  inputSchema: {
    type: 'object',
    properties: {
      method: {
        type: 'string',
        enum: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS'],
        default: 'GET',
        description: 'HTTP 메서드',
      },
      path: { type: 'string', description: '경로 (예: /api/rooms)' },
      headers: {
        type: 'object',
        additionalProperties: { type: 'string' },
        description: '요청 헤더',
      },
      body: { description: '요청 바디 (JSON)' },
      queryParams: {
        type: 'object',
        additionalProperties: { type: 'string' },
        description: '쿼리 파라미터',
      },
    },
    required: ['path'],
  },
  async handler(args: Record<string, unknown>, ctx: ToolContext) {
    const parsed = InputSchema.safeParse(args);
    if (!parsed.success) return fail(parsed.error.message);

    const { method, path, headers = {}, body, queryParams } = parsed.data;

    const url = new URL(path, ctx.baseUrl);
    if (queryParams) {
      for (const [k, v] of Object.entries(queryParams)) url.searchParams.set(k, v);
    }

    const requestHeaders: Record<string, string> = {
      ...(body != null ? { 'Content-Type': 'application/json' } : {}),
      ...headers,
    };

    const startMs = Date.now();
    let response: Response;
    try {
      response = await fetch(url.toString(), {
        method,
        headers: requestHeaders,
        ...(body != null ? { body: JSON.stringify(body) } : {}),
        signal: AbortSignal.timeout(30_000),
      });
    } catch (error) {
      return fail(`요청 실패: ${error instanceof Error ? error.message : String(error)}`);
    }
    const durationMs = Date.now() - startMs;

    const responseHeaders: Record<string, string> = {};
    response.headers.forEach((v, k) => {
      responseHeaders[k] = v;
    });

    let responseBody: unknown;
    const contentType = response.headers.get('content-type') ?? '';
    if (contentType.includes('application/json')) {
      try {
        responseBody = await response.json();
      } catch {
        responseBody = null;
      }
    } else {
      responseBody = await response.text();
    }

    return ok({
      request: {
        method,
        url: url.toString(),
        headers: requestHeaders,
        body: body ?? null,
      },
      response: {
        status: response.status,
        statusText: response.statusText,
        headers: responseHeaders,
        body: responseBody,
        durationMs,
      },
    });
  },
};
