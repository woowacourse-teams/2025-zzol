export const WHEEL_CONFIG = {
  SIZE: 300,
  CENTER: 150,
  RADIUS: 140,
  STROKE_WIDTH: 4,

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
