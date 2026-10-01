import type {
  RacingGameRunnersStateResponse,
  RunnerPosition,
} from '@/apis/websocket/generated/wsContract';

export type { RacingGameState } from '@/apis/websocket/generated/wsContract';

export type RacingGameData = RacingGameRunnersStateResponse;

export type RacingPlayer = RunnerPosition;
