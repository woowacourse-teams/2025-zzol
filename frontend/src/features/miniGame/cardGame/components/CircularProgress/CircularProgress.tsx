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
  // props에서 직접 파생한다(값 변경 시 CSS transition 으로 애니메이션됨).
  const strokeDashoffset = !isActive
    ? 0
    : total <= 0
      ? circumference
      : circumference * Math.min(1, (total - current + 1) / total);

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
