export const WHEEL_CONFIG = {
  SIZE: 300,
  CENTER: 150,
  RADIUS: 140,
  STROKE_WIDTH: 4,

  /**
   * 내 위치 마커 — 원판 바깥 여백(반지름 140~150)에 떠 있는 점과 짧은 선.
   *
   * 조각 자체는 키우지 않는다. 조각 넓이는 반지름의 제곱에 비례해서,
   * 내 조각만 크게 그리면 확률이 실제보다 커 보인다(140→145 면 7.3% 증가).
   * 확률을 보여주는 화면이므로 그 왜곡을 만들지 않는다.
   *
   * 최대 반경은 149.5 로 viewBox 안에서 끝나므로 원판 크기에 영향이 없다.
   */
  MY_MARKER_LINE_INNER: 132,
  MY_MARKER_LINE_OUTER: 137,
  MY_MARKER_DOT_CENTER: 143.2,
  MY_MARKER_DOT_RADIUS: 5.6,
  MY_MARKER_LINE_WIDTH: 3,
  /**
   * 흰 테두리 두께(한쪽). 점 지름의 20% 정도라야 조각 색 위에서도 형태가 또렷하다.
   * 점의 바깥 끝(143.2 + 5.6 + 1.1 = 149.9)이 viewBox 안에서 끝나도록 잡혀 있다.
   */
  MY_MARKER_OUTLINE: 1.1,

  /** 라벨이 시작하는 반지름 — 중심부는 조각이 뾰족해 글자를 담지 못한다 */
  LABEL_INNER_RADIUS: 45,
  /** 라벨이 끝나는 반지름 — 테두리와 겹치지 않게 가장자리에서 5px 띄운다 */
  LABEL_OUTER_RADIUS: 135,
  LABEL_MAX_FONT_SIZE: 13,
  /** 이보다 작아지면 읽을 수 없다고 보고 이름을 그리지 않는다 */
  LABEL_MIN_FONT_SIZE: 9,

  /**
   * 핀이 휠 위로 튀어나오는 높이(px).
   * 핀 스타일과 휠을 감싸는 영역의 위 여백이 같은 값을 써야 한다 —
   * 어긋나면 핀이 잘리거나 제목과 겹친다.
   */
  PIN_OVERHANG: 24,
} as const;

/** 라벨이 반경 방향으로 쓸 수 있는 길이 — 조각이 얼마나 얇든 일정하다 */
export const LABEL_LENGTH = WHEEL_CONFIG.LABEL_OUTER_RADIUS - WHEEL_CONFIG.LABEL_INNER_RADIUS;

/** 라벨의 중심이 놓이는 반지름 */
export const LABEL_RADIUS = (WHEEL_CONFIG.LABEL_OUTER_RADIUS + WHEEL_CONFIG.LABEL_INNER_RADIUS) / 2;
