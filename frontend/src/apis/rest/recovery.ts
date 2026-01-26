import { WebSocketMessage } from '../websocket/constants/constants';
import { api } from './api';

export type RecoveryMessage = {
  streamId: string;
  destination: string;
  payload: WebSocketMessage<unknown>;
  timestamp: number;
};

type RecoveryResponse = {
  success: boolean;
  messages: RecoveryMessage[];
  errorMessage?: string;
};

export const fetchRecoveryMessages = async (
  joinCode: string,
  playerName: string,
  lastStreamId: string
): Promise<RecoveryMessage[]> => {
  try {
    const response = await api.post<RecoveryResponse, undefined>(
      `/rooms/${joinCode}/recovery?playerName=${encodeURIComponent(playerName)}&lastId=${encodeURIComponent(lastStreamId)}`
    );

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

export const getLastStreamId = (joinCode: string): string | null => {
  try {
    return localStorage.getItem(`lastStreamId:${joinCode}`);
  } catch {
    return null;
  }
};

export const clearLastStreamId = (joinCode: string): void => {
  try {
    localStorage.removeItem(`lastStreamId:${joinCode}`);
  } catch {
    // ignore
  }
};
