import { useRacingGameRankedPlayers } from '@/contexts/RacingGame/RacingGameContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import RankItem from '../RankItem/RankItem';
import * as S from './RacingRanks.styled';

const RacingRanks = () => {
  const rankedPlayers = useRacingGameRankedPlayers();
  const { myName } = useIdentifier();

  return (
    <S.Container>
      <S.RankList>
        {rankedPlayers.map((player, index) => {
          const isMe = player.playerName === myName;

          return (
            <RankItem
              key={player.playerName}
              playerName={player.playerName}
              rank={index + 1}
              isMe={isMe}
              isFixed={player.isFinished}
            />
          );
        })}
      </S.RankList>
    </S.Container>
  );
};

RacingRanks.displayName = 'RacingRanks';

export default RacingRanks;
