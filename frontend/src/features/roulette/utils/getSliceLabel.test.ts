import { LABEL_RADIUS, WHEEL_CONFIG } from '../constants/config';
import { getSliceLabel } from './getSliceLabel';

/** 반지름 r 지점에서 조각이 접선 방향으로 내주는 폭 */
const thicknessAt = (radius: number, sweep: number) =>
  2 * radius * Math.sin((sweep / 2) * (Math.PI / 180));

/** 글자열의 안쪽 끝 반지름 — 여기가 가장 빠듯하다 */
const innerEndOf = (fontSize: number, nameLength: number) =>
  LABEL_RADIUS - (nameLength * fontSize) / 2;

describe('getSliceLabel', () => {
  describe('그릴 수 없으면 null 을 돌려준다', () => {
    it('이름이 비어 있으면 null', () => {
      expect(getSliceLabel({ startAngle: 0, endAngle: 90, nameLength: 0 })).toBeNull();
    });

    it('각도가 0 이하면 null', () => {
      expect(getSliceLabel({ startAngle: 90, endAngle: 90, nameLength: 3 })).toBeNull();
      expect(getSliceLabel({ startAngle: 90, endAngle: 30, nameLength: 3 })).toBeNull();
    });

    it('글자가 읽을 수 없을 만큼 작아지면 null', () => {
      // 10자 이름은 8도 조각(약 2.2%)에서 하한 아래로 떨어진다
      expect(getSliceLabel({ startAngle: 0, endAngle: 8, nameLength: 10 })).toBeNull();
    });

    it('같은 조각이라도 이름이 짧으면 살아남는다', () => {
      expect(getSliceLabel({ startAngle: 0, endAngle: 8, nameLength: 3 })).not.toBeNull();
    });
  });

  describe('글자가 옆 조각을 침범하지 않는다', () => {
    // 회귀: 예전에는 라벨 중간점에서만 두께를 확인해, 안쪽 끝이 이웃 조각으로 넘쳤다
    const sweeps = [9, 10, 12, 18, 25.2, 36, 64.8, 180];
    const lengths = [1, 2, 3, 5, 7, 10];

    it.each(sweeps)('%s도 조각에서 어떤 길이의 이름도 넘치지 않는다', (sweep) => {
      lengths.forEach((nameLength) => {
        const label = getSliceLabel({ startAngle: 0, endAngle: sweep, nameLength });
        if (!label) return;

        const innerEnd = innerEndOf(label.fontSize, nameLength);
        // 글자 높이가 가장 좁은 지점의 폭 이내여야 한다
        expect(label.fontSize).toBeLessThanOrEqual(thicknessAt(innerEnd, sweep) + 0.001);
      });
    });

    it('이름 전체가 라벨 길이 안에 들어온다', () => {
      const label = getSliceLabel({ startAngle: 0, endAngle: 180, nameLength: 10 });

      expect(label!.fontSize * 10).toBeLessThanOrEqual(
        WHEEL_CONFIG.LABEL_OUTER_RADIUS - WHEEL_CONFIG.LABEL_INNER_RADIUS + 0.001
      );
    });
  });

  describe('글자 크기', () => {
    it('넉넉한 조각의 짧은 이름은 최대 크기에서 멈춘다', () => {
      const label = getSliceLabel({ startAngle: 0, endAngle: 120, nameLength: 3 });

      expect(label?.fontSize).toBe(WHEEL_CONFIG.LABEL_MAX_FONT_SIZE);
    });

    it('최대 길이(10자)도 3.3% 조각까지는 읽을 수 있게 들어간다', () => {
      const label = getSliceLabel({ startAngle: 0, endAngle: 12, nameLength: 10 });

      expect(label?.fontSize).toBeGreaterThanOrEqual(WHEEL_CONFIG.LABEL_MIN_FONT_SIZE);
    });

    it('이름이 길수록 작아진다', () => {
      const short = getSliceLabel({ startAngle: 0, endAngle: 60, nameLength: 4 });
      const long = getSliceLabel({ startAngle: 0, endAngle: 60, nameLength: 10 });

      expect(long!.fontSize).toBeLessThan(short!.fontSize);
    });
  });

  describe('왼쪽 반원의 글자는 뒤집어 정립시킨다', () => {
    it('오른쪽 반원은 중심에서 바깥을 향한다', () => {
      // 중심각 90도 (3시 방향)
      const label = getSliceLabel({ startAngle: 60, endAngle: 120, nameLength: 3 });

      expect(label).toMatchObject({ x: WHEEL_CONFIG.CENTER + LABEL_RADIUS, y: 150, rotate: 0 });
    });

    it('왼쪽 반원은 180도 돌려 반대편에 놓는다', () => {
      // 중심각 270도 (9시 방향)
      const label = getSliceLabel({ startAngle: 240, endAngle: 300, nameLength: 3 });

      expect(label).toMatchObject({ x: WHEEL_CONFIG.CENTER - LABEL_RADIUS, y: 150, rotate: 360 });
    });

    it('경계인 180도는 왼쪽으로 본다', () => {
      const label = getSliceLabel({ startAngle: 150, endAngle: 210, nameLength: 3 });

      expect(label?.rotate).toBe(270);
    });
  });

  describe('혼자 남아 조각이 원 전체일 때', () => {
    // 회귀: sin 은 180도를 지나면 다시 줄어들어, 360도 조각의 두께를 0 으로 계산했다
    it('최대 길이 이름도 사라지지 않는다', () => {
      const label = getSliceLabel({ startAngle: 0, endAngle: 360, nameLength: 10 });

      expect(label).not.toBeNull();
      // 침범할 이웃이 없으니 길이 제약만 남는다
      expect(label?.fontSize).toBeCloseTo(
        (WHEEL_CONFIG.LABEL_OUTER_RADIUS - WHEEL_CONFIG.LABEL_INNER_RADIUS) / 10
      );
    });

    it('짧은 이름은 최대 크기로 그린다', () => {
      const label = getSliceLabel({ startAngle: 0, endAngle: 360, nameLength: 3 });

      expect(label?.fontSize).toBe(WHEEL_CONFIG.LABEL_MAX_FONT_SIZE);
    });
  });
});
