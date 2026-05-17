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
    const sources: { kind: string; className: string; methodName: string }[] = [];

    for (const topic of catalog.topics) {
      if (topic.path === args.path) {
        for (const p of topic.publishers) {
          sources.push({ kind: 'topic', ...p.source });
        }
      }
    }
    for (const queue of catalog.queues) {
      if (queue.path === args.path) {
        for (const p of queue.publishers) {
          sources.push({ kind: 'queue', ...p.source });
        }
      }
    }
    for (const send of catalog.sends) {
      if (send.destination === args.path) {
        sources.push({ kind: 'send', ...send.source });
      }
    }

    if (sources.length === 0) {
      return fail(`path ${args.path} 에 해당하는 소스가 없습니다`);
    }
    return ok({ path: args.path, sources });
  },
};
