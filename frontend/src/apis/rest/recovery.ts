import { WebSocketMessage } from '../websocket/constants/constants';

export type RecoveryMessage = {
  streamId: string;
  destination: string;
  response: WebSocketMessage<unknown>;
  timestamp: number;
};

type RecoveryResponse = {
  success: boolean;
  messages: RecoveryMessage[];
  errorMessage?: string;
};

const getApiUrl = (): string => {
  // Storybook 환경에서는 process.env가 없을 수 있음
  if (typeof process !== 'undefined' && process.env?.API_URL) {
    return process.env.API_URL;
  }
  return '';
};

export const fetchRecoveryMessages = async (
  joinCode: string,
  playerName: string,
  lastStreamId: string
): Promise<RecoveryMessage[]> => {
  try {
    const apiUrl = getApiUrl();
    const url = `${apiUrl}/api/rooms/${joinCode}/recovery?playerName=${encodeURIComponent(playerName)}&lastId=${encodeURIComponent(lastStreamId)}`;

    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    });

    if (!res.ok) {
      console.warn('Recovery API 실패:', res.status);
      return [];
    }

    const response: RecoveryResponse = await res.json();

    if (!response.success) {
      console.warn('Recovery API 실패:', response.errorMessage);
      return [];
    }

    return response.messages ?? [];
  } catch (error) {
    console.error('Recovery API 호출 실패:', error);
    return [];
  }
};

export const getLastStreamId = (joinCode: string, playerName: string): string | null => {
  try {
    return localStorage.getItem(`lastStreamId:${joinCode}:${playerName}`);
  } catch {
    return null;
  }
};

export const saveLastStreamId = (joinCode: string, playerName: string, streamId: string): void => {
  try {
    localStorage.setItem(`lastStreamId:${joinCode}:${playerName}`, streamId);
  } catch {
    // ignore
  }
};

export const clearLastStreamId = (joinCode: string, playerName: string): void => {
  try {
    localStorage.removeItem(`lastStreamId:${joinCode}:${playerName}`);
  } catch {
    // ignore
  }
};
