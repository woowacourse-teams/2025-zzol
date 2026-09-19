import {
  formatGlobalDiff,
  formatPercentile,
  formatRecord,
  formatWinRate,
  globalBarRatio,
  recordLabels,
  winRatePercent,
} from './formatRecord';

describe('formatRecord', () => {
  it.each([
    ['RACING_GAME', 12340, '12.34초'],
    ['SPEED_TOUCH', 9005, '9.01초'],
    ['BLIND_TIMER', 120, '0.12초'],
    ['BLOCK_STACKING', 14, '14층'],
  ] as const)('%s 의 %p 를 %p 로 찍는다', (type, value, expected) => {
    expect(formatRecord(type, value)).toBe(expected);
  });
});

describe('formatGlobalDiff', () => {
  it.each([
    ['RACING_GAME', 14020, 15800, '전체 평균보다 1.78초 빨라요'],
    ['RACING_GAME', 15800, 15280, '전체 평균보다 0.52초 느려요'],
    ['SPEED_TOUCH', 9000, 9500, '전체 평균보다 0.50초 빨라요'],
    ['BLIND_TIMER', 410, 520, '전체 평균보다 0.11초 정확해요'],
    ['BLIND_TIMER', 520, 410, '전체 평균보다 오차가 0.11초 커요'],
    ['BLOCK_STACKING', 9, 7, '전체 평균보다 2층 높아요'],
    ['BLOCK_STACKING', 5, 7, '전체 평균보다 2층 낮아요'],
  ] as const)(
    '%s 의 내 평균 %p 와 전체 평균 %p 차이를 %p 로 적는다',
    (type, avg, global, expected) => {
      expect(formatGlobalDiff(type, avg, global)).toBe(expected);
    }
  );

  it('내 평균과 전체 평균이 같으면 같다고 적는다', () => {
    expect(formatGlobalDiff('RACING_GAME', 12340, 12340)).toBe('전체 평균과 같아요');
    expect(formatGlobalDiff('BLOCK_STACKING', 7, 7)).toBe('전체 평균과 같아요');
  });
});

describe('globalBarRatio', () => {
  it('시간 게임은 짧은 쪽이 1 이다', () => {
    expect(globalBarRatio('RACING_GAME', 5000, 10000)).toEqual({ mine: 1, global: 0.5 });
    expect(globalBarRatio('BLIND_TIMER', 10000, 5000)).toEqual({ mine: 0.5, global: 1 });
  });

  it('블록은 높은 쪽이 1 이다', () => {
    expect(globalBarRatio('BLOCK_STACKING', 10, 5)).toEqual({ mine: 1, global: 0.5 });
    expect(globalBarRatio('BLOCK_STACKING', 5, 10)).toEqual({ mine: 0.5, global: 1 });
  });

  it('둘 다 0 이면 0 으로 나누지 않고 꽉 채운다', () => {
    expect(globalBarRatio('BLIND_TIMER', 0, 0)).toEqual({ mine: 1, global: 1 });
  });
});

describe('formatPercentile', () => {
  it('상위 % 문구를 만든다', () => {
    expect(formatPercentile(23)).toBe('상위 23%에요');
  });
});

describe('winRatePercent', () => {
  it('정수 % 로 반올림하고 참여 0판이면 null 이다', () => {
    expect(winRatePercent(1, 3)).toBe(33);
    expect(winRatePercent(0, 0)).toBeNull();
  });
});

describe('formatWinRate', () => {
  it.each([
    [3, 14, '21%'],
    [1, 3, '33%'],
    [2, 3, '67%'],
    [0, 5, '0%'],
    [5, 5, '100%'],
  ])('%p 승 / %p 판 은 %p', (win, play, expected) => {
    expect(formatWinRate(win, play)).toBe(expected);
  });

  it('참여 0판이면 - 다', () => {
    expect(formatWinRate(0, 0)).toBe('-');
  });
});

describe('recordLabels', () => {
  it('초시계만 오차 라벨을 쓴다', () => {
    expect(recordLabels('BLIND_TIMER')).toEqual({ best: '최소 오차', average: '평균 오차' });
    expect(recordLabels('RACING_GAME')).toEqual({ best: '최고 기록', average: '평균 기록' });
  });
});
