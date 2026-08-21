import { formatProbability, formatProbabilityChange } from './formatProbability';

describe('formatProbability', () => {
  // 서버는 0~10000 정수를 100 으로 나눠 보낸다 — 값은 항상 n/100 이다
  it.each([
    [20, '20.00%'],
    [12.5, '12.50%'],
    [11.4, '11.40%'],
    [8.33, '8.33%'],
    [16.66, '16.66%'],
    [100, '100.00%'],
    [0, '0.00%'],
  ])('%p 를 %p 로 찍어 소수점 위치를 맞춘다', (input, expected) => {
    expect(formatProbability(input)).toBe(expected);
  });

  it('세로로 쌓아도 소수점 자리가 어긋나지 않는다', () => {
    const rendered = [20, 12.5, 8.33].map(formatProbability);

    const decimalPositions = rendered.map((text) => text.length - text.indexOf('.'));
    expect(new Set(decimalPositions).size).toBe(1);
  });
});

describe('formatProbabilityChange', () => {
  it('늘었으면 부호를 붙인다', () => {
    expect(formatProbabilityChange(2)).toBe('+2.00%');
  });

  it('줄었으면 음수 부호가 그대로 나온다', () => {
    expect(formatProbabilityChange(-5)).toBe('-5.00%');
  });

  it('변화가 없으면 0 도 늘어난 쪽으로 본다', () => {
    expect(formatProbabilityChange(0)).toBe('+0.00%');
  });
});
