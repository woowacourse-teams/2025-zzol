import { z } from 'zod';
import type { ToolDefinition, ToolContext } from './types.js';
import { ok, fail } from './types.js';
import type { OperationObject, OpenApiSpec } from '../openapi/types.js';

const InputSchema = z.object({
  method: z.string(),
  path: z.string(),
});

function collectRefs(
  obj: unknown,
  spec: OpenApiSpec,
  visited = new Set<string>()
): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  JSON.stringify(obj, (_k, v: unknown) => {
    if (typeof v === 'object' && v !== null && '$ref' in v) {
      const ref = (v as Record<string, unknown>).$ref as string;
      if (!visited.has(ref)) {
        visited.add(ref);
        const match = /^#\/components\/schemas\/(.+)$/.exec(ref);
        if (match?.[1]) {
          const name = match[1];
          const resolved = spec.components?.schemas?.[name];
          if (resolved) {
            result[name] = resolved;
            Object.assign(result, collectRefs(resolved, spec, visited));
          }
        }
      }
    }
    return v;
  });
  return result;
}

export const httpDescribeTool: ToolDefinition = {
  name: 'http_describe',
  description: '특정 HTTP 엔드포인트의 요청/응답 스키마를 상세히 반환합니다.',
  inputSchema: {
    type: 'object',
    properties: {
      method: { type: 'string', description: 'HTTP 메서드 (예: GET, POST)' },
      path: { type: 'string', description: 'API 경로 (예: /api/rooms/{roomId})' },
    },
    required: ['method', 'path'],
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

    const { method, path } = parsed.data;
    const pathItem = spec.paths[path];
    if (!pathItem) return fail(`경로를 찾을 수 없습니다: ${path}`);

    const op = pathItem[method.toLowerCase()] as OperationObject | undefined;
    if (!op) return fail(`${method.toUpperCase()} ${path} 를 찾을 수 없습니다`);

    const schemas = collectRefs({ requestBody: op.requestBody, responses: op.responses }, spec);

    return ok({
      method: method.toUpperCase(),
      path,
      operationId: op.operationId,
      summary: op.summary,
      description: op.description,
      tags: op.tags,
      parameters: op.parameters,
      requestBody: op.requestBody,
      responses: op.responses,
      schemas,
    });
  },
};
