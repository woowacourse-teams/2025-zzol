import { StompSession } from "../stomp/client.js";
import { fail, ok, type ToolDefinition } from "./types.js";

type SubscribeArgs = {
  topic?: string;
  roomToken?: string;
  joinCode?: string;
  playerName?: string;
  durationMs?: number;
  maxMessages?: number;
};

const DEFAULT_DURATION_MS = 10_000;
const DEFAULT_MAX_MESSAGES = 1_000;

export const wsSubscribeTool: ToolDefinition = {
  name: "ws_subscribe",
  description:
    "토픽을 구독하고 durationMs 동안 캡처된 envelope 메시지를 raw 형태로 반환합니다. " +
    "roomToken 은 POST /api/rooms/{joinCode}/session-token (ADR-0009) 로 발급받습니다.",
  inputSchema: {
    type: "object",
    properties: {
      topic: { type: "string" },
      roomToken: { type: "string" },
      joinCode: { type: "string" },
      playerName: { type: "string" },
      durationMs: { type: "number", minimum: 100, maximum: 60_000 },
      maxMessages: { type: "number", minimum: 1, maximum: 10_000 },
    },
    required: ["topic", "roomToken"],
    additionalProperties: false,
  },
  handler: async (rawArgs, ctx) => {
    const args = rawArgs as SubscribeArgs;
    if (!args.topic) return fail("topic 은 필수입니다");
    if (!args.roomToken) return fail("roomToken 은 필수입니다");

    const duration = args.durationMs ?? DEFAULT_DURATION_MS;
    const maxMessages = args.maxMessages ?? DEFAULT_MAX_MESSAGES;
    const session = await StompSession.connect({
      brokerUrl: ctx.brokerUrl,
      roomToken: args.roomToken,
      ...(args.joinCode ? { joinCode: args.joinCode } : {}),
      ...(args.playerName ? { playerName: args.playerName } : {}),
    });

    const messages: Array<{ receivedAt: string; body: unknown; headers: Record<string, string> }> = [];
    let unsubscribe: (() => void) | undefined;
    try {
      unsubscribe = session.subscribe(args.topic, (message) => {
        if (messages.length < maxMessages) {
          messages.push({
            receivedAt: new Date().toISOString(),
            body: tryParseJson(message.body),
            headers: { ...message.headers },
          });
        }
      });
      await new Promise((resolve) => setTimeout(resolve, duration));
    } finally {
      unsubscribe?.();
      await session.close();
    }

    return ok({ topic: args.topic, durationMs: duration, captured: messages.length, messages });
  },
};

function tryParseJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}
