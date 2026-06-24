import Crown from '@/assets/crown.svg';
import Headline4 from '@/components/@common/Headline4/Headline4';
import { ColorList } from '@/constants/color';
import { PlayerType } from '@/types/player';
import { PropsWithChildren } from 'react';
import * as S from './PlayerCard.styled';
import PlayerIcon from '@/components/@composition/PlayerIcon/PlayerIcon';
import CheckIcon from '@/assets/check-icon.svg';
import IconTextItem from '@/components/@common/IconTextItem/IconTextItem';

type Props = {
  name: string;
  playerColor: ColorList;
  playerType?: PlayerType;
  isReady?: boolean;
} & PropsWithChildren;

const PlayerCard = ({
  name,
  playerColor,
  playerType = 'GUEST',
  isReady = false,
  children,
}: Props) => {
  return (
    <IconTextItem
      iconContent={<PlayerIcon color={playerColor} />}
      textContent={
        <S.NameWrapper>
          <Headline4>{name}</Headline4>
          {playerType === 'HOST' && <S.CrownIcon src={Crown} alt="crown" />}
          {playerType === 'GUEST' && isReady && <S.ReadyIcon src={CheckIcon} alt="ready" />}
        </S.NameWrapper>
      }
      rightContent={children}
    />
  );
};

export default PlayerCard;
