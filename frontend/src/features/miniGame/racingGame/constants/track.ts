/**
 * 서버 좌표 1 unit 을 몇 px 로 그릴지 정하는 배율.
 *
 * 화면 폭이 최대 430px 이라 1일 때 내 앞뒤 약 215 unit 만 보인다. 전체 트랙 3000 unit 의 7% 다.
 * 기기별로 보이는 범위를 조정하는 손잡이라 상수로 뽑아 둔다.
 */
export const PIXELS_PER_UNIT = 1;

/** 서버 RacingGame.MAX_SPEED 와 같은 값. 속도 게이지의 눈금 상한이다. */
export const MAX_SPEED = 60;

export const SPEED_SEGMENT_COUNT = 6;
