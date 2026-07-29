import { useAuth } from '@/features/auth/contexts/AuthContext';
import { useMySeasonRank, useSeasonLeaderboard } from '@/features/home/hooks/useSeasonRanking';
import type { SeasonTier } from '@/types/season';
import * as S from './RankTab.styled';

const TIER_LABEL: Record<SeasonTier, string> = {
  BRONZE: '브론즈',
  SILVER: '실버',
  GOLD: '골드',
  DIAMOND: '다이아',
};

/**
 * 게임 종합 랭크 탭 — 미니게임 등수대로 획득한 포인트를 월간 누적하는 종합 랭킹.
 * 내 순위 히어로 카드(포인트 레드) + 티어 뱃지 + 메달 리더보드 + 포인트 규칙 안내.
 * 회원(로그인) 결과만 집계된다.
 */
const RankTab = () => {
  const { user } = useAuth();
  const { data: leaderboard, loading, error } = useSeasonLeaderboard();
  const { data: myRank, loading: myRankLoading } = useMySeasonRank();

  const rows = leaderboard?.rows ?? [];
  const myUserCode = user?.userCode;

  return (
    <S.Container>
      <S.Title>게임 종합 랭크</S.Title>
      <S.Caption>
        게임에서 얻은 등수대로 포인트가 쌓이는 종합 랭킹이에요.
        <br />
        모든 미니게임의 결과가 집계돼요.
      </S.Caption>

      {!myRankLoading && myRank && (
        <S.HeroCard>
          <S.HeroTopRow>
            <S.HeroLabel>내 순위</S.HeroLabel>
            <S.TierBadge $tier={myRank.tier}>{TIER_LABEL[myRank.tier]}</S.TierBadge>
          </S.HeroTopRow>
          <S.HeroRankRow>
            <S.HeroRank>{myRank.rank}</S.HeroRank>
            <S.HeroRankUnit>위</S.HeroRankUnit>
            <S.HeroPoints>{myRank.totalPoints}P</S.HeroPoints>
          </S.HeroRankRow>
          <S.HeroFooter>
            {leaderboard ? `${leaderboard.seasonKey} 시즌 · ` : ''}전체 {myRank.totalMembers}명 중
          </S.HeroFooter>
        </S.HeroCard>
      )}

      <S.BoardCard>
        <S.BoardHeader>
          <S.BoardTitle>리더보드</S.BoardTitle>
          {leaderboard && leaderboard.totalMembers > 0 && (
            <S.BoardCount>{leaderboard.totalMembers}명 참여 중</S.BoardCount>
          )}
        </S.BoardHeader>
        {loading && <S.Spinner />}
        {!loading && error && (
          <S.Empty>
            <S.EmptyText>랭킹을 불러오지 못했어요. 잠시 후 다시 시도해주세요.</S.EmptyText>
          </S.Empty>
        )}
        {!loading && !error && rows.length === 0 && (
          <S.Empty>
            <S.EmptyEmoji>🏆</S.EmptyEmoji>
            <S.EmptyText>
              아직 이번 달 기록이 없어요.
              <br />
              미니게임을 플레이하고 첫 1위를 차지해보세요!
            </S.EmptyText>
          </S.Empty>
        )}
        {!loading &&
          rows.map((row, index) => (
            <S.AnimatedItem key={row.userCode} $index={index}>
              <S.Row $isMe={row.userCode === myUserCode}>
                <S.Medal $rank={row.rank}>{row.rank}</S.Medal>
                <S.RowName>
                  <S.Nickname>{row.nickname}</S.Nickname>
                  <S.UserCode>#{row.userCode}</S.UserCode>
                </S.RowName>
                <S.TierBadge $tier={row.tier}>{TIER_LABEL[row.tier]}</S.TierBadge>
                <S.RowPoints>{row.totalPoints}P</S.RowPoints>
              </S.Row>
            </S.AnimatedItem>
          ))}
      </S.BoardCard>

      <S.GuideCard>
        <S.GuideTitle>포인트 안내</S.GuideTitle>
        <S.GuideList>
          <li>• 게임 1등 100P · 2등 70P · 3등 50P · 그 외 30P를 얻어요.</li>
          <li>• 동점이면 해당 순위들의 포인트를 합쳐 똑같이 나눠 가져요.</li>
          <li>• 300P 실버 · 1,000P 골드 · 3,000P 다이아 티어로 올라가요.</li>
          <li>• 랭크는 매달 1일에 초기화돼요. 로그인한 플레이만 집계돼요.</li>
        </S.GuideList>
      </S.GuideCard>
    </S.Container>
  );
};

export default RankTab;
