import type { WebSocketMessage } from '../types/network';

/**
 * STOMP 메시지의 요약 문자열을 생성합니다.
 * 명령어, destination, nid, receipt 등의 정보를 포함합니다.
 *
 * @param message - WebSocket 메시지 객체
 * @returns 메시지 요약 문자열
 */
export const getMessageSummary = (message: WebSocketMessage): string => {
  if (message.isStompMessage && message.stompHeaders) {
    const command = message.stompHeaders['command'] || 'STOMP';
    const destination = message.stompHeaders['destination'] || '';
    const nid = message.stompHeaders['nid'] || message.stompHeaders['id'] || '';
    const receipt = message.stompHeaders['receipt'] || '';
    const receiptId = message.stompHeaders['receipt-id'] || '';

    let summary = command;

    // SUBSCRIBE/UNSUBSCRIBE의 경우 nid 추가
    if ((command === 'SUBSCRIBE' || command === 'UNSUBSCRIBE') && nid) {
      summary = `${command} ${nid}`;
      if (destination) {
        summary = `${summary} ${destination}`;
      }
    } else if (command === 'DISCONNECT' && receipt) {
      // DISCONNECT의 경우 receipt 추가
      summary = `${command} ${receipt}`;
    } else if (command === 'RECEIPT' && receiptId) {
      // RECEIPT의 경우 receipt-id 추가
      summary = `${command} ${receiptId}`;
    } else if (destination) {
      summary = `${command} ${destination}`;
    }

    return summary;
  }

  // 일반 메시지인 경우 데이터 일부 표시
  const maxLength = 100;
  const data = message.data || '';
  const summary = data.length > maxLength ? data.substring(0, maxLength) + '...' : data;
  return summary;
};
