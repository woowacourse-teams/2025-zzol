import { canAcceptTap } from './canAcceptTap';

const playing = { isConnected: true, racingGameState: 'PLAYING' as const, isGoal: false };

describe('canAcceptTap', () => {
  it('연결된 PLAYING 에서만 받는다', () => {
    expect(canAcceptTap(playing)).toBe(true);
  });

  it('시작 전에는 안 받는다', () => {
    expect(canAcceptTap({ ...playing, racingGameState: 'DESCRIPTION' })).toBe(false);
    expect(canAcceptTap({ ...playing, racingGameState: 'PREPARE' })).toBe(false);
  });

  it('끝난 뒤에는 안 받는다', () => {
    expect(canAcceptTap({ ...playing, racingGameState: 'DONE' })).toBe(false);
  });

  it('끊긴 동안에는 안 받는다', () => {
    expect(canAcceptTap({ ...playing, isConnected: false })).toBe(false);
  });

  it('완주한 뒤에는 안 받는다', () => {
    expect(canAcceptTap({ ...playing, isGoal: true })).toBe(false);
  });
});
