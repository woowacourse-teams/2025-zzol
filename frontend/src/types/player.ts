export type PlayerType = 'HOST' | 'GUEST';

export type Player = {
  userId: number;
  playerName: string;
  playerType: PlayerType;
  isReady: boolean;
  colorIndex: number;
  probability: number;
};
