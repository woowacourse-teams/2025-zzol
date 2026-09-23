import { useWebSocketSubscription } from '@/apis/websocket/hooks/useWebSocketSubscription';
import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { LadderGameState, LadderGhost, LadderLine, Pole } from '@/types/miniGame/ladderGame';
import { PropsWithChildren, useCallback, useEffect, useRef, useState } from 'react';
import { LadderGameContext } from './LadderGameContext';

type StateMessage =
  | { state: 'DESCRIPTION' | 'DONE' }
  | { state: 'PREPARE'; poles: Pole[]; bottomRanks: Record<string, number>; rowCount: number }
  | { state: 'DRAWING'; endTimeEpochMs: number }
  | { state: 'RESULT'; rankings: Record<string, number>; animationDurationMs: number };

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

  const clearGhostTimer = useCallback(() => {
    if (ghostTimerRef.current) {
      clearTimeout(ghostTimerRef.current);
      ghostTimerRef.current = null;
    }
  }, []);

  useEffect(() => () => clearGhostTimer(), [clearGhostTimer]);

  useWebSocketSubscription<StateMessage>(
    `/room/${joinCode}/ladder/state`,
    useCallback((msg: StateMessage) => {
      setGameState(msg.state);
      if (msg.state === 'PREPARE') {
        setPoles(msg.poles);
        setBottomRanks(msg.bottomRanks);
        setRowCount(msg.rowCount);
      } else if (msg.state === 'DRAWING') {
        setEndTimeEpochMs(msg.endTimeEpochMs);
      } else if (msg.state === 'RESULT') {
        setRankings(msg.rankings);
        setAnimationDurationMs(msg.animationDurationMs);
      }
    }, [])
  );

  useWebSocketSubscription<LadderLine>(
    `/room/${joinCode}/ladder/line`,
    useCallback(
      (line: LadderLine) => {
        setLines((prev) => [...prev, line]);
        if (line.playerName === myName) {
          clearGhostTimer();
          setGhost(null);
        }
      },
      [myName, clearGhostTimer]
    )
  );

  const drawLine = useCallback(
    (segmentIndex: number, row: number) => {
      clearGhostTimer();
      setGhost({ segmentIndex, row });
      send(`/room/${joinCode}/ladder/draw`, { playerName: myName, segmentIndex, row });

      // 같은 자리를 다른 플레이어가 먼저 차지하면 서버가 조용히 무시한다. 그때 ghost 를 거둔다.
      ghostTimerRef.current = setTimeout(() => {
        setGhost(null);
      }, 2000);
    },
    [clearGhostTimer, send, joinCode, myName]
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
      }}
    >
      {children}
    </LadderGameContext.Provider>
  );
};

export default LadderGameProvider;
