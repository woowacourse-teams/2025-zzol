import MiniGameResultSkeleton from '@/components/@composition/MiniGameResultSkeleton/MiniGameResultSkeleton';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { NunchiResultEntry, NunchiResultTier } from '@/types/miniGame/nunchiGame';
import { useMemo } from 'react';
import NunchiTierBadge from '../components/NunchiTierBadge/NunchiTierBadge';
import { useNunchiResult } from '../hooks/useNunchiResult';
import * as S from './NunchiGameResultPage.styled';

/**
 * 눈치게임 3계층 결과 본문(요구사항 8/9, ADR 구현 노트 N7) — 신규 전용 뷰.
 *
 * 공유 MiniGameResultPage 의 Layout.Content 슬롯에 끼워진다(GAME_CONFIGS.NUNCHI_GAME.ResultContent).
 * 배너·룰렛 진행 버튼·룰렛 구독은 공유 페이지가 유지하므로 여기선 본문(결과 리스트)만 그린다.
 *
 * 3계층 정렬: SOLO → COLLISION → MISS(좋음→나쁨). BE 가 standard-competition rank(1,2,2,4,6)와
 * tier 를 함께 주므로, 같은 rank 끼리 묶고 tier 배지를 단다.
 */

// tier 정렬 우선순위(좋음→나쁨). BE rank 가 권위지만 동일 rank 내 표시·계층 헤더 정렬에 사용.
const TIER_ORDER: Record<NunchiResultTier, number> = {
  SOLO: 0,
  COLLISION: 1,
  MISS: 2,
};

const NunchiGameResultPage = () => {
  const { joinCode, myName } = useIdentifier();
  const { results, loading } = useNunchiResult(joinCode);

  // rank 오름차순(좋음 먼저), 동일 rank 는 tier 우선순위 → 이름으로 안정 정렬.
  const sortedResults = useMemo(
    () =>
      [...results].sort(
        (a, b) =>
          a.rank - b.rank ||
          TIER_ORDER[a.tier] - TIER_ORDER[b.tier] ||
          a.playerName.localeCompare(b.playerName)
      ),
    [results]
  );

  if (loading) {
    return <MiniGameResultSkeleton />;
  }

  if (sortedResults.length === 0) {
    return <S.Empty>결과를 불러오지 못했습니다.</S.Empty>;
  }

  return (
    <S.ResultList>
      {sortedResults.map((entry: NunchiResultEntry) => {
        const isMe = entry.playerName === myName;
        return (
          <S.ResultItem key={entry.playerName} $isMe={isMe} $tier={entry.tier}>
            <S.Rank>{entry.rank}</S.Rank>
            <S.Name $isMe={isMe}>{entry.playerName}</S.Name>
            <NunchiTierBadge tier={entry.tier} />
          </S.ResultItem>
        );
      })}
    </S.ResultList>
  );
};

export default NunchiGameResultPage;
