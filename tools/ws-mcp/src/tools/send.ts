import { z } from 'zod';
import { StompSession } from '../stomp/client.js';
import { fail, ok, type ToolDefinition } from './types.js';

const SendArgsSchema = z.object({
  destination: z.string({ required_error: 'destination 은 필수입니다' }),
  roomToken: z.string({ required_error: 'roomToken 은 필수입니다' }),
  payload: z.unknown().optional(),
  joinCode: z.string().optional(),
  playerName: z.string().optional(),
  waitForResponseTopic: z.string().optional(),
  waitMs: z.number().min(100).max(30_000).optional(),
});

const DEFAULT_WAIT_MS = 3_000;
const MAX_CAPTURED = 100;

export const wsSendTool: ToolDefinition = {
  name: 'ws_send',
  description:
    'destination 으로 메시지를 보내고, waitForResponseTopic 이 주어지면 waitMs 동안 후속 응답을 캡처합니다. ' +
    'roomToken 은 POST /api/rooms/{joinCode}/session-token (ADR-0009) 로 발급받습니다.',
  inputSchema: {
    type: 'object',
    properties: {
      destination: { type: 'string' },
      payload: { description: '전송 body — JSON 직렬화 가능한 값' },
      roomToken: { type: 'string' },
      joinCode: { type: 'string' },
      playerName: { type: 'string' },
      waitForResponseTopic: { type: 'string' },
      waitMs: { type: 'number', minimum: 100, maximum: 30_000 },
    },
    required: ['destination', 'roomToken'],
    additionalProperties: false,
  },
  handler: async (rawArgs, ctx) => {
    const parsed = SendArgsSchema.safeParse(rawArgs);
    if (!parsed.success) return fail(parsed.error.issues[0]?.message ?? '잘못된 인수');
    const args = parsed.data;

    const session = await StompSession.connect({
      brokerUrl: ctx.brokerUrl,
      roomToken: args.roomToken,
      ...(args.joinCode ? { joinCode: args.joinCode } : {}),
      ...(args.playerName ? { playerName: args.playerName } : {}),
    });

    const captured: { receivedAt: string; body: unknown }[] = [];
    let unsubscribe: (() => void) | undefined;
    try {
      if (args.waitForResponseTopic) {
        const responseTopic = args.waitForResponseTopic;
        let timeoutId: ReturnType<typeof setTimeout> | undefined;
        await new Promise<void>((resolve) => {
          unsubscribe = session.subscribe(responseTopic, (message) => {
            if (captured.length >= MAX_CAPTURED) return;
            captured.push({
              receivedAt: new Date().toISOString(),
              body: tryParseJson(message.body),
            });
            if (captured.length >= MAX_CAPTURED) {
              clearTimeout(timeoutId);
              unsubscribe?.();
              resolve();
            }
          });
          session.send(args.destination, args.payload ?? {});
          timeoutId = setTimeout(resolve, args.waitMs ?? DEFAULT_WAIT_MS);
        });
      } else {
        session.send(args.destination, args.payload ?? {});
      }
    } finally {
      unsubscribe?.();
      await session.close();
    }

    return ok({
      destination: args.destination,
      sent: true,
      responseTopic: args.waitForResponseTopic ?? null,
      captured,
      truncated: captured.length >= MAX_CAPTURED,
    });
  },
};

function tryParseJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}
