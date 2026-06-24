import { Card, CardGameRound, SelectedCardInfo } from '@/types/miniGame/cardGame';
import CardBack from '../CardBack/CardBack';
import CardFront from '../CardFront/CardFront';
import * as S from './PlayerCardDisplay.styled';
import Flip from '@/components/@common/Flip/Flip';
import { DESIGN_TOKENS } from '@/constants/design';

type Props = {
  selectedCardInfo: SelectedCardInfo;
};

const PlayerCardDisplay = ({ selectedCardInfo }: Props) => {
  const renderPlayerCard = (round: CardGameRound) => {
    const cardInfo = selectedCardInfo[round];

    return (
      <Flip
        flipped={cardInfo.isSelected}
        width={DESIGN_TOKENS.card.medium.width}
        height={DESIGN_TOKENS.card.medium.height}
        duration={0.5}
        initialView={<CardBack size="medium" disabled={true} />}
        flippedView={
          <CardFront
            size="medium"
            card={
              {
                type: cardInfo.type,
                value: cardInfo.value,
              } as Card
            }
          />
        }
      />
    );
  };

  return (
    <S.Container>
      {renderPlayerCard('FIRST')}
      {renderPlayerCard('SECOND')}
    </S.Container>
  );
};

export default PlayerCardDisplay;
