import Description from '@/components/@common/Description/Description';
import * as S from './RankItem.styled';
import { memo } from 'react';

type Props = {
  playerName: string;
  rank: number;
  isMe: boolean;
  isFixed: boolean;
};

const RankItem = memo(({ playerName, rank, isMe, isFixed }: Props) => {
  return (
    <S.Container $isFixed={isFixed}>
      <S.RankNumber>
        <Description color={getTextColor(isMe, isFixed)}>{rank}</Description>
      </S.RankNumber>
      <Description color={getTextColor(isMe, isFixed)}>{playerName}</Description>
    </S.Container>
  );
});

RankItem.displayName = 'RankItem';

export default RankItem;

const getTextColor = (isMe: boolean, isFixed: boolean) => {
  if (isMe) return 'point-500';
  if (isFixed) return 'yellow';
  return 'white';
};
