import {
  formatBeatShare,
  formatRecord,
  formatWinRate,
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

describe('formatBeatShare', () => {
  it.each([
    ['RACING_GAME', 23, 41, '회원 41명 중 77%보다 빨라요'],
    ['SPEED_TOUCH', 50, 10, '회원 10명 중 50%보다 빨라요'],
    ['BLIND_TIMER', 23, 41, '회원 41명 중 77%보다 정확해요'],
    ['BLOCK_STACKING', 23, 41, '회원 41명 중 77%보다 높이 쌓았어요'],
  ] as const)('%s 상위 %p%% 를 회원 %p명 기준 %p 로 적는다', (type, pct, members, expected) => {
    expect(formatBeatShare(type, pct, members)).toBe(expected);
  });

  it('1등과 꼴찌는 % 대신 말로 적는다', () => {
    expect(formatBeatShare('RACING_GAME', 1, 41)).toBe('회원 41명 중 1등이에요');
    expect(formatBeatShare('BLOCK_STACKING', 100, 41)).toBe('회원 41명 중 가장 낮아요');
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
    expect(recordLabels('BLIND_TIMER')).toEqual({
      best: '최소 오차',
      average: '내 평균 오차',
      global: '전체 평균 오차',
    });
    expect(recordLabels('RACING_GAME')).toEqual({
      best: '최고 기록',
      average: '내 평균',
      global: '전체 평균',
    });
  });
});
