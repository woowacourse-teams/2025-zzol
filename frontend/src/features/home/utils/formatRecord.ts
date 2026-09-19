import type { RecordGameType } from '@/types/records';

/** 밀리초를 소수점 2자리 초로 변환한다 (예: 18230 → 18.23). */
export const millisToSeconds = (millis: number) => Math.round(millis / 10) / 100;

const isFloorGame = (type: RecordGameType) => type === 'BLOCK_STACKING';

/** 저장 단위 값을 화면 표기로 바꾼다. 시간 게임은 `12.34초`, 블록은 `14층`. */
export const formatRecord = (type: RecordGameType, value: number) =>
  isFloorGame(type) ? `${value}층` : `${millisToSeconds(value).toFixed(2)}초`;

/**
 * 내 평균과 전체 평균의 차이 문구.
 * 시간 게임은 빨라요·느려요, 초시계는 정확해요·오차가 커요, 블록은 높아요·낮아요.
 */
export const formatGlobalDiff = (type: RecordGameType, average: number, globalAverage: number) => {
  if (average === globalAverage) return '전체 평균과 같아요';
  if (isFloorGame(type)) {
    const floors = Math.abs(average - globalAverage);
    return average > globalAverage
      ? `전체 평균보다 ${floors}층 높아요`
      : `전체 평균보다 ${floors}층 낮아요`;
  }
  const seconds = millisToSeconds(Math.abs(average - globalAverage)).toFixed(2);
  const better = average < globalAverage;
  if (type === 'BLIND_TIMER') {
    return better
      ? `전체 평균보다 ${seconds}초 정확해요`
      : `전체 평균보다 오차가 ${seconds}초 커요`;
  }
  return better ? `전체 평균보다 ${seconds}초 빨라요` : `전체 평균보다 ${seconds}초 느려요`;
};

/**
 * 내 평균 대 전체 평균 막대의 두 채움 비율(0~1). 좋은 쪽이 항상 1 이고 나쁜 쪽이 그 비율이다.
 * 시간·오차 게임은 작을수록 좋고, 블록은 클수록 좋다.
 */
export const globalBarRatio = (type: RecordGameType, average: number, globalAverage: number) => {
  const [small, large] = [Math.min(average, globalAverage), Math.max(average, globalAverage)];
  const ratio = large === 0 ? 1 : small / large;
  const mineIsGood = isFloorGame(type) ? average === large : average === small;
  return mineIsGood ? { mine: 1, global: ratio } : { mine: ratio, global: 1 };
};

/** 상위 % 배지 문구. */
export const formatPercentile = (percentile: number) => `상위 ${percentile}%에요`;

/** 당첨 확률 정수 %. 참여 0판이면 null. */
export const winRatePercent = (winCount: number, playCount: number) =>
  playCount === 0 ? null : Math.round((winCount / playCount) * 100);

/** 당첨 확률 표기. 참여 0판이면 `-`. */
export const formatWinRate = (winCount: number, playCount: number) => {
  const percent = winRatePercent(winCount, playCount);
  return percent === null ? '-' : `${percent}%`;
};

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
