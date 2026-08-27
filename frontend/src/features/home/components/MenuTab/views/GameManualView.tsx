import MiniGameCarousel from '@/features/home/components/tabs/HomeTab/MiniGameCarousel/MiniGameCarousel';
import * as S from './GameManualView.styled';

const GameManualView = () => (
  <S.Container>
    <S.HintBanner>
      <S.Subtitle>게임을 탭하면 자세한 규칙을 볼 수 있어요</S.Subtitle>
    </S.HintBanner>
    <MiniGameCarousel />
    <S.MoreLink to="/games">게임별 자세한 설명 보기 &gt;</S.MoreLink>
  </S.Container>
);

export default GameManualView;
