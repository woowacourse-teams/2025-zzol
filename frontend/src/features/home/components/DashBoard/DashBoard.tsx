import { useDashboardData } from '../../hooks/useDashboardData';
import TopWinnersSlide from './TopWinnersSlide/TopWinnersSlide';
import LowestProbabilitySlide from './LowestProbabilitySlide/LowestProbabilitySlide';
import GamePlayCountSlide from './GamePlayCountSlide/GamePlayCountSlide';
import * as S from './DashBoard.styled';

const DashBoard = () => {
  const { topWinners, lowestProbabilityWinner, gamePlayCounts } = useDashboardData();

  return (
    <S.Container>
      <TopWinnersSlide winners={topWinners} displayCount={5} />
      <LowestProbabilitySlide
        players={lowestProbabilityWinner?.players ?? []}
        probability={lowestProbabilityWinner?.probability ?? 0}
      />
      <GamePlayCountSlide games={gamePlayCounts} />
    </S.Container>
  );
};

export default DashBoard;
