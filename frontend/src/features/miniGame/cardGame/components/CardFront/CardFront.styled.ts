import { ColorList } from '@/constants/color';
import { Card } from '@/types/miniGame/cardGame';
import { Size } from '@/types/styles';
import styled from '@emotion/styled';
import { cardVariants, circleVariants } from '../../constants/variants';
import { DESIGN_TOKENS } from '@/constants/design';

type Props = {
  $size?: Size;
  $playerColor?: ColorList | null;
  $card?: Card;
  $isMyCard?: boolean;
};

const CARD_TEXT_COLORS = {
  POSITIVE: '#6DA4FF',
  NEGATIVE: '#FF6C6C',
  MULTIPLIER: '#81E121',
  DEFAULT: '#000',
} as const;

const getCardTextColor = ($card?: Card) => {
  if (!$card) return CARD_TEXT_COLORS.DEFAULT;

  const { type, value } = $card;

  switch (type) {
    case 'ADDITION':
      return value >= 0 ? CARD_TEXT_COLORS.POSITIVE : CARD_TEXT_COLORS.NEGATIVE;

    case 'MULTIPLIER':
      return CARD_TEXT_COLORS.MULTIPLIER;

    default:
      return CARD_TEXT_COLORS.DEFAULT;
  }
};

const cardTextFontSize = {
  small: DESIGN_TOKENS.typography.small, // ~12px
  medium: DESIGN_TOKENS.typography.h4, // ~16px
  large: DESIGN_TOKENS.typography.h3, // ~20px
};

export const Container = styled.div<Props>`
  ${({ $size }) => cardVariants[$size || 'large']}
  background-color: ${({ theme }) => theme.color.point[200]};
  border-radius: 7px;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  ${({ $playerColor, $isMyCard }) =>
    $isMyCard && $playerColor
      ? ` box-shadow: 
      0 0 4px ${$playerColor},
      0 0 8px ${$playerColor},
      0 0 12px ${$playerColor},
      0 0 14px ${$playerColor}`
      : 'box-shadow: 0 3px 3px rgba(0, 0, 0, 0.4);'}
`;

export const Circle = styled.div<Props>`
  background-color: ${({ theme }) => theme.color.point[50]};
  width: ${({ $size }) => circleVariants[$size || 'large']};
  height: ${({ $size }) => circleVariants[$size || 'large']};

  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
`;

export const CardIcon = styled.img`
  width: 80%;
  height: 80%;
`;

export const CardText = styled.span<Props>`
  font-size: ${({ $size }) => cardTextFontSize[$size || 'large']};
  color: ${({ $card }) => getCardTextColor($card)};
  font-weight: 700;
  line-height: 1;
  text-align: center;
`;

export const PlayerName = styled.span<Props>`
  color: ${({ theme, $playerColor }) => $playerColor || theme.color.point[400]};
  margin-top: 10px;
  font-size: ${DESIGN_TOKENS.typography.h4};
  font-weight: 700;
  max-width: 80%;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;
