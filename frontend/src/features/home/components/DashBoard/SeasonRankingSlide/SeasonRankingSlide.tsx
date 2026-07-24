import RankingItem from '@/components/@common/RankingItem/RankingItem';
import { useMySeasonRank, useSeasonLeaderboard } from '@/features/home/hooks/useSeasonRanking';
import * as S from './SeasonRankingSlide.styled';

/**
 * 포인트 랭킹 카드 — 미니게임 결과가 시즌 포인트로 정산된 리더보드.
 * 이달의 통계 섹션이 이미 월 단위 범위를 뜻하므로 카드 안에서 시즌을 다시 표기하지 않는다.
 * 회원(로그인) 결과만 집계된다.
 */
const SeasonRankingSlide = () => {
  const { data: leaderboard, loading } = useSeasonLeaderboard();
  const { data: myRank, loading: myRankLoading } = useMySeasonRank();

  const rows = leaderboard?.rows ?? [];

  return (
    <S.Card>
      <S.CardTitle>포인트 랭킹</S.CardTitle>
      {!myRankLoading && myRank && (
        <S.MyRankRow>
          <S.MyRankLabel>내 순위</S.MyRankLabel>
          <S.MyRankValue>
            {myRank.rank}위 · {myRank.totalPoints}P {myRank.tier}
          </S.MyRankValue>
        </S.MyRankRow>
      )}
      {loading && <S.Spinner />}
      {!loading && rows.length === 0 && (
        <S.Empty>아직 이번 달 기록이 없어요. 블라인드 타이머를 플레이해보세요!</S.Empty>
      )}
      {!loading &&
        rows.map((row, index) => (
          <S.AnimatedItem key={row.userCode} $index={index}>
            <RankingItem
              rank={row.rank}
              name={`${row.nickname}#${row.userCode}`}
              count={row.totalPoints}
              unit="P"
            />
          </S.AnimatedItem>
        ))}
    </S.Card>
  );
};

export default SeasonRankingSlide;
