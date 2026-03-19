import { useWebSocketSubscription } from '@/apis/websocket/hooks/useWebSocketSubscription';
import { PropsWithChildren, useCallback, useState } from 'react';
import { useIdentifier } from '../Identifier/IdentifierContext';
import { BombRelayGameContext } from './BombRelayGameContext';
import {
  BombRelayGameState,
  BombRelayProgressData,
  BombRelayStateData,
  BombRelayWordResult,
} from '@/types/miniGame/bombRelayGame';

const BombRelayGameProvider = ({ children }: PropsWithChildren) => {
  const [gameState, setGameState] = useState<BombRelayGameState>('DESCRIPTION');
  const [currentRound, setCurrentRound] = useState(0);
  const [maxRounds, setMaxRounds] = useState(0);
  const [currentWord, setCurrentWord] = useState('');
  const [currentTurnPlayerName, setCurrentTurnPlayerName] = useState('');
  const [eliminatedPlayerName, setEliminatedPlayerName] = useState<string | null>(null);
  const [progressData, setProgressData] = useState<BombRelayProgressData>({
    currentWord: '',
    currentTurnPlayerName: '',
    currentRound: 0,
    players: [],
  });
  const [lastWordResult, setLastWordResult] = useState<BombRelayWordResult | null>(null);
  const { joinCode } = useIdentifier();

  const handleStateChange = useCallback((data: BombRelayStateData) => {
    setGameState(data.state);
    setCurrentRound(data.currentRound);
    setMaxRounds(data.maxRounds);
    setEliminatedPlayerName(data.eliminatedPlayerName);
    if (data.currentWord) {
      setCurrentWord(data.currentWord);
    }
    if (data.currentTurnPlayerName) {
      setCurrentTurnPlayerName(data.currentTurnPlayerName);
    }
  }, []);

  const handleProgressUpdate = useCallback((data: BombRelayProgressData) => {
    setProgressData(data);
    setCurrentWord(data.currentWord);
    setCurrentTurnPlayerName(data.currentTurnPlayerName);
    setCurrentRound(data.currentRound);
  }, []);

  const handleWordResult = useCallback((data: BombRelayWordResult) => {
    setLastWordResult(data);
  }, []);

  useWebSocketSubscription(`/room/${joinCode}/bomb-relay/state`, handleStateChange);
  useWebSocketSubscription(`/room/${joinCode}/bomb-relay/progress`, handleProgressUpdate);
  useWebSocketSubscription(`/room/${joinCode}/bomb-relay/word-result`, handleWordResult);

  return (
    <BombRelayGameContext.Provider
      value={{
        gameState,
        currentRound,
        maxRounds,
        currentWord,
        currentTurnPlayerName,
        eliminatedPlayerName,
        progressData,
        lastWordResult,
      }}
    >
      {children}
    </BombRelayGameContext.Provider>
  );
};

export default BombRelayGameProvider;
