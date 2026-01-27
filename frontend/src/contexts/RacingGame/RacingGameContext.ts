import { RacingGameData, RacingGameState } from '@/types/miniGame/racingGame';
import { createContextSelector } from '@/utils/createContextSelector';
import { useCallback, useRef } from 'react';

type RacingGameContextType = {
  racingGameState: RacingGameState;
  racingGameData: RacingGameData;
};
const { Provider, useContextSelector } = createContextSelector<RacingGameContextType>();

export const RacingGameProvider = Provider;

export const useRacingGameState = () =>
  useContextSelector((state: RacingGameContextType) => state.racingGameState);
export const useRacingGameData = () =>
  useContextSelector((state: RacingGameContextType) => state.racingGameData);

type RankedPlayer = {
  playerName: string;
  position: number;
  isFinished: boolean;
};

export const useRacingGameRankedPlayers = () => {
  const finishOrderRef = useRef<RankedPlayer[]>([]);
  const finishedPlayerNamesRef = useRef<Set<string>>(new Set());
  const previousResultRef = useRef<RankedPlayer[]>([]);

  const selector = useCallback((state: RacingGameContextType) => {
    const finishOrder = finishOrderRef.current;
    const { players, distance } = state.racingGameData;

    players.forEach(({ playerName, position }) => {
      if (position >= distance.end && !finishedPlayerNamesRef.current.has(playerName)) {
        finishedPlayerNamesRef.current.add(playerName);
        finishOrder.push({ playerName, position, isFinished: true });
      }
    });

    const unFinishedSortedPlayers = players
      .filter((player) => !finishedPlayerNamesRef.current.has(player.playerName))
      .sort((a, b) => b.position - a.position)
      .map((player) => ({
        playerName: player.playerName,
        position: player.position,
        isFinished: false,
      }));

    const newRankedPlayers = [...finishOrder, ...unFinishedSortedPlayers];

    const hasRankChanged = !previousResultRef.current.some(
      (prevPlayer, index) => prevPlayer.playerName === newRankedPlayers[index]?.playerName
    );

    if (!hasRankChanged) {
      return previousResultRef.current;
    }

    previousResultRef.current = newRankedPlayers;
    return newRankedPlayers;
  }, []);

  return useContextSelector(selector);
};

//호환성을 위해 남겨둠
export const useRacingGame = () => {
  const racingGameState = useRacingGameState();
  const racingGameData = useRacingGameData();
  return { racingGameState, racingGameData };
};
