import { useWebSocketSubscription } from '@/apis/websocket/hooks/useWebSocketSubscription';
import { PropsWithChildren, useCallback, useState } from 'react';
import { useIdentifier } from '../Identifier/IdentifierContext';
import { BlindTimerGameContext } from './BlindTimerGameContext';
import {
  BlindTimerGameState,
  BlindTimerProgressData,
  BlindTimerStateData,
} from '@/types/miniGame/blindTimerGame';

const BlindTimerGameProvider = ({ children }: PropsWithChildren) => {
  const [gameState, setGameState] = useState<BlindTimerGameState>('DESCRIPTION');
  const [targetTimeMillis, setTargetTimeMillis] = useState(0);
  const [blindDelayMillis, setBlindDelayMillis] = useState(3000);
  const [progressData, setProgressData] = useState<BlindTimerProgressData>({
    players: [],
  });
  const { joinCode } = useIdentifier();

  const handleStateChange = useCallback((data: BlindTimerStateData) => {
    setGameState(data.state);
    if (data.targetTimeMillis > 0) {
      setTargetTimeMillis(data.targetTimeMillis);
    }
    if (data.blindDelayMillis > 0) {
      setBlindDelayMillis(data.blindDelayMillis);
    }
  }, []);

  const handleProgressUpdate = useCallback((data: BlindTimerProgressData) => {
    setProgressData(data);
  }, []);

  useWebSocketSubscription(`/room/${joinCode}/blind-timer/state`, handleStateChange);
  useWebSocketSubscription(`/room/${joinCode}/blind-timer/progress`, handleProgressUpdate);

  return (
    <BlindTimerGameContext.Provider
      value={{ gameState, targetTimeMillis, blindDelayMillis, progressData }}
    >
      {children}
    </BlindTimerGameContext.Provider>
  );
};

export default BlindTimerGameProvider;
