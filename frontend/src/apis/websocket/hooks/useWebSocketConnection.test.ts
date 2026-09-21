import { act, renderHook } from '@testing-library/react';
import type { Client, IFrame } from '@stomp/stompjs';
import { useWebSocketConnection } from './useWebSocketConnection';
import { createStompClient } from '../utils/createStompClient';

jest.mock('../utils/createStompClient');
jest.mock('../utils/WebSocketErrorHandler');

/**
 * isConnected 는 stompjs 콜백으로만 갱신되는 React 상태다.
 * 서버가 error 없이 close 만 해도 false 로 내려가야 버튼 잠금(#1792)이 동작한다.
 */
describe('useWebSocketConnection', () => {
  let client: Partial<Client>;

  beforeEach(() => {
    client = { activate: jest.fn(), deactivate: jest.fn() };
    (createStompClient as jest.Mock).mockReturnValue(client);
    jest.spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => jest.restoreAllMocks());

  const connect = () => {
    const { result } = renderHook(() => useWebSocketConnection());
    act(() => result.current.startSocket('token'));
    act(() => client.onConnect?.({} as IFrame));
    expect(result.current.isConnected).toBe(true);
    return result;
  };

  it('onWebSocketClose 가 오면 isConnected 가 false 가 된다', () => {
    const result = connect();
    act(() => client.onWebSocketClose?.({} as CloseEvent));
    expect(result.current.isConnected).toBe(false);
  });

  it('onWebSocketError 가 오면 isConnected 가 false 가 된다', () => {
    const result = connect();
    act(() => client.onWebSocketError?.({} as Event));
    expect(result.current.isConnected).toBe(false);
  });

  // deactivate() 뒤에도 stompjs 는 close 콜백을 부른다. 죽은 소켓은 receipt 가 없어 약 8초 뒤에 오는데,
  // 그 사이 새 클라이언트가 붙었으면 옛 close 가 새 연결의 isConnected 를 덮으면 안 된다.
  it('버린 옛 클라이언트의 늦은 close 는 새 연결의 isConnected 를 덮지 않는다', () => {
    const result = connect();
    const oldClient = client;
    act(() => result.current.stopSocket());

    client = { activate: jest.fn(), deactivate: jest.fn() };
    (createStompClient as jest.Mock).mockReturnValue(client);
    act(() => result.current.startSocket('token'));
    act(() => client.onConnect?.({} as IFrame));
    expect(result.current.isConnected).toBe(true);

    act(() => oldClient.onWebSocketClose?.({} as CloseEvent));
    expect(result.current.isConnected).toBe(true);
  });
});
