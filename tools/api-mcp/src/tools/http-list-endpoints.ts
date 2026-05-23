import { z } from 'zod';
import type { ToolDefinition, ToolContext } from './types.js';
import { ok, fail } from './types.js';
import type { OperationObject } from '../openapi/types.js';

const HTTP_METHODS = ['get', 'post', 'put', 'delete', 'patch', 'head', 'options'] as const;

const InputSchema = z.object({
  method: z.string().optional(),
  tag: z.string().optional(),
  q: z.string().optional(),
});

export const httpListEndpointsTool: ToolDefinition = {
  name: 'http_list_endpoints',
  description:
    'OpenAPI spec에서 HTTP 엔드포인트 목록을 조회합니다. method/tag/키워드로 필터링 가능합니다.',
  inputSchema: {
    type: 'object',
    properties: {
      method: {
        type: 'string',
        enum: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS'],
        description: '메서드 필터',
      },
      tag: { type: 'string', description: '태그 필터 (예: room, admin)' },
      q: { type: 'string', description: '경로/summary 부분 문자열 검색' },
    },
  },
  async handler(args: Record<string, unknown>, ctx: ToolContext) {
    const parsed = InputSchema.safeParse(args);
    if (!parsed.success) return fail(parsed.error.message);

    let spec;
    try {
      spec = await ctx.openapi.load();
    } catch (error) {
      return fail(
        `OpenAPI spec 로드 실패: ${error instanceof Error ? error.message : String(error)}`
      );
    }

    const { method, tag, q } = parsed.data;
    const results: unknown[] = [];

    for (const [path, pathItem] of Object.entries(spec.paths)) {
      for (const m of HTTP_METHODS) {
        if (method && m !== method.toLowerCase()) continue;
        const op = pathItem[m] as OperationObject | undefined;
        if (!op) continue;
        if (tag && !(op.tags ?? []).some((t) => t.toLowerCase().includes(tag.toLowerCase())))
          continue;
        if (q) {
          const haystack = `${path} ${op.summary ?? ''} ${op.operationId ?? ''}`.toLowerCase();
          if (!haystack.includes(q.toLowerCase())) continue;
        }
        results.push({
          method: m.toUpperCase(),
          path,
          operationId: op.operationId,
          summary: op.summary,
          tags: op.tags,
          deprecated: op.deprecated ?? false,
        });
      }
    }

    return ok({ total: results.length, endpoints: results });
  },
};
