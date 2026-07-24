import RankingItem from '@/components/@common/RankingItem/RankingItem';
import { useMySeasonRank, useSeasonLeaderboard } from '@/features/home/hooks/useSeasonRanking';
import * as T from './RankingTab.styled';
import * as S from './OverallRankSection.styled';

/**
 * 게임 종합 랭크 — 미니게임 등수대로 획득한 포인트를 월간 누적하는 종합 랭킹.
 * 종목별 최고 기록(전체 랭킹)과 달리 꾸준한 참여와 성적을 측정한다.
 * 회원(로그인) 결과만 집계된다.
 */
const OverallRankSection = () => {
  const { data: leaderboard, loading } = useSeasonLeaderboard();
  const { data: myRank, loading: myRankLoading } = useMySeasonRank();

  const rows = leaderboard?.rows ?? [];

  return (
    <T.RankingSection>
      <T.StatsSectionTitle>게임 종합 랭크</T.StatsSectionTitle>
      <S.Caption>
        게임에서 얻은 등수대로 포인트가 쌓이는 종합 랭킹이에요. 지금은 블라인드 타이머만 집계돼요.
      </S.Caption>
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
    </T.RankingSection>
  );
};

export default OverallRankSection;
