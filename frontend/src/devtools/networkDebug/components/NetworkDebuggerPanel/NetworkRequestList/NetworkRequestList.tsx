import { useMemo } from 'react';
import { NetworkRequest } from '@/devtools/networkDebug/types/network';
import { checkIsTouchDevice } from '@/utils/checkIsTouchDevice';
import * as S from './NetworkRequestList.styled';

const COLUMN_WIDTHS = {
  TYPE: '80px',
  CONTEXT: '70px',
  STATUS: '50px',
} as const;

type Props = {
  requests: NetworkRequest[];
  selectedRequestId: string | null;
  onSelectRequest: (id: string) => void;
};

const API_URL = process.env.API_URL || '';

/**
 * 네트워크 요청 목록을 표시하는 컴포넌트입니다.
 */
const NetworkRequestList = ({ requests, selectedRequestId, onSelectRequest }: Props) => {
  const isMobile = useMemo(() => checkIsTouchDevice(), []);

  /**
   * 모바일에서 URL에서 API_URL을 제거한 경로를 반환합니다.
   */
  const formatUrlForMobile = (url: string): string => {
    if (!isMobile || !API_URL) return url;
    if (url.startsWith(API_URL)) {
      return url.substring(API_URL.length);
    }
    return url;
  };

  /**
   * 요청의 상태에 따른 색상을 반환합니다.
   */
  const getStatusColor = (request: NetworkRequest): string => {
    const status = request.status;
    if (!status) return '#999';
    if (status === 'NETWORK_ERROR') return '#d93025';
    if (request.type === 'websocket') {
      // WebSocket Status 101은 성공적으로 연결됨을 의미
      if (status === 101 || request.connectionStatus === 'open') return '#0f9d58';
      if (request.connectionStatus === 'error') return '#d93025';
      if (request.connectionStatus === 'closed') return '#999';
      return '#0f9d58';
    }
    if (typeof status === 'number') {
      if (status >= 200 && status < 300) return '#0f9d58';
      if (status >= 300 && status < 400) return '#f4b400';
      if (status >= 400) return '#d93025';
    }
    return '#999';
  };

  /**
   * 요청의 상태 텍스트를 반환합니다.
   */
  const getStatusText = (request: NetworkRequest): string => {
    if (request.type === 'websocket') {
      // WebSocket은 Status 101로 표시 (구글 개발자 도구와 동일)
      if (request.status === 101) {
        return '101';
      }
      // fallback
      if (request.connectionStatus === 'open') return '101';
      if (request.connectionStatus === 'closed') return 'Closed';
      if (request.connectionStatus === 'error') return 'Error';
      return '101';
    }
    if (request.status === 'NETWORK_ERROR') return 'Error';
    return String(request.status || '-');
  };

  if (requests.length === 0) {
    return (
      <S.EmptyState>
        <S.EmptyText>No network requests</S.EmptyText>
      </S.EmptyState>
    );
  }

  return (
    <S.List>
      <S.ListHeader>
        <S.HeaderCell $width={COLUMN_WIDTHS.TYPE}>Type</S.HeaderCell>
        {!isMobile && <S.HeaderCell $width={COLUMN_WIDTHS.CONTEXT}>Context</S.HeaderCell>}
        <S.HeaderCell $flex={1}>URL</S.HeaderCell>
        <S.HeaderCell $width={COLUMN_WIDTHS.STATUS}>Status</S.HeaderCell>
      </S.ListHeader>
      <S.ListBody>
        {requests.map((request) => (
          <S.RequestRow
            key={request.id}
            selected={selectedRequestId === request.id}
            onClick={() => onSelectRequest(request.id)}
          >
            <S.RequestCell $width={COLUMN_WIDTHS.TYPE}>
              <S.TypeBadge type={request.type}>{request.type}</S.TypeBadge>
            </S.RequestCell>
            {!isMobile && (
              <S.RequestCell $width={COLUMN_WIDTHS.CONTEXT}>
                <S.ContextBadge>{request.context}</S.ContextBadge>
              </S.RequestCell>
            )}
            <S.RequestCell $flex={1} title={request.url}>
              <S.UrlText>{formatUrlForMobile(request.url)}</S.UrlText>
            </S.RequestCell>
            <S.RequestCell $width={COLUMN_WIDTHS.STATUS}>
              <S.StatusText color={getStatusColor(request)}>{getStatusText(request)}</S.StatusText>
            </S.RequestCell>
          </S.RequestRow>
        ))}
      </S.ListBody>
    </S.List>
  );
};

export default NetworkRequestList;
