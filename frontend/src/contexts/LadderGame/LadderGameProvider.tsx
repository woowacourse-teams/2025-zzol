import { useWebSocketSubscription } from '@/apis/websocket/hooks/useWebSocketSubscription';
import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { LadderGameState, LadderGhost, LadderLine, Pole } from '@/types/miniGame/ladderGame';
import { PropsWithChildren, useCallback, useEffect, useRef, useState } from 'react';
import { LadderGameContext } from './LadderGameContext';
import type { LadderStateResponse } from '@/apis/websocket/generated/wsContract';

const LadderGameProvider = ({ children }: PropsWithChildren) => {
  const { joinCode, myName } = useIdentifier();
  const { send } = useWebSocket();

  const [gameState, setGameState] = useState<LadderGameState>('DESCRIPTION');
  const [poles, setPoles] = useState<Pole[]>([]);
  const [bottomRanks, setBottomRanks] = useState<Record<string, number>>({});
  const [rowCount, setRowCount] = useState(0);
  const [lines, setLines] = useState<LadderLine[]>([]);
  const [ghost, setGhost] = useState<LadderGhost | null>(null);
  const [endTimeEpochMs, setEndTimeEpochMs] = useState<number | null>(null);
  const [rankings, setRankings] = useState<Record<string, number> | null>(null);
  const [animationDurationMs, setAnimationDurationMs] = useState<number | null>(null);

  const ghostTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  // 구독 콜백이 최신 ghost 를 읽게 state 와 함께 둔다
  const ghostRef = useRef<LadderGhost | null>(null);

  const clearGhostTimer = useCallback(() => {
    if (ghostTimerRef.current) {
      clearTimeout(ghostTimerRef.current);
      ghostTimerRef.current = null;
    }
  }, []);

  useEffect(() => () => clearGhostTimer(), [clearGhostTimer]);

  const dropGhost = useCallback(() => {
    clearGhostTimer();
    ghostRef.current = null;
    setGhost(null);
  }, [clearGhostTimer]);

  useWebSocketSubscription(
    `/room/${joinCode}/ladder/state`,
    useCallback((msg: LadderStateResponse) => {
      setGameState(msg.state);
      if (msg.state === 'PREPARE') {
        setPoles(msg.poles ?? []);
        setBottomRanks(msg.bottomRanks ?? {});
        setRowCount(msg.rowCount ?? 0);
      } else if (msg.state === 'DRAWING') {
        setEndTimeEpochMs(msg.endTimeEpochMs ?? null);
      } else if (msg.state === 'RESULT') {
        setRankings(msg.rankings ?? null);
        setAnimationDurationMs(msg.animationDurationMs ?? null);
      }
    }, [])
  );

  useWebSocketSubscription(
    `/room/${joinCode}/ladder/line`,
    useCallback(
      (line: LadderLine) => {
        setLines((prev) => [...prev, line]);
        // 내 선이라도 지금 ghost 자리의 응답일 때만 거둔다. 타임아웃 뒤 늦게 온 앞선 응답이 새 ghost 를 지우지 않게 한다
        const pending = ghostRef.current;
        const isPendingLine =
          line.playerName === myName &&
          pending !== null &&
          Number(line.segmentIndex) === pending.segmentIndex &&
          line.row === pending.row;
        if (isPendingLine) dropGhost();
      },
      [myName, dropGhost]
    )
  );

  const drawLine = useCallback(
    (segmentIndex: number, row: number) => {
      clearGhostTimer();
      const next = { segmentIndex, row };
      ghostRef.current = next;
      setGhost(next);
      send(`/room/${joinCode}/ladder/draw`, { playerName: myName, segmentIndex, row });

      // 서버가 조용히 무시한 요청(범위 밖 등)은 응답이 없다. 그때 ghost 를 거둔다
      ghostTimerRef.current = setTimeout(dropGhost, 2000);
    },
    [clearGhostTimer, dropGhost, send, joinCode, myName]
  );

  return (
    <LadderGameContext.Provider
      value={{
        gameState,
        poles,
        bottomRanks,
        rowCount,
        lines,
        ghost,
        endTimeEpochMs,
        rankings,
        animationDurationMs,
        drawLine,
        dropGhost,
      }}
    >
      {children}
    </LadderGameContext.Provider>
  );
};

export default LadderGameProvider;
