import { storageManager, STORAGE_KEYS } from '@/utils/StorageManager';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import * as S from './MyInfoView.styled';

const MyInfoView = () => {
  const { myName } = useIdentifier();

  const winCount = storageManager.getItem(STORAGE_KEYS.WIN_COUNT, 'localStorage', '0');
  const streak = storageManager.getItem(STORAGE_KEYS.NON_WIN_STREAK, 'localStorage', '0');

  return (
    <S.Container>
      <S.ProfileHeader>
        <S.ProfileIcon>😎</S.ProfileIcon>
        <S.ProfileInfo>
          <S.WelcomeMessage>{myName || '익명의 사용자'}님</S.WelcomeMessage>
          <S.UserStatus>오늘도 쫄깃한 승부를 즐겨보세요!</S.UserStatus>
        </S.ProfileInfo>
      </S.ProfileHeader>

      <S.StatGrid>
        <S.StatCard>
          <S.StatLabel>누적 당첨 횟수</S.StatLabel>
          <S.StatValue>{winCount}회</S.StatValue>
        </S.StatCard>
        <S.StatCard>
          <S.StatLabel>연속 안걸린 횟수</S.StatLabel>
          <S.StatValue>{streak}회</S.StatValue>
        </S.StatCard>
      </S.StatGrid>

      <S.InfoSection>
        <S.SectionTitle>통계 안내</S.SectionTitle>
        <S.TooltipCard>
          <S.TooltipText>
            • 당첨 횟수는 룰렛에서 최종적으로 선택된 횟수입니다.
            <br />
            • 연속 안걸린 횟수는 마지막 당첨 이후 성공적으로 대기를 피한 횟수입니다.
            <br />• 통계 데이터는 현재 브라우저의 로컬 스토리지에 저장됩니다.
          </S.TooltipText>
        </S.TooltipCard>
      </S.InfoSection>
    </S.Container>
  );
};

export default MyInfoView;
