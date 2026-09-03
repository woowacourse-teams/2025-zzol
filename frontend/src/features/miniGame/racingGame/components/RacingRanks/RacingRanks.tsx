import RankItem from '../RankItem/RankItem';
import { RankedPlayer } from '../../hooks/useRaceRanking';
import * as S from './RacingRanks.styled';

type Props = {
  rankedPlayers: RankedPlayer[];
  myName: string;
  endDistance: number;
};

const RacingRanks = ({ rankedPlayers, myName, endDistance }: Props) => {
  return (
    <S.Container>
      <S.RankList>
        {rankedPlayers.map((player, index) => (
          <RankItem
            key={player.playerName}
            playerName={player.playerName}
            rank={index + 1}
            isMe={player.playerName === myName}
            isFixed={endDistance > 0 && player.position >= endDistance}
          />
        ))}
      </S.RankList>
    </S.Container>
  );
};

export default RacingRanks;
