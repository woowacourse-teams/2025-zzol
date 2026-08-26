import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { PointerEvent, RefObject, useCallback, useEffect, useRef } from 'react';
import { SteerCommand } from '@/types/miniGame/wormGame';
import { WormStore } from '../core/wormStore';

/** 머리(화면 중앙) 주변 데드존 — 손떨림으로 각도가 튀는 것을 막는다 */
const DEAD_ZONE_PX = 20;
/** 조향 전송 주기(10Hz). 마지막 값만 유효하므로 변화 시에만 보낸다 */
const SEND_INTERVAL_MS = 100;

type Params = {
  store: WormStore;
  joinCode: string;
  playerName: string;
  containerRef: RefObject<HTMLElement | null>;
  enabled: boolean;
};

/**
 * 포인터 방향 조향(PC·모바일 단일 경로). 추적 카메라 덕에 머리는 항상 컨테이너 중앙이므로
 * "중앙 → 포인터" 각도가 곧 목표각이다. 로컬 예측용으로 store 에 즉시 쓰고, 서버에는 10Hz·변화 시만 보낸다.
 */
export const useWormSteering = ({ store, joinCode, playerName, containerRef, enabled }: Params) => {
  const { send } = useWebSocket();
  const seqRef = useRef(0);
  const lastSentRef = useRef<number | null>(null);

  const steerTo = useCallback(
    (e: PointerEvent<HTMLElement>) => {
      const el = containerRef.current;
      if (!el || !enabled) return;
      const rect = el.getBoundingClientRect();
      const dx = e.clientX - (rect.left + rect.width / 2);
      const dy = e.clientY - (rect.top + rect.height / 2);
      if (Math.hypot(dx, dy) < DEAD_ZONE_PX) return;
      store.steer(Math.atan2(dy, dx));
    },
    [containerRef, enabled, store]
  );

  const onPointerDown = useCallback(
    (e: PointerEvent<HTMLElement>) => {
      e.currentTarget.setPointerCapture?.(e.pointerId);
      steerTo(e);
    },
    [steerTo]
  );
  const onPointerMove = useCallback(
    (e: PointerEvent<HTMLElement>) => {
      // 마우스는 누른 채 드래그할 때만, 터치·펜은 항상(터치는 접촉 중에만 move 가 온다)
      if (e.pointerType === 'mouse' && e.buttons === 0) return;
      steerTo(e);
    },
    [steerTo]
  );

  useEffect(() => {
    if (!enabled) return;
    const timer = setInterval(() => {
      const angle = store.targetAngle;
      if (angle === null || angle === lastSentRef.current) return;
      lastSentRef.current = angle;
      seqRef.current += 1;
      const command: SteerCommand = { playerName, angle, seq: seqRef.current };
      send(`/room/${joinCode}/worm/steer`, command);
    }, SEND_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [enabled, joinCode, playerName, send, store]);

  return { onPointerDown, onPointerMove };
};
