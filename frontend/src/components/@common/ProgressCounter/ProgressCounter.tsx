import * as S from './ProgressCounter.styled';

type Props = {
  current: number;
  total: number;
  ariaLabel?: string;
};

const ProgressCounter = ({ current, total, ariaLabel }: Props) => {
  return (
    <S.Container aria-label={ariaLabel}>
      {current}/{total}
    </S.Container>
  );
};

export default ProgressCounter;
