import { PropsWithChildren, useCallback, useState } from 'react';
import { RacingGameContext } from './RacingGameContext';
import { useWebSocketSubscription } from '@/apis/websocket/hooks/useWebSocketSubscription';
import { RacingGameData, RacingGameState } from '@/types/miniGame/racingGame';
import { useIdentifier } from '../Identifier/IdentifierContext';

const RacingGameProvider = ({ children }: PropsWithChildren) => {
  const [racingGameState, setRacingGameState] = useState<RacingGameState>('DESCRIPTION');
  const [racingGameData, setRacingGameData] = useState<RacingGameData>({
    players: [],
    // 첫 메시지가 오기 전에는 결승선 거리를 모른다. 임의의 숫자를 채우면 도착선이
    // 실제보다 앞에 그려지고 진행률도 틀리게 나온다. 0은 "아직 모름"이다.
    distance: {
      start: 0,
      end: 0,
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
