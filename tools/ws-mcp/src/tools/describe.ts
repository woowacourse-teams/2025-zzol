import type { QueueEntry, SchemaEntry, SendEntry, TopicEntry, WsCatalog } from "../catalog/types.js";
import { fail, ok, type ToolDefinition } from "./types.js";

type DescribeArgs = { path: string };

export const wsDescribeTool: ToolDefinition = {
  name: "ws_describe",
  description:
    "Path (topic / queue / send destination) 한 개의 완전한 컨트랙트를 반환합니다. " +
    "payloadType, publishers (description + className#methodName), 참조 schema 동봉.",
  inputSchema: {
    type: "object",
    properties: {
      path: {
        type: "string",
        description:
          "토픽 (/topic/...), 큐 (/user/queue/...) 또는 send destination (/app/...) 중 하나",
      },
    },
    required: ["path"],
    additionalProperties: false,
  },
  handler: async (rawArgs, ctx) => {
    const args = rawArgs as Partial<DescribeArgs>;
    if (!args.path) {
      return fail("path 는 필수입니다");
    }
    const { catalog, source, fetchedAt } = await ctx.catalog.load();
    const matched = match(catalog, args.path);
    if (!matched) {
      return fail(`path ${args.path} 에 해당하는 토픽/큐/send 가 없습니다`);
    }
    const referencedSchemas = collectSchemas(catalog, matched);
    return ok({
      ...matched,
      schemas: referencedSchemas,
      meta: { source, fetchedAt },
    });
  },
};

type Matched =
  | { kind: "topic"; entry: TopicEntry }
  | { kind: "queue"; entry: QueueEntry }
  | { kind: "send"; entry: SendEntry };

function match(catalog: WsCatalog, path: string): Matched | null {
  const topic = catalog.topics.find((t) => t.path === path);
  if (topic) return { kind: "topic", entry: topic };
  const queue = catalog.queues.find((q) => q.path === path);
  if (queue) return { kind: "queue", entry: queue };
  const send = catalog.sends.find((s) => s.destination === path);
  if (send) return { kind: "send", entry: send };
  return null;
}

function collectSchemas(catalog: WsCatalog, matched: Matched): Record<string, SchemaEntry> {
  const result: Record<string, SchemaEntry> = {};
  for (const name of matched.entry.referencedSchemas) {
    const schema = catalog.schemas[name];
    if (schema) {
      result[name] = schema;
    }
  }
  return result;
}
