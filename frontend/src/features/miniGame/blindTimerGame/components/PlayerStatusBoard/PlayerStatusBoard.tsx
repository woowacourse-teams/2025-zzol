import { BlindTimerPlayerProgress } from '@/types/miniGame/blindTimerGame';
import * as S from './PlayerStatusBoard.styled';

type Props = {
  players: BlindTimerPlayerProgress[];
  myName: string;
};

const PlayerStatusBoard = ({ players, myName }: Props) => {
  const sorted = [...players].sort((a, b) => {
    if (a.playerName === myName) return -1;
    if (b.playerName === myName) return 1;
    return 0;
  });

  return (
    <S.Container>
      {sorted.map((player) => {
        const isMe = player.playerName === myName;
        return (
          <S.PlayerChip
            key={player.playerName}
            $stopped={player.stopped}
            $timedOut={player.timedOut}
            $isMe={isMe}
          >
            <S.StatusDot $stopped={player.stopped} $timedOut={player.timedOut} />
            {isMe ? '나' : player.playerName}
          </S.PlayerChip>
        );
      })}
    </S.Container>
  );
};

export default PlayerStatusBoard;
