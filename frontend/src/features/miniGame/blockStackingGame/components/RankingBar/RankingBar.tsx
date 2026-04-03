import { useBlockStackingGameContext } from '@/contexts/BlockStackingGame/BlockStackingGameContext';
import * as S from './RankingBar.styled';

const RankingBar = () => {
  const { rankings } = useBlockStackingGameContext();

  if (rankings.length === 0) return null;

  return (
    <S.Wrapper>
      {rankings.map(({ name, floor }) => (
        <S.Item key={name}>
          <S.Name>{name}</S.Name>
          <S.Floor>{floor}층</S.Floor>
        </S.Item>
      ))}
    </S.Wrapper>
  );
};

export default RankingBar;
