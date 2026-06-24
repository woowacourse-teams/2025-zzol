import { colorList } from '@/constants/color';
import { useParticipants } from '@/contexts/Participants/ParticipantsContext';
import { Card, CardInfo } from '@/types/miniGame/cardGame';
import CardBack from '../CardBack/CardBack';
import CardFront from '../CardFront/CardFront';
import * as S from './GameCardGrid.styled';
import Flip from '@/components/@common/Flip/Flip';

import { DESIGN_TOKENS } from '@/constants/design';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';

type Props = {
  cardInfos: CardInfo[];
  onCardClick: (cardIndex: number) => void;
};

const GameCardGrid = ({ cardInfos, onCardClick }: Props) => {
  const { getParticipantColorIndex } = useParticipants();
  const { myName } = useIdentifier();

  return (
    <S.Container>
      {cardInfos.map((cardInfo, index) => {
        const playerColor = cardInfo.playerName
          ? colorList[getParticipantColorIndex(cardInfo.playerName)]
          : null;

        return (
          <Flip
            key={index}
            flipped={cardInfo.selected}
            width={DESIGN_TOKENS.card.large.width}
            height={DESIGN_TOKENS.card.large.height}
            duration={0.5}
            initialView={<CardBack onClick={() => onCardClick(index)} />}
            flippedView={
              <CardFront
                card={
                  {
                    type: cardInfo.cardType,
                    value: cardInfo.value,
                  } as Card
                }
                playerColor={playerColor}
                isMyCard={cardInfo.playerName === myName}
                playerName={cardInfo.playerName || undefined}
              />
            }
          />
        );
      })}
    </S.Container>
  );
};

export default GameCardGrid;
