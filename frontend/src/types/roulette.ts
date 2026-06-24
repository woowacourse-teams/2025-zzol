import { ColorList } from '@/constants/color';

export type RouletteView = 'roulette' | 'statistics';

export type PlayerProbability = {
  playerName: string;
  probability: number;
  playerColor: ColorList;
};

export type ProbabilityHistory = {
  prev: PlayerProbability[];
  current: PlayerProbability[];
};

export type RouletteSector = {
  playerName: string;
  startAngle: number;
  endAngle: number;
  playerColor: ColorList;
};

export type RouletteWinnerResponse = {
  playerName: string;
  colorIndex: number;
  randomAngle: number;
};
