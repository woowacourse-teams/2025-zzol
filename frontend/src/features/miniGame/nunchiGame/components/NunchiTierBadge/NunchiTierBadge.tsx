import { NunchiResultTier } from '@/types/miniGame/nunchiGame';
import * as S from './NunchiTierBadge.styled';

/**
 * 3계층 결과 배지(요구사항 9, ADR 구현 노트 N7).
 *
 * rank 숫자만으로는 충돌/미입력 동점 그룹을 구분할 수 없으므로 tier 를 배지로 시각화한다.
 *  - SOLO:      정상 단독 입력.
 *  - COLLISION: 충돌(동시 입력) — 그룹 동점.
 *  - MISS:      미입력(타임아웃) — 제일 꼴등.
 */
const TIER_LABEL: Record<NunchiResultTier, string> = {
  SOLO: '성공',
  COLLISION: '충돌',
  MISS: '미입력',
};

type Props = {
  tier: NunchiResultTier;
};

const NunchiTierBadge = ({ tier }: Props) => {
  return <S.Badge $tier={tier}>{TIER_LABEL[tier]}</S.Badge>;
};

export default NunchiTierBadge;
