import { Client, type IMessage, type StompHeaders } from '@stomp/stompjs';
import WebSocket from 'ws';

export interface ConnectOptions {
  brokerUrl: string;
  roomToken: string;
  joinCode?: string;
  playerName?: string;
}

export class StompSession {
  private constructor(private readonly client: Client) {}

  static async connect(options: ConnectOptions): Promise<StompSession> {
    const connectHeaders: StompHeaders = { roomToken: options.roomToken };
    if (options.joinCode) {
      connectHeaders.joinCode = options.joinCode;
    }
    if (options.playerName) {
      connectHeaders.playerName = options.playerName;
    }
    const client = new Client({
      brokerURL: options.brokerUrl,
      webSocketFactory: () => new WebSocket(options.brokerUrl) as unknown,
      connectHeaders,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      reconnectDelay: 0,
    });

    await new Promise<void>((resolve, reject) => {
      client.onConnect = () => {
        resolve();
      };
      client.onStompError = (frame) => {
        reject(new Error(frame.headers.message ?? 'STOMP error'));
      };
      client.onWebSocketError = (event) => {
        reject(new Error(String(event)));
      };
      client.onWebSocketClose = (event: { code?: number }) => {
        reject(
          new Error(`WebSocket closed before connect (code=${String(event.code ?? 'unknown')})`)
        );
      };
      client.activate();
    });

    return new StompSession(client);
  }

  subscribe(destination: string, onMessage: (message: IMessage) => void): () => void {
    const sub = this.client.subscribe(destination, onMessage);
    return () => {
      sub.unsubscribe();
    };
  }

  send(destination: string, body: unknown): void {
    this.client.publish({
      destination,
      body: typeof body === 'string' ? body : JSON.stringify(body),
      headers: { 'content-type': 'application/json' },
    });
  }

  async close(): Promise<void> {
    await this.client.deactivate();
  }
}
