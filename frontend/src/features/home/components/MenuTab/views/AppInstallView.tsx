import useInstallPrompt from '@/hooks/useInstallPrompt';
import * as S from './AppInstallView.styled';

const BENEFITS = [
  { emoji: '⚡', title: '빠른 실행', desc: 'QR 스캔 없이 바로 입장' },
  { emoji: '📱', title: '앱처럼 사용', desc: '전체 화면으로 몰입감 있게' },
  { emoji: '🚀', title: '빠른 로딩', desc: '재방문 시 캐시로 즉시 실행' },
];

const AppInstallView = () => {
  const { isInstallable, isInstalled, install } = useInstallPrompt();

  return (
    <S.Container>
      <S.HeroCard>
        <S.HeroIconWrap>
          <svg width="32" height="32" viewBox="0 0 32 32" fill="none" aria-hidden="true">
            <path d="M16 4 L16 22" stroke="white" strokeWidth="2.5" strokeLinecap="round" />
            <path
              d="M9 16 L16 23 L23 16"
              stroke="white"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            <path d="M6 27 H26" stroke="white" strokeWidth="2.5" strokeLinecap="round" />
          </svg>
        </S.HeroIconWrap>
        <S.HeroTitle>앱으로 설치하기</S.HeroTitle>
        <S.HeroSub>홈 화면에 추가하여 앱처럼 사용하세요</S.HeroSub>
      </S.HeroCard>

      <S.SectionLabel>이런 점이 좋아요</S.SectionLabel>
      <S.Card>
        {BENEFITS.map(({ emoji, title, desc }) => (
          <S.BenefitRow key={title}>
            <S.BenefitEmoji aria-hidden="true">{emoji}</S.BenefitEmoji>
            <S.BenefitInfo>
              <S.BenefitTitle>{title}</S.BenefitTitle>
              <S.BenefitDesc>{desc}</S.BenefitDesc>
            </S.BenefitInfo>
          </S.BenefitRow>
        ))}
      </S.Card>

      {isInstalled ? (
        <S.StatusBanner $type="installed">
          <S.StatusEmoji aria-hidden="true">✓</S.StatusEmoji>
          <S.StatusText>이미 홈 화면에 설치되어 있어요</S.StatusText>
        </S.StatusBanner>
      ) : isInstallable ? (
        <S.InstallButton onClick={install}>홈 화면에 추가</S.InstallButton>
      ) : (
        <>
          <S.SectionLabel>설치 방법</S.SectionLabel>
          <S.Card>
            <S.GuideRow>
              <S.GuideBadge>iOS</S.GuideBadge>
              <S.GuideDesc>공유 버튼 → 홈 화면에 추가</S.GuideDesc>
            </S.GuideRow>
            <S.GuideRow>
              <S.GuideBadge>AOS</S.GuideBadge>
              <S.GuideDesc>주소창 오른쪽 설치 버튼 탭</S.GuideDesc>
            </S.GuideRow>
          </S.Card>
        </>
      )}
    </S.Container>
  );
};

export default AppInstallView;
