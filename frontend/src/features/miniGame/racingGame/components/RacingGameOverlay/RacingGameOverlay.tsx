import { ReactNode, useRef, useEffect } from 'react';
import * as S from './RacingGameOverlay.styled';
import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useRacingGame } from '@/contexts/RacingGame/RacingGameContext';

const TAP_SEND_INTERVAL_MS = 200;

type Props = {
  children: ReactNode;
  isGoal: boolean;
};

const RacingGameOverlay = ({ children, isGoal }: Props) => {
  const { joinCode } = useIdentifier();
  const { send, isConnected } = useWebSocket();
  const { racingGameState } = useRacingGame();

  const tapCountRef = useRef(0);
  const intervalRef = useRef<number | null>(null);

  const handlePointerDown = () => {
    tapCountRef.current += 1;
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
    <S.Overlay data-testid="racing-game-overlay" onPointerDown={handlePointerDown}>
      {children}
      {!isConnected && (
        <S.DisconnectBanner role="status" aria-live="polite">
          <S.DisconnectTitle>연결이 끊겼습니다. 다시 연결 중...</S.DisconnectTitle>
          <S.DisconnectHint>그동안 누른 탭은 서버로 가지 않습니다.</S.DisconnectHint>
        </S.DisconnectBanner>
      )}
    </S.Overlay>
  );
};

export default RacingGameOverlay;
