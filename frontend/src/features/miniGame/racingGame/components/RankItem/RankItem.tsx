import Description from '@/components/@common/Description/Description';
import * as S from './RankItem.styled';

type Props = {
  playerName: string;
  rank: number;
  isMe: boolean;
  isFixed: boolean;
};

const RankItem = ({ playerName, rank, isMe, isFixed }: Props) => {
  return (
    <S.Container $isFixed={isFixed} $isMe={isMe}>
      <S.RankNumber>
        <Description color={getTextColor(isFixed)}>{rank}</Description>
      </S.RankNumber>
      <Description color={getTextColor(isFixed)}>{playerName}</Description>
      {isMe && <S.MyPointer />}
    </S.Container>
  );
};

export default RankItem;

// 내 행은 배경이 빨강으로 바뀌므로 글자는 흰색을 그대로 둔다. 완주하면 노랑으로 고정된다.
const getTextColor = (isFixed: boolean) => (isFixed ? 'yellow' : 'white');
