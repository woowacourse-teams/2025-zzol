import type { RecordGameType } from '@/types/records';

/** 밀리초를 소수점 2자리 초로 변환한다 (예: 18230 → 18.23). */
export const millisToSeconds = (millis: number) => Math.round(millis / 10) / 100;

const isFloorGame = (type: RecordGameType) => type === 'BLOCK_STACKING';

/** 저장 단위 값을 화면 표기로 바꾼다. 시간 게임은 `12.34초`, 블록은 `14층`. */
export const formatRecord = (type: RecordGameType, value: number) =>
  isFloorGame(type) ? `${value}층` : `${millisToSeconds(value).toFixed(2)}초`;

/**
 * 상위 % 를 "몇 %보다 나은가"로 뒤집은 문구. 상위 23% 면 77% 보다 낫다.
 * 1등·꼴찌는 0%·99% 대신 말로 적는다.
 */
export const formatBeatShare = (type: RecordGameType, percentile: number, memberCount: number) => {
  const head = `회원 ${memberCount}명 중`;
  if (percentile === 1) return `${head} 1등이에요`;
  if (percentile === 100) return `${head} 가장 낮아요`;
  const verb = type === 'BLIND_TIMER' ? '정확해요' : isFloorGame(type) ? '높이 쌓았어요' : '빨라요';
  return `${head} ${100 - percentile}%보다 ${verb}`;
};

/** 당첨 확률 정수 %. 참여 0판이면 null. */
export const winRatePercent = (winCount: number, playCount: number) =>
  playCount === 0 ? null : Math.round((winCount / playCount) * 100);

/** 당첨 확률 표기. 참여 0판이면 `-`. */
export const formatWinRate = (winCount: number, playCount: number) => {
  const percent = winRatePercent(winCount, playCount);
  return percent === null ? '-' : `${percent}%`;
};

export const recordLabels = (type: RecordGameType) =>
  type === 'BLIND_TIMER'
    ? { best: '최소 오차', average: '내 평균 오차', global: '전체 평균 오차' }
    : { best: '최고 기록', average: '내 평균', global: '전체 평균' };
