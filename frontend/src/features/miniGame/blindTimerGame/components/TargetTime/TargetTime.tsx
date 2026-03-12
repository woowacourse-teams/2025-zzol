import * as S from './TargetTime.styled';

type Props = {
  targetTimeMillis: number;
};

const formatTargetTime = (ms: number): string => {
  const seconds = (ms / 1000).toFixed(2);
  return `${seconds}초`;
};

const TargetTime = ({ targetTimeMillis }: Props) => {
  return (
    <S.Container>
      <S.Label>목표</S.Label>
      <S.Time>{formatTargetTime(targetTimeMillis)}</S.Time>
    </S.Container>
  );
};

export default TargetTime;
