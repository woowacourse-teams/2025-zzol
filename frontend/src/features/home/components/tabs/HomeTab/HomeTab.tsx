import useModal from '@/components/@common/Modal/useModal';
import { usePlayerType } from '@/contexts/PlayerType/PlayerTypeContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import EnterRoomModal from '../../EnterRoomModal/EnterRoomModal';
import HeroCard from './HeroCard/HeroCard';
import JoinByCodeCard from './JoinByCodeCard/JoinByCodeCard';
import NewsCarousel from './NewsCarousel/NewsCarousel';
import MiniGameCarousel from './MiniGameCarousel/MiniGameCarousel';
import * as S from './HomeTab.styled';

const HomeTab = () => {
  const navigate = useReplaceNavigate();
  const { openModal, closeModal } = useModal();
  const { setHost, setGuest } = usePlayerType();

  const handleClickHost = () => {
    setHost();
    setTimeout(() => navigate('/entry/name'), 120);
  };

  const handleClickGuest = () => {
    setGuest();
    openModal(<EnterRoomModal onClose={closeModal} />, {
      title: '방 참가하기',
      showCloseButton: true,
    });
  };

  return (
    <S.Container>
      <S.CardGrid>
        <HeroCard onClick={handleClickHost} />
        <JoinByCodeCard onClick={handleClickGuest} />
      </S.CardGrid>

      <S.Section>
        <S.SectionTitle>서비스 소식</S.SectionTitle>
        <NewsCarousel />
      </S.Section>

      <S.Section>
        <S.SectionHeader>
          <S.SectionTitle>미니게임</S.SectionTitle>
          <S.SectionSub>탭해서 규칙 확인</S.SectionSub>
        </S.SectionHeader>
        <MiniGameCarousel />
      </S.Section>
    </S.Container>
  );
};

export default HomeTab;
