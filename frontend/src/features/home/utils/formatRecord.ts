import type { RecordGameType } from '@/types/records';

/** 밀리초를 소수점 2자리 초로 변환한다 (예: 18230 → 18.23). */
export const millisToSeconds = (millis: number) => Math.round(millis / 10) / 100;

const isFloorGame = (type: RecordGameType) => type === 'BLOCK_STACKING';

/** 저장 단위 값을 화면 표기로 바꾼다. 시간 게임은 `12.34초`, 블록은 `14층`. */
export const formatRecord = (type: RecordGameType, value: number) =>
  isFloorGame(type) ? `${value}층` : `${millisToSeconds(value).toFixed(2)}초`;

/** 최고와 평균의 차이 문구. 같으면 null 이라 막대와 함께 숨긴다. */
export const formatRecordDiff = (type: RecordGameType, best: number, average: number) => {
  if (best === average) return null;
  if (isFloorGame(type)) return `최고 기록이 평균보다 ${best - average}층 높아요`;
  const seconds = millisToSeconds(Math.abs(average - best)).toFixed(2);
  return type === 'BLIND_TIMER'
    ? `최소 오차가 평균보다 ${seconds}초 정확해요`
    : `최고 기록이 평균보다 ${seconds}초 빨라요`;
};

/**
 * 차이 막대의 두 채움 비율(0~1). 좋은 쪽이 항상 1 이고 나쁜 쪽이 그 비율이다.
 * 시간·오차 게임은 작을수록 좋아 best/average, 블록은 클수록 좋아 average/best.
 */
export const recordBarRatio = (type: RecordGameType, best: number, average: number) => {
  if (best === 0 && average === 0) return { best: 1, average: 1 };
  return isFloorGame(type)
    ? { best: 1, average: average / best }
    : { best: best / average, average: 1 };
};

/** 당첨 확률 정수 %. 참여 0판이면 `-`. */
export const formatWinRate = (winCount: number, playCount: number) =>
  playCount === 0 ? '-' : `${Math.round((winCount / playCount) * 100)}%`;

export const RECORD_HINT: Record<RecordGameType, string> = {
  RACING_GAME: '완주 시간이 짧을수록 좋아요',
  BLOCK_STACKING: '높이 쌓을수록 좋아요',
  BLIND_TIMER: '오차가 작을수록 좋아요',
  SPEED_TOUCH: '빠르게 누를수록 좋아요',
};

export const recordLabels = (type: RecordGameType) =>
  type === 'BLIND_TIMER'
    ? { best: '최소 오차', average: '평균 오차' }
    : { best: '최고 기록', average: '평균 기록' };
