import { PropsWithChildren, useCallback, useState } from 'react';
import { useWebSocketSubscription } from '@/apis/websocket/hooks/useWebSocketSubscription';
import { RacingGameData, RacingGameState } from '@/types/miniGame/racingGame';
import { useIdentifier } from '../Identifier/IdentifierContext';
import { RacingGameProvider } from './RacingGameContext';

const RacingGameProviderWrapper = ({ children }: PropsWithChildren) => {
  const [racingGameState, setRacingGameState] = useState<RacingGameState>('DESCRIPTION');
  const [racingGameData, setRacingGameData] = useState<RacingGameData>({
    players: [],
    distance: {
      start: 0,
      end: 1000,
    },
  });
  const { joinCode } = useIdentifier();

  const handleRacingGameState = useCallback((data: { state: RacingGameState }) => {
    setRacingGameState(data.state);
  }, []);
  const handleRacingGameData = useCallback((data: RacingGameData) => {
    setRacingGameData(data);
  }, []);

  useWebSocketSubscription(`/room/${joinCode}/racing-game/state`, handleRacingGameState);

  useWebSocketSubscription(`/room/${joinCode}/racing-game`, handleRacingGameData);

  return (
    <RacingGameProvider value={{ racingGameState, racingGameData }}>{children}</RacingGameProvider>
  );
};

export default RacingGameProviderWrapper;
