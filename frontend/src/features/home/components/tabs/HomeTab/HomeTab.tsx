import useModal from '@/components/@common/Modal/useModal';
import { usePlayerType } from '@/contexts/PlayerType/PlayerTypeContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useAuth } from '@/features/auth/contexts/AuthContext';
import { useMyStats } from '@/features/home/hooks/useMyStats';
import EnterRoomModal from '../../EnterRoomModal/EnterRoomModal';
import HeroCard from './HeroCard/HeroCard';
import JoinByCodeCard from './JoinByCodeCard/JoinByCodeCard';
import NewsCarousel from './NewsCarousel/NewsCarousel';
import MiniGameCarousel from './MiniGameCarousel/MiniGameCarousel';
import * as S from './HomeTab.styled';

type Props = { onNavigateToPatchNotes: () => void };

const HomeTab = ({ onNavigateToPatchNotes }: Props) => {
  const navigate = useReplaceNavigate();
  const { openModal, closeModal } = useModal();
  const { setHost, setGuest } = usePlayerType();
  const { isAuthenticated } = useAuth();
  const { winCount, streak } = useMyStats();

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
      <S.Section>
        <S.SectionTitle>서비스 소식</S.SectionTitle>
        <NewsCarousel onMoreClick={onNavigateToPatchNotes} />
      </S.Section>

      {isAuthenticated && (
        <S.Section>
          <S.SectionTitle>내 정보</S.SectionTitle>
          <S.MyInfoCard>
            <S.MyInfoStatGrid>
              <S.MyInfoStat>
                <S.MyInfoLabel>누적 당첨 횟수</S.MyInfoLabel>
                <S.MyInfoValueRow>
                  <S.MyInfoNumber>{winCount}</S.MyInfoNumber>
                  <S.MyInfoUnit>회</S.MyInfoUnit>
                </S.MyInfoValueRow>
              </S.MyInfoStat>
              <S.MyInfoStat>
                <S.MyInfoLabel>연속 생존</S.MyInfoLabel>
                <S.MyInfoValueRow>
                  <S.MyInfoNumber>{streak}</S.MyInfoNumber>
                  <S.MyInfoUnit>번</S.MyInfoUnit>
                </S.MyInfoValueRow>
              </S.MyInfoStat>
            </S.MyInfoStatGrid>
          </S.MyInfoCard>
        </S.Section>
      )}

      <S.Section>
        <S.SectionTitle>게임 시작</S.SectionTitle>
        <S.CardGrid>
          <HeroCard onClick={handleClickHost} />
          <JoinByCodeCard onClick={handleClickGuest} />
        </S.CardGrid>
      </S.Section>

      <S.Section>
        <S.SectionHeader>
          <S.SectionTitle>미니게임</S.SectionTitle>
          <S.SectionSub>탭해서 규칙 확인</S.SectionSub>
        </S.SectionHeader>
        <MiniGameCarousel />
      </S.Section>

      <S.ScrollIndicator>
        <S.ScrollChevron>⌄</S.ScrollChevron>
      </S.ScrollIndicator>
    </S.Container>
  );
};

export default HomeTab;
