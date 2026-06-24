import { useBlockStackingGameContext } from '@/contexts/BlockStackingGame/BlockStackingGameContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useMemo } from 'react';
import * as S from './BlockStackingRankList.styled';

interface BlockStackingRankListProps {
  isCentered?: boolean;
}

const BlockStackingRankList = ({ isCentered = false }: BlockStackingRankListProps) => {
  const { rankings } = useBlockStackingGameContext();
  const { myName } = useIdentifier();

  const sortedRankings = useMemo(() => {
    return [...rankings].sort((a, b) => b.floor - a.floor);
  }, [rankings]);

  if (sortedRankings.length === 0) return null;

  return (
    <S.RankList isCentered={isCentered}>
      {sortedRankings.slice(0, 6).map((player, index) => {
        const isMe = player.name === myName;
        return (
          <S.RankItem key={player.name} isMe={isMe} isCentered={isCentered}>
            <S.Rank>{index + 1}</S.Rank>
            <S.Name isMe={isMe}>{player.name}</S.Name>
            <S.Floor>{player.floor}층</S.Floor>
          </S.RankItem>
        );
      })}
    </S.RankList>
  );
};

export default BlockStackingRankList;
