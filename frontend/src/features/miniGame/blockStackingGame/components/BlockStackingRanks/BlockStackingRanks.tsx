import { useBlockStackingGameContext } from '@/contexts/BlockStackingGame/BlockStackingGameContext';
import BlockStackingRankList from '../BlockStackingRankList/BlockStackingRankList';
import * as S from './BlockStackingRanks.styled';

const BlockStackingRanks = () => {
  const { isLocalGameOver } = useBlockStackingGameContext();

  // 탈락(Game Over) 시에는 우측 상단 랭킹을 숨기고 중앙 오버레이의 랭킹만 표시합니다.
  if (isLocalGameOver) return null;

  return (
    <S.Container>
      <BlockStackingRankList />
    </S.Container>
  );
};

export default BlockStackingRanks;
