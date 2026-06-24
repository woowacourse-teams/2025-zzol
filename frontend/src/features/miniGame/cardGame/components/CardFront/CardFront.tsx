import CardIcon from '@/assets/sign-inversion-icon.svg';
import { ColorList } from '@/constants/color';
import { Card } from '@/types/miniGame/cardGame';
import { Size } from '@/types/styles';
import * as S from './CardFront.styled';

type Props = {
  size?: Size;
  playerColor?: ColorList | null;
  card: Card;
  isMyCard?: boolean;
  playerName?: string;
};

const CardFront = ({ size, playerColor, card, isMyCard = false, playerName }: Props) => {
  const isSignInversionCard = card.type === 'MULTIPLIER' && card.value === -1;

  return (
    <S.Container $size={size} $playerColor={playerColor} $isMyCard={isMyCard}>
      <S.Circle $size={size}>
        {isSignInversionCard ? (
          <S.CardIcon src={CardIcon} alt="부호 반전" />
        ) : (
          <S.CardText $size={size} $card={card}>
            {getCardText(card)}
          </S.CardText>
        )}
      </S.Circle>
      {playerName && <S.PlayerName $playerColor={playerColor}>{playerName}</S.PlayerName>}
    </S.Container>
  );
};

export default CardFront;

const getCardText = (card: Card) => {
  const { type, value } = card;
  if (type === 'ADDITION') return value >= 0 ? `+${value}` : `${value}`;
  else return value === -1 ? null : `x${value}`;
};
