export type RacingGameState = 'DESCRIPTION' | 'PREPARE' | 'PLAYING' | 'DONE';

export type RacingGameData = {
  distance: {
    start: number;
    end: number;
  };
  players: RacingPlayer[];
};

export type RacingPlayer = {
  playerName: string;
  position: number;
  speed: number;
};
