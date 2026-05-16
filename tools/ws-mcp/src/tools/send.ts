import { StompSession } from "../stomp/client.js";
import { fail, ok, type ToolDefinition } from "./types.js";

type SendArgs = {
  destination?: string;
  payload?: unknown;
  roomToken?: string;
  joinCode?: string;
  playerName?: string;
  waitForResponseTopic?: string;
  waitMs?: number;
};

const DEFAULT_WAIT_MS = 3_000;

export const wsSendTool: ToolDefinition = {
  name: "ws_send",
  description:
    "destination 으로 메시지를 보내고, waitForResponseTopic 이 주어지면 waitMs 동안 후속 응답을 캡처합니다.",
  inputSchema: {
    type: "object",
    properties: {
      destination: { type: "string" },
      payload: {},
      roomToken: { type: "string" },
      joinCode: { type: "string" },
      playerName: { type: "string" },
      waitForResponseTopic: { type: "string" },
      waitMs: { type: "number", minimum: 100, maximum: 30_000 },
    },
    required: ["destination", "roomToken"],
  },
  handler: async (rawArgs, ctx) => {
    const args = rawArgs as SendArgs;
    if (!args.destination) return fail("destination 은 필수입니다");
    if (!args.roomToken) return fail("roomToken 은 필수입니다");

    const session = await StompSession.connect({
      brokerUrl: ctx.brokerUrl,
      roomToken: args.roomToken,
      ...(args.joinCode ? { joinCode: args.joinCode } : {}),
      ...(args.playerName ? { playerName: args.playerName } : {}),
    });

    const captured: Array<{ receivedAt: string; body: unknown }> = [];
    let unsubscribe: (() => void) | null = null;
    if (args.waitForResponseTopic) {
      unsubscribe = session.subscribe(args.waitForResponseTopic, (message) => {
        captured.push({
          receivedAt: new Date().toISOString(),
          body: tryParseJson(message.body),
        });
      });
    }

    session.send(args.destination, args.payload ?? {});

    if (args.waitForResponseTopic) {
      await new Promise((resolve) => setTimeout(resolve, args.waitMs ?? DEFAULT_WAIT_MS));
      unsubscribe?.();
    }
    await session.close();

    return ok({
      destination: args.destination,
      sent: true,
      responseTopic: args.waitForResponseTopic ?? null,
      captured,
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
