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
