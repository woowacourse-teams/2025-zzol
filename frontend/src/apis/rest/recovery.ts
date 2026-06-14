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

const LAST_STREAM_ID_PREFIX = 'lastStreamId:';

export const getLastStreamId = (joinCode: string, playerName: string): string | null => {
  try {
    return localStorage.getItem(`${LAST_STREAM_ID_PREFIX}${joinCode}:${playerName}`);
  } catch {
    return null;
  }
};

export const saveLastStreamId = (joinCode: string, playerName: string, streamId: string): void => {
  try {
    localStorage.setItem(`${LAST_STREAM_ID_PREFIX}${joinCode}:${playerName}`, streamId);
  } catch {
    // ignore
  }
};

/**
 * localStorage의 모든 `lastStreamId:` 키를 일괄 삭제한다 (#1298 누적 방지).
 *
 * - 멀티탭으로 다른 방에 접속 중인 탭의 복구 키도 함께 삭제된다 (의도된 트레이드오프)
 * - WebSocket이 살아있는 동안 호출하면 직후 도착한 메시지로 키가 재생성될 수 있다.
 *   호출 전 stopSocket()을 먼저 보장할 것 (재생성돼도 다음 sweep에서 정리됨)
 */
export const clearAllLastStreamIds = (): void => {
  try {
    // 삭제 중 인덱스 시프트를 피하기 위해 키를 먼저 수집한 뒤 일괄 삭제
    const keys: string[] = [];
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key?.startsWith(LAST_STREAM_ID_PREFIX)) {
        keys.push(key);
      }
    }
    keys.forEach((key) => localStorage.removeItem(key));
  } catch {
    // ignore
  }
};
