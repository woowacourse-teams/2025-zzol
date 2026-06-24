import { PropsWithChildren, useCallback, useState } from 'react';
import { RacingGameContext } from './RacingGameContext';
import { useWebSocketSubscription } from '@/apis/websocket/hooks/useWebSocketSubscription';
import { RacingGameData, RacingGameState } from '@/types/miniGame/racingGame';
import { useIdentifier } from '../Identifier/IdentifierContext';

const RacingGameProvider = ({ children }: PropsWithChildren) => {
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
    <RacingGameContext.Provider value={{ racingGameState, racingGameData }}>
      {children}
    </RacingGameContext.Provider>
  );
};

export default RacingGameProvider;
