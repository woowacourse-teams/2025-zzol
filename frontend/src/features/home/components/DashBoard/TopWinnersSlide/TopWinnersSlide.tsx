import type { TopWinner } from '@/types/dashBoard';
import * as S from './TopWinnersSlide.styled';

type Props = {
  winners: TopWinner[];
  displayCount?: number;
};

const TopWinnersSlide = ({ winners, displayCount = 5 }: Props) => (
  <S.Card>
    <S.CardTitle>이달의 TOP{displayCount} 당첨자</S.CardTitle>
    {winners.length === 0 ? (
      <S.Empty>아직 당첨자가 없어요</S.Empty>
    ) : (
      <S.List>
        {winners.slice(0, displayCount).map((winner, index) => (
          <S.Item key={winner.playerName}>
            <S.Rank $rank={index + 1}>{index + 1}</S.Rank>
            <S.Name>{winner.playerName}</S.Name>
            <S.Count>{winner.winCount}회</S.Count>
          </S.Item>
        ))}
      </S.List>
    )}
  </S.Card>
);

export default TopWinnersSlide;
