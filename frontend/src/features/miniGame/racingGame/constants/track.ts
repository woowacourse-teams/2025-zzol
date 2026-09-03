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

/**
 * 서버 RacingGame.MIN_SPEED 와 같은 값. 탭을 멈춰도 속도가 이 아래로 안 내려간다.
 * 속도가 0이면 isStopped() 로 못 움직여 완주 못 한 채 경기가 끝나므로 서버가 바닥을 깔아 둔 것이다.
 *
 * 서버가 이 값을 안 내려줘 여기에 베껴 뒀다. 서버 상수가 바뀌면 여기도 바꿔야 한다.
 * 2단계에서 서버가 값을 실어 주면 그걸로 갈아 끼운다.
 */
export const MIN_SPEED = 3;
