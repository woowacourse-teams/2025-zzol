import CardIcon from '@/assets/card-icon.svg';
import RacingIcon from '@/assets/racing-icon.svg';
import SpeedTouchIcon from '@/assets/speed-touch-icon.svg';
import BlindTimerIcon from '@/assets/blind-timer-icon.svg';

/**
 * 전체 미니 게임 공통 타입
 */

export const MINI_GAME_NAME_MAP = {
  CARD_GAME: '카드게임',
  RACING_GAME: '레이싱게임',
  SPEED_TOUCH: '1 to 25',
  BLIND_TIMER: '뇌피셜 초시계',
} as const;

export type MiniGameType = keyof typeof MINI_GAME_NAME_MAP;

export const MINI_GAME_DESCRIPTION_MAP: Record<MiniGameType, string[]> = {
  CARD_GAME: ['2라운드 동안 매번 카드 1장씩 뒤집어', '가장 높은 점수를 내보세요!'],
  RACING_GAME: ['화면을 클릭해 속도를 높여서', '가장 먼저 도착하세요!'],
  SPEED_TOUCH: ['1부터 25까지 순서대로 터치해서', '가장 빠르게 완주하세요!'],
  BLIND_TIMER: ['목표 시간에 정확히 맞춰', 'STOP을 눌러보세요!'],
};

export const MINI_GAME_ICON_MAP: Record<MiniGameType, string> = {
  CARD_GAME: CardIcon,
  RACING_GAME: RacingIcon,
  SPEED_TOUCH: SpeedTouchIcon,
  BLIND_TIMER: BlindTimerIcon,
};
