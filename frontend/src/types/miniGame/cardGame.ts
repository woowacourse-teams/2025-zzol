/**
 * 카드 게임 타입
 */

import type { CardInfoMessage, CardType } from '@/apis/websocket/generated/wsContract';

export type { CardGameState, CardType } from '@/apis/websocket/generated/wsContract';

// TODO: 백엔드가 수정한 값대로 변경사항 반영 필요
export type AdditionValue = -40 | -30 | -20 | -10 | 0 | 10 | 20 | 30 | 40;
export type MultiplierValue = -1 | 0 | 2 | 4;

export type CardValue = AdditionValue | MultiplierValue;

export type AdditionCard = {
  type: 'ADDITION';
  value: AdditionValue;
};

export type MultiplierCard = {
  type: 'MULTIPLIER';
  value: MultiplierValue;
};

export type Card = AdditionCard | MultiplierCard;

export type CardInfo = CardInfoMessage;

export type SelectedCardInfo = Record<
  CardGameRound,
  {
    isSelected: boolean;
    type: CardType | null;
    value: CardValue | null;
  }
>;

export const CARD_GAME_ROUND_MAP = {
  FIRST: 1,
  SECOND: 2,
} as const;

export type CardGameRound = keyof typeof CARD_GAME_ROUND_MAP;
