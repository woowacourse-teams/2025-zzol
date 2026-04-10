import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useMyStats } from '@/features/home/hooks/useMyStats';
import * as S from './MyInfoView.styled';

const MyInfoView = () => {
  const { myName } = useIdentifier();
  const { winCount, streak } = useMyStats();

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
          <S.TooltipList>
            <li>• 당첨 횟수는 룰렛에서 최종적으로 선택된 횟수입니다.</li>
            <li>• 연속 안걸린 횟수는 마지막 당첨 이후 성공적으로 대기를 피한 횟수입니다.</li>
            <li>• 통계 데이터는 현재 브라우저의 로컬 스토리지에 저장됩니다.</li>
          </S.TooltipList>
          <S.ComingSoonText>차후 로그인을 통해서 상세한 통계를 지원할 예정입니다.</S.ComingSoonText>
        </S.TooltipCard>
      </S.InfoSection>
    </S.Container>
  );
};

export default MyInfoView;
