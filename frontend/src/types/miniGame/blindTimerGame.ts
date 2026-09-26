import type {
  BlindTimerProgressResponse,
  BlindTimerStateResponse,
} from '@/apis/websocket/generated/wsContract';

export type {
  BlindTimerGameState,
  BlindTimerPlayerProgress,
} from '@/apis/websocket/generated/wsContract';

export type BlindTimerStateData = BlindTimerStateResponse;

export type BlindTimerProgressData = BlindTimerProgressResponse;
