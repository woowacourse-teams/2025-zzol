import { useBlockStackingGameContext } from '@/contexts/BlockStackingGame/BlockStackingGameContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useMemo } from 'react';
import * as S from './BlockStackingRanks.styled';

const BlockStackingRanks = () => {
  const { rankings } = useBlockStackingGameContext();
  const { myName } = useIdentifier();

  const sortedRankings = useMemo(() => {
    return [...rankings].sort((a, b) => b.floor - a.floor);
  }, [rankings]);

  if (sortedRankings.length === 0) return null;

  return (
    <S.Container>
      <S.RankList>
        {sortedRankings.slice(0, 6).map((player, index) => {
          const isMe = player.name === myName;
          return (
            <S.RankItem key={player.name} isMe={isMe}>
              <S.Rank>{index + 1}</S.Rank>
              <S.Name>{player.name}</S.Name>
              <S.Floor>{player.floor}층</S.Floor>
            </S.RankItem>
          );
        })}
      </S.RankList>
    </S.Container>
  );
};

export default BlockStackingRanks;
