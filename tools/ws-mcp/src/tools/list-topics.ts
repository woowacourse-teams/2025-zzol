import { z } from 'zod';
import type { QueueEntry, SendEntry, TopicEntry } from '../catalog/types.js';
import { fail, ok, type ToolDefinition } from './types.js';

const ListArgsSchema = z.object({
  kind: z.enum(['topic', 'queue', 'send']).optional(),
  q: z.string().optional(),
});

export const wsListTopicsTool: ToolDefinition = {
  name: 'ws_list_topics',
  description:
    '카탈로그의 topics/queues/sends 를 나열합니다. kind 로 필터링, q 로 path/description substring 검색.',
  inputSchema: {
    type: 'object',
    properties: {
      kind: { type: 'string', enum: ['topic', 'queue', 'send'] },
      q: { type: 'string' },
    },
    additionalProperties: false,
  },
  handler: async (rawArgs, ctx) => {
    const parsed = ListArgsSchema.safeParse(rawArgs);
    if (!parsed.success) return fail(parsed.error.issues[0]?.message ?? '잘못된 인수');
    const args = parsed.data;
    const { catalog, source, fetchedAt } = await ctx.catalog.load();

    const topics =
      !args.kind || args.kind === 'topic'
        ? catalog.topics
            .filter((t) =>
              matches(args.q, t.path, t.publishers.map((p) => p.description).join(' '), t.payloadType ?? '')
            )
            .map(projectTopic)
        : [];
    const queues =
      !args.kind || args.kind === 'queue'
        ? catalog.queues
            .filter((q) =>
              matches(args.q, q.path, q.publishers.map((p) => p.description).join(' '), q.payloadType ?? '')
            )
            .map(projectQueue)
        : [];
    const sends =
      !args.kind || args.kind === 'send'
        ? catalog.sends.filter((s) => matches(args.q, s.destination, s.description)).map(projectSend)
        : [];

    return ok({
      topics,
      queues,
      sends,
      meta: { source, fetchedAt, total: topics.length + queues.length + sends.length },
    });
  },
};

function projectTopic(t: TopicEntry) {
  return {
    path: t.path,
    payloadType: t.payloadType,
    publishers: t.publishers.map((p) => `${p.source.className}#${p.source.methodName}`),
  };
}

function projectQueue(q: QueueEntry) {
  return {
    path: q.path,
    payloadType: q.payloadType,
    publishers: q.publishers.map((p) => `${p.source.className}#${p.source.methodName}`),
  };
}

function projectSend(s: SendEntry) {
  return {
    destination: s.destination,
    description: s.description,
    triggersTopics: s.triggersTopics,
    source: `${s.source.className}#${s.source.methodName}`,
  };
}

function matches(q: string | undefined, ...haystacks: string[]): boolean {
  if (!q) return true;
  const needle = q.toLowerCase();
  return haystacks.some((h) => h.toLowerCase().includes(needle));
}
