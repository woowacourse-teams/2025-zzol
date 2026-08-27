import MiniGameCarousel from '@/features/home/components/tabs/HomeTab/MiniGameCarousel/MiniGameCarousel';
import { GameIcon } from '../menuIcons';
import * as S from './GameManualView.styled';

const GameManualView = () => (
  <S.Container>
    <S.HintBanner>
      <S.Subtitle>게임을 탭하면 자세한 규칙을 볼 수 있어요</S.Subtitle>
    </S.HintBanner>
    <MiniGameCarousel />
    <S.MoreLink to="/games">
      <S.MoreLeft>
        <S.MoreIcon>
          <GameIcon />
        </S.MoreIcon>
        <S.MoreTexts>
          <S.MoreTitle>게임별 자세한 설명 보기</S.MoreTitle>
          <S.MoreDesc>규칙과 내기 활용법을 글로 정리했어요</S.MoreDesc>
        </S.MoreTexts>
      </S.MoreLeft>
      <S.MoreChevron aria-hidden="true">›</S.MoreChevron>
    </S.MoreLink>
  </S.Container>
);

export default GameManualView;
