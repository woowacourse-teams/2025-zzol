import RankingItem from '@/components/@common/RankingItem/RankingItem';
import { useMySeasonRank, useSeasonLeaderboard } from '@/features/home/hooks/useSeasonRanking';
import * as S from './RankTab.styled';

/**
 * 게임 종합 랭크 탭 — 미니게임 등수대로 획득한 포인트를 월간 누적하는 종합 랭킹.
 * 종목별 최고 기록(랭킹 탭)과 성격이 달라 별도 탭으로 분리했다.
 * 회원(로그인) 결과만 집계된다.
 */
const RankTab = () => {
  const { data: leaderboard, loading } = useSeasonLeaderboard();
  const { data: myRank, loading: myRankLoading } = useMySeasonRank();

  const rows = leaderboard?.rows ?? [];

  return (
    <S.Container>
      <S.Title>게임 종합 랭크</S.Title>
      <S.Caption>
        게임에서 얻은 등수대로 포인트가 쌓이는 종합 랭킹이에요.
        <br />
        지금은 블라인드 타이머만 집계돼요.
      </S.Caption>
      <S.CardWrapper>
        <S.Card>
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
      </S.CardWrapper>
    </S.Container>
  );
};

export default RankTab;
