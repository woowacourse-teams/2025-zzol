import RankingItem from '@/components/@common/RankingItem/RankingItem';
import { useMySeasonRank, useSeasonLeaderboard } from '@/features/home/hooks/useSeasonRanking';
import * as T from './RankingTab.styled';
import * as S from './SeasonRankingSection.styled';

/**
 * 시즌 랭킹 섹션 — 미니게임 결과가 시즌 포인트로 정산된 전역 리더보드.
 * 월 단위 시즌이라 매월 초기화되고, 회원(로그인) 결과만 집계된다.
 */
const SeasonRankingSection = () => {
  const { data: leaderboard, loading } = useSeasonLeaderboard();
  const { data: myRank, loading: myRankLoading } = useMySeasonRank();

  const rows = leaderboard?.rows ?? [];

  return (
    <T.RankingSection>
      <S.TitleRow>
        <T.StatsSectionTitle>시즌 랭킹</T.StatsSectionTitle>
        {leaderboard && <S.SeasonLabel>{leaderboard.seasonKey} 시즌</S.SeasonLabel>}
      </S.TitleRow>
      <S.Card>
        {!myRankLoading && myRank && (
          <S.MyRankRow>
            <S.MyRankLabel>내 순위</S.MyRankLabel>
            <S.MyRankValue>
              {myRank.rank}위 · {myRank.totalPoints}P {myRank.tier}
            </S.MyRankValue>
          </S.MyRankRow>
        )}
        {loading && <T.Spinner />}
        {!loading && rows.length === 0 && (
          <T.EmptyText>아직 이번 시즌 기록이 없어요. 블라인드 타이머를 플레이해보세요!</T.EmptyText>
        )}
        {!loading &&
          rows.map((row, index) => (
            <T.AnimatedItem key={row.userCode} $index={index}>
              <RankingItem
                rank={row.rank}
                name={`${row.nickname}#${row.userCode}`}
                count={row.totalPoints}
                unit="P"
              />
            </T.AnimatedItem>
          ))}
      </S.Card>
    </T.RankingSection>
  );
};

export default SeasonRankingSection;
