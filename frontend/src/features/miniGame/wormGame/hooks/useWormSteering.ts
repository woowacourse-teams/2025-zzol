import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { PointerEvent, RefObject, useCallback, useEffect, useRef } from 'react';
import { SteerCommand } from '@/types/miniGame/wormGame';
import { WormStore } from '../core/wormStore';

/** 조향 전송 주기(10Hz). 마지막 값만 유효하므로 변화 시에만 보낸다 */
const SEND_INTERVAL_MS = 100;

type Params = {
  store: WormStore;
  joinCode: string;
  playerName: string;
  containerRef: RefObject<HTMLElement | null>;
  /** 조준 입력 허용(PREPARE 중에도 미리 방향을 잡을 수 있게 전송과 분리) */
  inputEnabled: boolean;
  /** 서버 전송 허용(PLAYING·생존 중) */
  sendEnabled: boolean;
};

/**
 * 포인터 방향 조향(PC·모바일 단일 경로). 여기서는 포인터 위치(px)만 스토어에 기록하고,
 * 목표각은 렌더러가 매 프레임 "포인터 → 월드 → 머리 기준"으로 계산한다 — 마우스는 클릭 없이 hover 를,
 * 터치는 누르고 있는 위치를 카메라가 움직여도 계속 따라간다(설계 데모와 동일).
 * 서버에는 10Hz·변화 시만 보낸다.
 */
export const useWormSteering = ({
  store,
  joinCode,
  playerName,
  containerRef,
  inputEnabled,
  sendEnabled,
}: Params) => {
  const { send, isConnected } = useWebSocket();
  const seqRef = useRef(0);
  const lastSentRef = useRef<number | null>(null);

  useEffect(() => {
    store.setInputEnabled(inputEnabled);
  }, [store, inputEnabled]);

  const track = useCallback(
    (e: PointerEvent<HTMLElement>) => {
      const el = containerRef.current;
      if (!el) return;
      const rect = el.getBoundingClientRect();
      store.setPointer({ x: e.clientX - rect.left, y: e.clientY - rect.top });
    },
    [containerRef, store]
  );

  const onPointerDown = useCallback(
    (e: PointerEvent<HTMLElement>) => {
      e.currentTarget.setPointerCapture(e.pointerId);
      track(e);
    },
    [track]
  );

  useEffect(() => {
    if (!sendEnabled || !isConnected) return;
    // (재)연결 직후엔 현재 목표각을 한 번 다시 보낸다 — 끊김 중 send 는 조용히 실패하므로
    lastSentRef.current = null;
    const timer = setInterval(() => {
      const angle = store.targetAngle;
      if (angle === null || angle === lastSentRef.current) return;
      lastSentRef.current = angle;
      seqRef.current += 1;
      const command: SteerCommand = { playerName, angle, seq: seqRef.current };
      send(`/room/${joinCode}/worm/steer`, command);
    }, SEND_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [sendEnabled, isConnected, joinCode, playerName, send, store]);

  return { onPointerDown, onPointerMove: track };
};
