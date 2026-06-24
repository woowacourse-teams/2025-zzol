import { useEffect, useState } from 'react';
import * as S from './CircularProgress.styled';

type Props = {
  current: number;
  total: number;
  size?: string;
  isActive?: boolean;
};

const RADIUS = 45;
const circumference = 2 * Math.PI * RADIUS;

const CircularProgress = ({ current, total, size = '2rem', isActive = true }: Props) => {
  const [strokeDashoffset, setStrokeDashoffset] = useState(0);

  useEffect(() => {
    if (!isActive) {
      setStrokeDashoffset(0);
      return;
    }

    if (total <= 0) {
      setStrokeDashoffset(circumference);
      return;
    }

    const progress = Math.min(1, (total - current + 1) / total);
    const newStrokeDashoffset = circumference * progress;
    setStrokeDashoffset(newStrokeDashoffset);
  }, [current, total, isActive]);

  return (
    <S.Container $size={size}>
      <S.ProgressRing width="100%" height="100%" viewBox="0 0 100 100">
        <S.BackgroundCircle cx="50" cy="50" r={RADIUS} fill="none" />
        <S.ProgressCircle
          cx="50"
          cy="50"
          r={RADIUS}
          fill="none"
          strokeDasharray={circumference}
          strokeDashoffset={strokeDashoffset}
          transform="rotate(90 50 50) scale(-1,1) translate(-100, 0)"
          $isActive={isActive}
        />
      </S.ProgressRing>
      <S.CountText>{current}</S.CountText>
    </S.Container>
  );
};

export default CircularProgress;
