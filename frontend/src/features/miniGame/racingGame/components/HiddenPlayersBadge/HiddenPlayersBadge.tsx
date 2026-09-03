import * as S from './HiddenPlayersBadge.styled';

type Props = {
  direction: 'ahead' | 'behind';
  count: number;
};

/** 트랙에 다 못 세운 인원을 가장자리에 알린다. 순위표에는 뜨는데 트랙에 없는 사람을 설명한다. */
const HiddenPlayersBadge = ({ direction, count }: Props) => {
  if (count === 0) return null;

  return (
    <S.Badge $direction={direction}>
      <S.Arrow $direction={direction} />
      <S.Label>{direction === 'ahead' ? `앞으로 ${count}명 더` : `뒤로 ${count}명 더`}</S.Label>
    </S.Badge>
  );
};

export default HiddenPlayersBadge;

type SummaryProps = {
  totalCount: number;
  visibleCount: number;
};

/** 트랙에 몇 명이 보이는지와 무엇을 기준으로 골랐는지 적는다. */
export const HiddenPlayersSummary = ({ totalCount, visibleCount }: SummaryProps) => {
  if (totalCount === visibleCount) return null;

  return (
    <S.Summary>
      <S.SummaryText>
        {totalCount}명 중 {visibleCount}명 표시 · 나와 가까운 순서
      </S.SummaryText>
    </S.Summary>
  );
};
