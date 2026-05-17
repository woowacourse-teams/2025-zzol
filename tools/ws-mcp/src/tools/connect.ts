import { StompSession } from '../stomp/client.js';
import { fail, ok, type ToolDefinition } from './types.js';

interface ConnectArgs {
  joinCode?: string;
  roomToken?: string;
  playerName?: string;
}

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
    const args = rawArgs as ConnectArgs;
    if (!args.roomToken) {
      return fail('roomToken 은 필수입니다');
    }
    try {
      const session = await StompSession.connect({
        brokerUrl: ctx.brokerUrl,
        roomToken: args.roomToken,
        ...(args.joinCode ? { joinCode: args.joinCode } : {}),
        ...(args.playerName ? { playerName: args.playerName } : {}),
      });
      await session.close();
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
