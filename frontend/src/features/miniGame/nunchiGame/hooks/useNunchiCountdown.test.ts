import { act, renderHook } from '@testing-library/react';
import { useNunchiCountdown } from './useNunchiCountdown';

/**
 * useNunchiCountdown — 스큐 보정 + rAF 단일 루프(요구사항 1/2).
 *
 * rAF 는 jsdom 에 없으므로 setTimeout 으로 폴리필하고, Date.now 를 고정해 결정적으로 검증한다.
 */
describe('useNunchiCountdown', () => {
  const FIXED_NOW = 1_000_000;

  beforeEach(() => {
    jest.useFakeTimers();
    jest.spyOn(Date, 'now').mockReturnValue(FIXED_NOW);
    // rAF/cAF 폴리필: 다음 tick 으로 예약.
    jest
      .spyOn(window, 'requestAnimationFrame')
      .mockImplementation((cb) => setTimeout(() => cb(performance.now()), 16) as unknown as number);
    jest
      .spyOn(window, 'cancelAnimationFrame')
      .mockImplementation((id) => clearTimeout(id as unknown as number));
  });

  afterEach(() => {
    jest.restoreAllMocks();
    jest.useRealTimers();
  });

  const flush = () => act(() => void jest.advanceTimersByTime(20));

  it('enabled=false 또는 deadline=null 이면 0 을 반환한다', () => {
    const { result, rerender } = renderHook(
      ({ deadline, enabled }) => useNunchiCountdown(deadline, 0, enabled),
      { initialProps: { deadline: FIXED_NOW + 5000, enabled: false } }
    );
    expect(result.current).toBe(0);

    rerender({ deadline: null as unknown as number, enabled: true });
    flush();
    expect(result.current).toBe(0);
  });

  it('스큐 보정 없이 남은 시간을 ms 로 계산한다', () => {
    const { result } = renderHook(() => useNunchiCountdown(FIXED_NOW + 5000, 0, true));
    flush();
    // deadline - (now + offset) = 5000 - 0 = 5000
    expect(result.current).toBe(5000);
  });

  it('serverOffsetMs(시계 스큐)를 반영해 남은 시간을 보정한다', () => {
    // 클라 시계가 서버보다 2초 빠른 상황: offset = serverNow - clientNow = -2000.
    // remaining = deadline - (now + offset) = (now+1000) - (now-2000) = 3000.
    const { result } = renderHook(() => useNunchiCountdown(FIXED_NOW + 1000, -2000, true));
    flush();
    expect(result.current).toBe(3000);
  });

  it('데드라인이 이미 지났으면 음수가 아니라 0 으로 clamp 한다', () => {
    const { result } = renderHook(() => useNunchiCountdown(FIXED_NOW - 500, 0, true));
    flush();
    expect(result.current).toBe(0);
  });
});
