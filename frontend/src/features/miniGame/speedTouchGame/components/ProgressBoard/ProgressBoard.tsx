import { PlayerProgress } from '@/types/miniGame/speedTouchGame';
import * as S from './ProgressBoard.styled';

type Props = {
  players: PlayerProgress[];
  myName: string;
};

const MAX_NUMBER = 25;

const ProgressBoard = ({ players, myName }: Props) => {
  const sorted = [...players].sort((a, b) => {
    if (a.playerName === myName) return -1;
    if (b.playerName === myName) return 1;
    return b.currentNumber - a.currentNumber;
  });

  return (
    <S.Container>
      {sorted.map((player) => {
        const isMe = player.playerName === myName;
        const progress = player.currentNumber - 1;

        return (
          <S.PlayerRow key={player.playerName} $isMe={isMe}>
            <S.PlayerName>{isMe ? '나' : player.playerName}</S.PlayerName>
            <S.BarWrapper>
              <S.Bar $progress={progress} $isMe={isMe} />
            </S.BarWrapper>
            <S.Count>{player.finished ? '완주' : `${progress}/${MAX_NUMBER}`}</S.Count>
          </S.PlayerRow>
        );
      })}
    </S.Container>
  );
};

export default ProgressBoard;
