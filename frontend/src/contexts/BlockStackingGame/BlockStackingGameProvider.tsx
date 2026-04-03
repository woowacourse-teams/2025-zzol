import { useWebSocketSubscription } from '@/apis/websocket/hooks/useWebSocketSubscription';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { BlockStackingGameState } from '@/types/miniGame/blockStackingGame';
import { PropsWithChildren, useCallback, useState } from 'react';
import { BlockStackingGameContext } from './BlockStackingGameContext';

type BlockStackingRanking = { name: string; floor: number };

type StateMessage = {
  state: BlockStackingGameState;
  endTimeEpochMs?: number | null;
};

const BlockStackingGameProvider = ({ children }: PropsWithChildren) => {
  const { joinCode } = useIdentifier();
  const [gameState, setGameState] = useState<BlockStackingGameState>('DESCRIPTION');
  const [rankings, setRankings] = useState<BlockStackingRanking[]>([]);
  const [isLocalGameOver, setIsLocalGameOver] = useState(false);
  const [endTimeEpochMs, setEndTimeEpochMs] = useState<number | null>(null);

  const setLocalGameOver = useCallback(() => setIsLocalGameOver(true), []);

  useWebSocketSubscription(
    `/room/${joinCode}/block-stacking/state`,
    useCallback(({ state, endTimeEpochMs: ms }: StateMessage) => {
      setGameState(state);
      if (state === 'PLAYING') {
        setIsLocalGameOver(false);
        if (ms != null) setEndTimeEpochMs(ms);
      }
    }, [])
  );

  useWebSocketSubscription(
    `/room/${joinCode}/block-stacking/progress`,
    useCallback(({ players }: { players: BlockStackingRanking[] }) => {
      setRankings(players);
    }, [])
  );

  return (
    <BlockStackingGameContext.Provider
      value={{ gameState, rankings, isLocalGameOver, setLocalGameOver, endTimeEpochMs }}
    >
      {children}
    </BlockStackingGameContext.Provider>
  );
};

export default BlockStackingGameProvider;
