import { WebSocketMessage } from '../websocket/constants/constants';
import { api } from './api';

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

export const fetchRecoveryMessages = async (
  joinCode: string,
  playerName: string,
  lastStreamId: string
): Promise<RecoveryMessage[]> => {
  try {
    const response = await api.post<RecoveryResponse, undefined>(
      `/api/rooms/${joinCode}/recovery?playerName=${encodeURIComponent(playerName)}&lastId=${encodeURIComponent(lastStreamId)}`
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
