import { useWebSocketSubscription } from '@/apis/websocket/hooks/useWebSocketSubscription';
import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { LadderGameState, LadderLine, Pole } from '@/types/miniGame/ladderGame';
import { PropsWithChildren, useCallback, useEffect, useRef, useState } from 'react';
import { LadderGameContext } from './LadderGameContext';

type StateMessage =
  | { state: 'DESCRIPTION' | 'DONE' }
  | { state: 'PREPARE'; poles: Pole[]; bottomRanks: Record<string, number> }
  | { state: 'DRAWING'; endTimeEpochMs: number }
  | { state: 'RESULT'; rankings: Record<string, number>; animationDurationMs: number };

const LadderGameProvider = ({ children }: PropsWithChildren) => {
  const { joinCode, myName } = useIdentifier();
  const { send } = useWebSocket();

  const [gameState, setGameState] = useState<LadderGameState>('DESCRIPTION');
  const [poles, setPoles] = useState<Pole[]>([]);
  const [bottomRanks, setBottomRanks] = useState<Record<string, number>>({});
  const [lines, setLines] = useState<LadderLine[]>([]);
  const [ghostSegmentIndex, setGhostSegmentIndex] = useState<number | null>(null);
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
          setGhostSegmentIndex(null);
        }
      },
      [myName, clearGhostTimer]
    )
  );

  const drawLine = useCallback(
    (segmentIndex: number) => {
      clearGhostTimer();
      setGhostSegmentIndex(segmentIndex);
      send(`/room/${joinCode}/ladder/draw`, { playerName: myName, segmentIndex });

      ghostTimerRef.current = setTimeout(() => {
        setGhostSegmentIndex(null);
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
        lines,
        ghostSegmentIndex,
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
