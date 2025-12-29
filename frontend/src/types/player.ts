export type PlayerType = 'HOST' | 'GUEST';

export type Player = {
  playerName: string;
  playerType: PlayerType;
  isReady: boolean;
  colorIndex: number;
  probability: number;
};
