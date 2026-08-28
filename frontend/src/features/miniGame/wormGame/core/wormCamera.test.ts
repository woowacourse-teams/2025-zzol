import { followViewRadius } from './wormRenderer';

describe('followViewRadius', () => {
  it('430px 폰은 2px/u 를 채우도록 시야를 당긴다', () => {
    // 430 / (2 * 107.5) = 2.0 px/u — 궤적 레이어 해상도와 같아 확대로 뭉개지지 않는다
    expect(followViewRadius(430)).toBeCloseTo(107.5, 5);
  });

  it('큰 화면은 시야 상한 130u 를 넘겨 당기지 않는다', () => {
    // 여기서 더 넓히면 회피 판단에 필요한 것보다 멀리 보게 되고 요소만 작아진다
    expect(followViewRadius(768)).toBe(130);
    expect(followViewRadius(1920)).toBe(130);
  });

  it('작은 폰에서도 시야 하한 105u 아래로는 안 내려간다', () => {
    // 360px 은 계산상 90u 지만, 후반 192u/s 에서 0.47s 밖에 안 남아 회피가 불가능해진다
    expect(followViewRadius(360)).toBe(105);
    expect(followViewRadius(320)).toBe(105);
  });
});
