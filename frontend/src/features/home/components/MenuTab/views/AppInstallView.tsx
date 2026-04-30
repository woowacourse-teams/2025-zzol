import useInstallPrompt from '@/hooks/useInstallPrompt';
import * as S from './AppInstallView.styled';

const AppInstallView = () => {
  const { isInstallable, isInstalled, install } = useInstallPrompt();

  return (
    <S.Container>
      <S.BenefitCard>
        <S.BenefitItem>
          <S.BenefitIcon aria-hidden="true">⚡</S.BenefitIcon>
          <S.BenefitText>QR 없이 바로 실행</S.BenefitText>
        </S.BenefitItem>
        <S.BenefitItem>
          <S.BenefitIcon aria-hidden="true">📱</S.BenefitIcon>
          <S.BenefitText>앱처럼 전체 화면으로 사용</S.BenefitText>
        </S.BenefitItem>
        <S.BenefitItem>
          <S.BenefitIcon aria-hidden="true">🚀</S.BenefitIcon>
          <S.BenefitText>재방문 시 빠른 로딩</S.BenefitText>
        </S.BenefitItem>
      </S.BenefitCard>

      {isInstalled ? (
        <S.StatusCard $status="installed">
          <S.StatusIcon aria-hidden="true">✓</S.StatusIcon>
          <S.StatusText>이미 홈 화면에 설치되어 있어요</S.StatusText>
        </S.StatusCard>
      ) : isInstallable ? (
        <S.InstallButton onClick={install}>홈 화면에 추가</S.InstallButton>
      ) : (
        <S.StatusCard $status="unavailable">
          <S.StatusText>
            브라우저 주소창 오른쪽의 설치 버튼을 누르거나,
            <br />
            Safari에서 공유 → 홈 화면에 추가를 선택하세요
          </S.StatusText>
        </S.StatusCard>
      )}
    </S.Container>
  );
};

export default AppInstallView;
