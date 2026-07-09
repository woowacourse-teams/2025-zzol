import CardIcon from '@/assets/card-icon.svg';
import RacingIcon from '@/assets/racing-icon.svg';
import SpeedTouchIcon from '@/assets/speed-touch-icon.svg';
import BlindTimerIcon from '@/assets/blind-timer-icon.svg';
import BlockStackingIcon from '@/assets/block-stacking-icon.svg';
import LadderGameIcon from '@/assets/ladder-game-icon.svg';
import NunchiGameIcon from '@/assets/nunchi-game-icon.svg';

/**
 * 전체 미니 게임 공통 타입
 */

export const MINI_GAME_NAME_MAP = {
  CARD_GAME: '카드게임',
  RACING_GAME: '레이싱게임',
  SPEED_TOUCH: '1 to 25',
  BLIND_TIMER: '뇌피셜 초시계',
  BLOCK_STACKING: '블록 쌓기',
  LADDER_GAME: '사다리 게임',
  NUNCHI_GAME: '눈치게임',
} as const;

export type MiniGameType = keyof typeof MINI_GAME_NAME_MAP;

export const HIDDEN_MINI_GAMES: MiniGameType[] = [];

export const MINI_GAME_DESCRIPTION_MAP: Record<MiniGameType, string[]> = {
  CARD_GAME: ['카드를 뒤집어 가장 높은 점수를 내보세요!'],
  RACING_GAME: ['클릭해서 속도를 높여 가장 먼저 도착하세요!'],
  SPEED_TOUCH: ['1부터 25까지 순서대로 빠르게 터치하세요!'],
  BLIND_TIMER: ['목표 시간에 정확히 맞춰 STOP을 눌러보세요!'],
  BLOCK_STACKING: ['블록을 정확히 쌓아올리세요!'],
  LADDER_GAME: ['사다리를 타고 순위를 결정하세요!'],
  NUNCHI_GAME: ['눈치를 보며 숫자를 순서대로 누르세요! 동시에 누르면 충돌입니다.'],
};

export const MINI_GAME_ICON_MAP: Record<MiniGameType, string> = {
  CARD_GAME: CardIcon,
  RACING_GAME: RacingIcon,
  SPEED_TOUCH: SpeedTouchIcon,
  BLIND_TIMER: BlindTimerIcon,
  BLOCK_STACKING: BlockStackingIcon,
  LADDER_GAME: LadderGameIcon,
  NUNCHI_GAME: NunchiGameIcon,
};
