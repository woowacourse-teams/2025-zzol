export const WHEEL_CONFIG = {
  SIZE: 300,
  CENTER: 150,
  RADIUS: 140,
  STROKE_WIDTH: 4,

  /** 라벨이 시작하는 반지름 — 중심부는 조각이 뾰족해 글자를 담지 못한다 */
  LABEL_INNER_RADIUS: 28,
  /** 라벨이 끝나는 반지름 — 테두리·핀과 겹치지 않게 가장자리에서 8px 띄운다 */
  LABEL_OUTER_RADIUS: 132,
  LABEL_MAX_FONT_SIZE: 13,
  /**
   * 이름을 그릴 최소 조각 각도(도).
   *
   * 라벨 반지름 80 지점에서 조각의 두께는 2 * 80 * sin(각도/2) 다.
   * 10도면 13.9px 로 최대 글자 크기 13px 을 담을 수 있다.
   * 이 하한을 세워 두면 두께 제약이 절대 걸리지 않아, 글자 크기는 길이만 보면 된다.
   */
  MIN_LABEL_ANGLE: 10,
} as const;

/** 라벨이 반경 방향으로 쓸 수 있는 길이 — 조각이 얼마나 얇든 일정하다 */
export const LABEL_LENGTH = WHEEL_CONFIG.LABEL_OUTER_RADIUS - WHEEL_CONFIG.LABEL_INNER_RADIUS;

/** 라벨의 중심이 놓이는 반지름 */
export const LABEL_RADIUS = (WHEEL_CONFIG.LABEL_OUTER_RADIUS + WHEEL_CONFIG.LABEL_INNER_RADIUS) / 2;
