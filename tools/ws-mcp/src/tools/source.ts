import { z } from 'zod';
import { fail, ok, type ToolDefinition } from './types.js';

const SourceArgsSchema = z.object({
  path: z.string({ required_error: 'path 는 필수입니다' }),
});

export const wsSourceTool: ToolDefinition = {
  name: 'ws_source',
  description: 'Path 의 발행/처리 메서드 소스 위치 (className#methodName) 만 간단히 반환합니다.',
  inputSchema: {
    type: 'object',
    properties: {
      path: { type: 'string' },
    },
    required: ['path'],
    additionalProperties: false,
  },
  handler: async (rawArgs, ctx) => {
    const parsed = SourceArgsSchema.safeParse(rawArgs);
    if (!parsed.success) return fail(parsed.error.issues[0]?.message ?? '잘못된 인수');
    const args = parsed.data;
    const { catalog } = await ctx.catalog.load();
    const sources = [
      ...catalog.topics
        .filter((t) => t.path === args.path)
        .flatMap((t) => t.publishers.map((p) => ({ kind: 'topic' as const, ...p.source }))),
      ...catalog.queues
        .filter((q) => q.path === args.path)
        .flatMap((q) => q.publishers.map((p) => ({ kind: 'queue' as const, ...p.source }))),
      ...catalog.sends
        .filter((s) => s.destination === args.path)
        .map((s) => ({ kind: 'send' as const, ...s.source })),
    ];

    if (sources.length === 0) {
      return fail(`path ${args.path} 에 해당하는 소스가 없습니다`);
    }
    return ok({ path: args.path, sources });
  },
};
