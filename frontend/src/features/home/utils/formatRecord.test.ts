import {
  formatRecord,
  formatRecordDiff,
  formatWinRate,
  recordBarRatio,
  recordLabels,
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

describe('formatRecordDiff', () => {
  it.each([
    ['RACING_GAME', 12340, 14020, '최고 기록이 평균보다 1.68초 빨라요'],
    ['SPEED_TOUCH', 9000, 9500, '최고 기록이 평균보다 0.50초 빨라요'],
    ['BLIND_TIMER', 120, 410, '최소 오차가 평균보다 0.29초 정확해요'],
    ['BLOCK_STACKING', 14, 9, '최고 기록이 평균보다 5층 높아요'],
  ] as const)('%s 의 최고 %p 와 평균 %p 차이를 %p 로 적는다', (type, best, avg, expected) => {
    expect(formatRecordDiff(type, best, avg)).toBe(expected);
  });

  it('최고와 평균이 같으면 null 이다', () => {
    expect(formatRecordDiff('RACING_GAME', 12340, 12340)).toBeNull();
    expect(formatRecordDiff('BLOCK_STACKING', 7, 7)).toBeNull();
  });
});

describe('recordBarRatio', () => {
  it('시간 게임은 최고가 짧을수록 좋아 최고 막대가 평균 대비 비율이다', () => {
    expect(recordBarRatio('RACING_GAME', 5000, 10000)).toEqual({ best: 0.5, average: 1 });
  });

  it('블록은 높을수록 좋아 평균 막대가 최고 대비 비율이다', () => {
    expect(recordBarRatio('BLOCK_STACKING', 10, 5)).toEqual({ best: 1, average: 0.5 });
  });

  it('둘 다 0 이면 0 으로 나누지 않고 꽉 채운다', () => {
    expect(recordBarRatio('BLIND_TIMER', 0, 0)).toEqual({ best: 1, average: 1 });
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
