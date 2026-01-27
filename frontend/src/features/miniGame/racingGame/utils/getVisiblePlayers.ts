import { RacingPlayer } from '@/types/miniGame/racingGame';

const VISIBLE_PLAYER_COUNT = 7;
const HALF_COUNT = 3;

export const getVisiblePlayers = (players: RacingPlayer[], myName: string): RacingPlayer[] => {
  if (players.length === 0) return [];

  const totalPlayers = players.length;

  const playerIndexMap = new Map<string, number>();
  for (let i = 0; i < totalPlayers; i++) {
    playerIndexMap.set(players[i].playerName, i);
  }

  const myIndex = playerIndexMap.get(myName);
  if (myIndex === undefined) return [];

  if (totalPlayers <= VISIBLE_PLAYER_COUNT) {
    const result: RacingPlayer[] = [];
    const halfCount = Math.floor(totalPlayers / 2);

    for (let i = 0; i < totalPlayers; i++) {
      const offset = i - halfCount;
      const sourcePlayerIndex = (myIndex + offset + totalPlayers) % totalPlayers;
      result.push({ ...players[sourcePlayerIndex] });
    }

    return result;
  }

  const result: RacingPlayer[] = [];

  for (let i = 0; i < VISIBLE_PLAYER_COUNT; i++) {
    const offset = i - HALF_COUNT;
    const sourcePlayerIndex = (myIndex + offset + totalPlayers) % totalPlayers;
    result.push({ ...players[sourcePlayerIndex] });
  }

  return result;
};
