import { getSliceLabel } from './getSliceLabel';

describe('getSliceLabel', () => {
  describe('이름을 그릴지 결정한다', () => {
    it('조각이 10도보다 얇으면 null 을 돌려준다', () => {
      // 확률 2% = 7.2도
      expect(getSliceLabel({ startAngle: 0, endAngle: 7.2, nameLength: 3 })).toBeNull();
    });

    it('조각이 10도 이상이면 배치를 돌려준다', () => {
      expect(getSliceLabel({ startAngle: 0, endAngle: 10, nameLength: 3 })).not.toBeNull();
    });

    it('이름이 비어 있으면 null 을 돌려준다', () => {
      expect(getSliceLabel({ startAngle: 0, endAngle: 90, nameLength: 0 })).toBeNull();
    });
  });

  describe('글자 크기는 반경 길이 104px 를 글자 수로 나눈다', () => {
    it('최대 길이(10자)도 조각 두께와 무관하게 들어간다', () => {
      // 확률 5% = 18도 — 가로 배치로는 26px 밖에 못 쓰던 최악 케이스
      const label = getSliceLabel({ startAngle: 0, endAngle: 18, nameLength: 10 });

      expect(label?.fontSize).toBeCloseTo(10.4);
    });

    it('짧은 이름은 최대 크기에서 멈춘다', () => {
      const label = getSliceLabel({ startAngle: 0, endAngle: 120, nameLength: 3 });

      expect(label?.fontSize).toBe(13);
    });
  });

  describe('왼쪽 반원의 글자는 뒤집어 정립시킨다', () => {
    it('오른쪽 반원은 중심에서 바깥을 향한다', () => {
      // 중심각 90도 (3시 방향)
      const label = getSliceLabel({ startAngle: 60, endAngle: 120, nameLength: 3 });

      expect(label).toMatchObject({ x: 230, y: 150, rotate: 0 });
    });

    it('왼쪽 반원은 180도 돌려 반대편에 놓는다', () => {
      // 중심각 270도 (9시 방향)
      const label = getSliceLabel({ startAngle: 240, endAngle: 300, nameLength: 3 });

      expect(label).toMatchObject({ x: 70, y: 150, rotate: 360 });
    });

    it('경계인 180도는 왼쪽으로 본다', () => {
      const label = getSliceLabel({ startAngle: 150, endAngle: 210, nameLength: 3 });

      expect(label?.rotate).toBe(270);
    });
  });
});
