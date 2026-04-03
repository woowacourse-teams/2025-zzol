import { useWebSocketSubscription } from '@/apis/websocket/hooks/useWebSocketSubscription';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { BlockStackingGameState } from '@/types/miniGame/blockStackingGame';
import { PropsWithChildren, useCallback, useState } from 'react';
import { BlockStackingGameContext } from './BlockStackingGameContext';

type BlockStackingRanking = { name: string; floor: number };

const BlockStackingGameProvider = ({ children }: PropsWithChildren) => {
  const { joinCode } = useIdentifier();
  const [gameState, setGameState] = useState<BlockStackingGameState>('DESCRIPTION');
  const [rankings, setRankings] = useState<BlockStackingRanking[]>([]);
  const [isLocalGameOver, setIsLocalGameOver] = useState(false);

  const setLocalGameOver = useCallback(() => setIsLocalGameOver(true), []);

  useWebSocketSubscription(
    `/room/${joinCode}/block-stacking/state`,
    useCallback(({ state }: { state: BlockStackingGameState }) => {
      setGameState(state);
      if (state === 'PLAYING') setIsLocalGameOver(false);
    }, [])
  );

  useWebSocketSubscription(
    `/room/${joinCode}/block-stacking/progress`,
    useCallback(({ players }: { players: BlockStackingRanking[] }) => {
      setRankings(players);
    }, [])
  );

  useWebSocketSubscription(
    `/room/${joinCode}/block-stacking/complete`,
    useCallback(() => {
      setGameState('DONE');
    }, [])
  );

  return (
    <BlockStackingGameContext.Provider value={{ gameState, rankings, isLocalGameOver, setLocalGameOver }}>
      {children}
    </BlockStackingGameContext.Provider>
  );
};

export default BlockStackingGameProvider;
