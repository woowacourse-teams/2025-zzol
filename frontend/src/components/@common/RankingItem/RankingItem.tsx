import Headline4 from '@/components/@common/Headline4/Headline4';
import { ColorKey } from '@/constants/color';
import * as S from './RankingItem.styled';

type Props = {
  rank: number;
  name: string;
  count: number;
  unit?: string;
};

const RankingItem = ({ rank, name, count, unit = '회' }: Props) => {
  const getRankTextColor = (rank: number): ColorKey => {
    if (rank <= 3) return 'white';
    return 'gray-700';
  };

  return (
    <S.Container>
      <S.RankNumber $rank={rank}>
        <Headline4 color={getRankTextColor(rank)}>{rank}</Headline4>
      </S.RankNumber>
      <S.Content>
        <Headline4>{name}</Headline4>
        <Headline4>{count}{unit}</Headline4>
      </S.Content>
    </S.Container>
  );
};

export default RankingItem;
