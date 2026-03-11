import { useWebSocketSubscription } from '@/apis/websocket/hooks/useWebSocketSubscription';
import { PropsWithChildren, useCallback, useState } from 'react';
import { useIdentifier } from '../Identifier/IdentifierContext';
import { SpeedTouchGameContext } from './SpeedTouchGameContext';
import { SpeedTouchGameState, SpeedTouchProgressData } from '@/types/miniGame/speedTouchGame';

const SpeedTouchGameProvider = ({ children }: PropsWithChildren) => {
  const [gameState, setGameState] = useState<SpeedTouchGameState>('DESCRIPTION');
  const [progressData, setProgressData] = useState<SpeedTouchProgressData>({
    players: [],
  });
  const { joinCode } = useIdentifier();

  const handleStateChange = useCallback((data: { state: SpeedTouchGameState }) => {
    setGameState(data.state);
  }, []);

  const handleProgressUpdate = useCallback((data: SpeedTouchProgressData) => {
    setProgressData(data);
  }, []);

  useWebSocketSubscription(`/room/${joinCode}/speed-touch/state`, handleStateChange);
  useWebSocketSubscription(`/room/${joinCode}/speed-touch/progress`, handleProgressUpdate);

  return (
    <SpeedTouchGameContext.Provider value={{ gameState, progressData }}>
      {children}
    </SpeedTouchGameContext.Provider>
  );
};

export default SpeedTouchGameProvider;
