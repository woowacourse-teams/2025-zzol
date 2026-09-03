import { PointerEvent, ReactNode, useRef, useEffect, useState } from 'react';
import * as S from './RacingGameOverlay.styled';
import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useRacingGame } from '@/contexts/RacingGame/RacingGameContext';
import DisconnectOverlay from '../DisconnectOverlay/DisconnectOverlay';

const TAP_SEND_INTERVAL_MS = 200;
const TAP_VIBRATION_MS = 10;
/** 연타하면 리플이 쌓인다. 화면에 동시에 남길 개수를 제한한다. */
const MAX_RIPPLES = 6;

type Ripple = {
  id: number;
  x: number;
  y: number;
};

type Props = {
  children: ReactNode;
  isGoal: boolean;
  onTap?: () => void;
};

const RacingGameOverlay = ({ children, isGoal, onTap }: Props) => {
  const { joinCode } = useIdentifier();
  const { send, isConnected } = useWebSocket();
  const { racingGameState } = useRacingGame();

  const tapCountRef = useRef(0);
  const intervalRef = useRef<number | null>(null);
  const rippleIdRef = useRef(0);
  const [ripples, setRipples] = useState<Ripple[]>([]);

  // 탭이 먹혔다는 신호를 서버 응답 전에 로컬에서 낸다. 서버는 200ms 인터벌과 왕복 뒤에야 답한다.
  const handlePointerDown = (event: PointerEvent<HTMLDivElement>) => {
    tapCountRef.current += 1;
    onTap?.();
    navigator.vibrate?.(TAP_VIBRATION_MS);

    const bounds = event.currentTarget.getBoundingClientRect();
    rippleIdRef.current += 1;
    const ripple = {
      id: rippleIdRef.current,
      x: event.clientX - bounds.left,
      y: event.clientY - bounds.top,
    };
    setRipples((prev) => [...prev, ripple].slice(-MAX_RIPPLES));
  };

  const removeRipple = (id: number) => {
    setRipples((prev) => prev.filter((ripple) => ripple.id !== id));
  };

  useEffect(() => {
    intervalRef.current = window.setInterval(() => {
      // 끊긴 동안 send 는 실패하면서 Sentry 이벤트를 남긴다. 200ms 마다 쌓이므로 아예 보내지 않고,
      // 그 사이 누른 탭도 버린다. 나중에 몰아서 보내면 서버가 받은 적 없는 속도로 뛴다.
      if (!isConnected || racingGameState !== 'PLAYING' || isGoal) {
        tapCountRef.current = 0;
        return;
      }

      const currentTapCount = tapCountRef.current;
      tapCountRef.current = 0;

      // playerName은 보내지 않는다 — 서버가 STOMP principal에서 도출한다.
      send(`/room/${joinCode}/racing-game/tap`, {
        tapCount: currentTapCount,
      });
    }, TAP_SEND_INTERVAL_MS);
    return () => {
      if (intervalRef.current) {
        window.clearInterval(intervalRef.current);
      }
    };
  }, [joinCode, send, racingGameState, isGoal, isConnected]);

  return (
    <S.Overlay
      data-testid="racing-game-overlay"
      role="application"
      aria-label="레이싱 게임. 화면을 빠르게 연타하면 앞으로 나갑니다"
      onPointerDown={handlePointerDown}
    >
      {children}
      {ripples.map(({ id, x, y }) => (
        // 좌표는 탭할 때마다 달라진다. styled prop 으로 넘기면 탭마다 클래스가 하나씩 생긴다.
        <S.Ripple key={id} style={{ left: x, top: y }} onAnimationEnd={() => removeRipple(id)} />
      ))}
      {!isConnected && <DisconnectOverlay />}
    </S.Overlay>
  );
};

export default RacingGameOverlay;
