import useFetch from '@/apis/rest/useFetch';
import type { TopWinner, LowestProbabilityWinner, GamePlayCount } from '@/types/dashBoard';
import TopWinnersSlide from './TopWinnersSlide/TopWinnersSlide';
import LowestProbabilitySlide from './LowestProbabilitySlide/LowestProbabilitySlide';
import GamePlayCountSlide from './GamePlayCountSlide/GamePlayCountSlide';
import * as S from './DashBoard.styled';

const DashBoard = () => {
  const { data: topWinners } = useFetch<TopWinner[]>({
    endpoint: '/dashboard/top-winners',
  });
  const { data: lowestProbabilityWinner } = useFetch<LowestProbabilityWinner>({
    endpoint: '/dashboard/lowest-probability-winner',
  });
  const { data: gamePlayCounts } = useFetch<GamePlayCount[]>({
    endpoint: '/dashboard/game-play-counts',
  });

  return (
    <S.Container>
      <TopWinnersSlide winners={topWinners || []} displayCount={5} />
      <LowestProbabilitySlide
        WinnerNames={lowestProbabilityWinner?.playerNames || []}
        probability={lowestProbabilityWinner?.probability || 0}
      />
      <GamePlayCountSlide games={gamePlayCounts || []} />
    </S.Container>
  );
};

export default DashBoard;
