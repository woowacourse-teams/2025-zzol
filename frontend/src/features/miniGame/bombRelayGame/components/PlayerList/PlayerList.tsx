import { BombRelayPlayerProgress } from '@/types/miniGame/bombRelayGame';
import * as S from './PlayerList.styled';

type Props = {
  players: BombRelayPlayerProgress[];
  currentTurnPlayerName: string;
  myName: string;
};

const PlayerList = ({ players, currentTurnPlayerName, myName }: Props) => {
  return (
    <S.Container>
      {players.map((player) => {
        const isMe = player.playerName === myName;
        const isTurn = player.playerName === currentTurnPlayerName;

        return (
          <S.PlayerChip
            key={player.playerName}
            $eliminated={player.eliminated}
            $isCurrentTurn={isTurn}
            $isMe={isMe}
          >
            {isTurn && !player.eliminated && <S.BombEmoji>💣</S.BombEmoji>}
            {player.playerName}
            {isMe && <S.MeTag>나</S.MeTag>}
            {player.eliminated && ' 💥'}
          </S.PlayerChip>
        );
      })}
    </S.Container>
  );
};

export default PlayerList;
