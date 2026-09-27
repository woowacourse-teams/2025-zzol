import { reportWebSocketError } from '@/apis/utils/reportSentryError';
import WebSocketErrorHandler from './WebSocketErrorHandler';

jest.mock('@/apis/utils/reportSentryError');

describe('WebSocketErrorHandler', () => {
  beforeEach(() => jest.spyOn(console, 'error').mockImplementation(() => {}));
  afterEach(() => jest.restoreAllMocks());

  // 연결 대기 중 클릭은 정상 상황이라 error 로 알림을 받지 않는다 (#1792)
  it('미연결 상태의 send 는 warning 으로 보고한다', () => {
    WebSocketErrorHandler.handleConnectionRequiredError({
      type: 'send',
      url: '/room/X/update-ready',
      isConnected: false,
      hasClient: true,
    });

    expect(reportWebSocketError).toHaveBeenCalledWith(
      expect.stringContaining('WebSocket 연결 안됨'),
      expect.objectContaining({ level: 'warning' })
    );
  });
});
