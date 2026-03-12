import * as S from './TimerDisplay.styled';

type Props = {
  displayTime: string;
  isBlind: boolean;
  isStopped: boolean;
  stoppedTimeDisplay: string | null;
  onStop: () => void;
};

const TimerDisplay = ({ displayTime, isBlind, isStopped, stoppedTimeDisplay, onStop }: Props) => {
  return (
    <S.Container>
      <S.TimeText $isBlind={isBlind}>{displayTime}</S.TimeText>
      <S.StopButton $disabled={isStopped} onClick={isStopped ? undefined : onStop}>
        STOP
      </S.StopButton>
      {isStopped && stoppedTimeDisplay && (
        <S.StoppedResult>
          <S.StoppedLabel>내 기록</S.StoppedLabel>
          <S.StoppedTime>{stoppedTimeDisplay}초</S.StoppedTime>
        </S.StoppedResult>
      )}
    </S.Container>
  );
};

export default TimerDisplay;
