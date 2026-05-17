import { ok, type ToolDefinition } from "./types.js";

type ListArgs = {
  kind?: "topic" | "queue" | "send";
  q?: string;
};

export const wsListTopicsTool: ToolDefinition = {
  name: "ws_list_topics",
  description:
    "카탈로그의 topics/queues/sends 를 나열합니다. kind 로 필터링, q 로 path/description substring 검색.",
  inputSchema: {
    type: "object",
    properties: {
      kind: { type: "string", enum: ["topic", "queue", "send"] },
      q: { type: "string" },
    },
    additionalProperties: false,
  },
  handler: async (rawArgs, ctx) => {
    const args = rawArgs as ListArgs;
    const { catalog, source, fetchedAt } = await ctx.catalog.load();

    const topics =
      !args.kind || args.kind === "topic"
        ? catalog.topics
            .filter((t) => matches(args.q, t.path, t.publishers.map((p) => p.description).join(" "), t.payloadType ?? ""))
            .map((t) => ({
              path: t.path,
              payloadType: t.payloadType,
              publishers: t.publishers.map((p) => `${p.source.className}#${p.source.methodName}`),
            }))
        : [];
    const queues =
      !args.kind || args.kind === "queue"
        ? catalog.queues
            .filter((q) => matches(args.q, q.path, q.publishers.map((p) => p.description).join(" "), q.payloadType ?? ""))
            .map((q) => ({
              path: q.path,
              payloadType: q.payloadType,
              publishers: q.publishers.map((p) => `${p.source.className}#${p.source.methodName}`),
            }))
        : [];
    const sends =
      !args.kind || args.kind === "send"
        ? catalog.sends
            .filter((s) => matches(args.q, s.destination, s.description))
            .map((s) => ({
              destination: s.destination,
              description: s.description,
              triggersTopics: s.triggersTopics,
              source: `${s.source.className}#${s.source.methodName}`,
            }))
        : [];

    return ok({
      topics,
      queues,
      sends,
      meta: { source, fetchedAt, total: topics.length + queues.length + sends.length },
    });
  },
};

function matches(q: string | undefined, ...haystacks: string[]): boolean {
  if (!q) return true;
  const needle = q.toLowerCase();
  return haystacks.some((h) => h.toLowerCase().includes(needle));
}
