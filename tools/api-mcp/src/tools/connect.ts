import { z } from 'zod';
import { StompSession } from '../stomp/client.js';
import { fail, ok, type ToolDefinition } from './types.js';

const ConnectArgsSchema = z.object({
  roomToken: z.string({ required_error: 'roomToken 은 필수입니다' }),
  joinCode: z.string().optional(),
  playerName: z.string().optional(),
});

export const wsConnectTool: ToolDefinition = {
  name: 'ws_connect',
  description: 'STOMP 세션을 짧게 열어 연결 가능 여부를 확인합니다. 검증 후 자동으로 닫힘.',
  inputSchema: {
    type: 'object',
    properties: {
      joinCode: { type: 'string' },
      roomToken: { type: 'string' },
      playerName: { type: 'string' },
    },
    required: ['roomToken'],
    additionalProperties: false,
  },
  handler: async (rawArgs, ctx) => {
    const parsed = ConnectArgsSchema.safeParse(rawArgs);
    if (!parsed.success) return fail(parsed.error.issues[0]?.message ?? '잘못된 인수');
    const args = parsed.data;
    try {
      const session = await StompSession.connect({
        brokerUrl: ctx.brokerUrl,
        roomToken: args.roomToken,
        ...(args.joinCode ? { joinCode: args.joinCode } : {}),
        ...(args.playerName ? { playerName: args.playerName } : {}),
      });
      try {
        await session.close();
      } catch {
        // close 실패는 연결 성공 여부에 영향을 주지 않음
      }
      return ok({ connected: true, brokerUrl: ctx.brokerUrl });
    } catch (error) {
      return ok({
        connected: false,
        brokerUrl: ctx.brokerUrl,
        error: error instanceof Error ? error.message : String(error),
      });
    }
  },
};
