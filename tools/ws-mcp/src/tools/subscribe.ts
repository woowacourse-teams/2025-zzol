import { z } from 'zod';
import { StompSession } from '../stomp/client.js';
import { fail, ok, tryParseJson, type ToolDefinition } from './types.js';

const SubscribeArgsSchema = z.object({
  topic: z.string({ required_error: 'topic 은 필수입니다' }),
  roomToken: z.string({ required_error: 'roomToken 은 필수입니다' }),
  joinCode: z.string().optional(),
  playerName: z.string().optional(),
  durationMs: z.number().min(100).max(60_000).optional(),
  maxMessages: z.number().min(1).max(10_000).optional(),
});

const DEFAULT_DURATION_MS = 10_000;
const DEFAULT_MAX_MESSAGES = 1_000;

export const wsSubscribeTool: ToolDefinition = {
  name: 'ws_subscribe',
  description:
    '토픽을 구독하고 durationMs 동안 캡처된 envelope 메시지를 raw 형태로 반환합니다. ' +
    'roomToken 은 POST /api/rooms/{joinCode}/session-token (ADR-0009) 로 발급받습니다.',
  inputSchema: {
    type: 'object',
    properties: {
      topic: { type: 'string' },
      roomToken: { type: 'string' },
      joinCode: { type: 'string' },
      playerName: { type: 'string' },
      durationMs: { type: 'number', minimum: 100, maximum: 60_000 },
      maxMessages: { type: 'number', minimum: 1, maximum: 10_000 },
    },
    required: ['topic', 'roomToken'],
    additionalProperties: false,
  },
  handler: async (rawArgs, ctx) => {
    const parsed = SubscribeArgsSchema.safeParse(rawArgs);
    if (!parsed.success) return fail(parsed.error.issues[0]?.message ?? '잘못된 인수');
    const args = parsed.data;

    const duration = args.durationMs ?? DEFAULT_DURATION_MS;
    const maxMessages = args.maxMessages ?? DEFAULT_MAX_MESSAGES;
    const session = await StompSession.connect({
      brokerUrl: ctx.brokerUrl,
      roomToken: args.roomToken,
      ...(args.joinCode ? { joinCode: args.joinCode } : {}),
      ...(args.playerName ? { playerName: args.playerName } : {}),
    });

    const messages: { receivedAt: string; body: unknown; headers: Record<string, string> }[] = [];
    let unsubscribe: (() => void) | undefined;
    try {
      let timeoutId: ReturnType<typeof setTimeout> | undefined;
      await new Promise<void>((resolve) => {
        unsubscribe = session.subscribe(args.topic, (message) => {
          if (messages.length >= maxMessages) return;
          messages.push({
            receivedAt: new Date().toISOString(),
            body: tryParseJson(message.body),
            headers: Object.assign({}, message.headers),
          });
          if (messages.length >= maxMessages) {
            clearTimeout(timeoutId);
            unsubscribe?.();
            resolve();
          }
        });
        timeoutId = setTimeout(resolve, duration);
      });
    } finally {
      unsubscribe?.();
      await session.close();
    }

    return ok({
      topic: args.topic,
      durationMs: duration,
      captured: messages.length,
      truncated: messages.length >= maxMessages,
      messages,
    });
  },
};
