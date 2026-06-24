import * as S from './TimerBar.styled';

type Props = {
  seconds: number;
  progress: number;
};

const URGENT_THRESHOLD = 10;

const TimerBar = ({ seconds, progress }: Props) => {
  const isUrgent = seconds <= URGENT_THRESHOLD;

  return (
    <S.Container>
      <S.BarWrapper>
        <S.Bar $progress={progress} $urgent={isUrgent} />
      </S.BarWrapper>
      <S.TimeText $urgent={isUrgent}>{seconds}초</S.TimeText>
    </S.Container>
  );
};

export default TimerBar;
