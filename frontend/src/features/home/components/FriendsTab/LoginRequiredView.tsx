import styled from '@emotion/styled';
import { useAuth } from '@/features/auth/hooks/useAuth';
import ProviderButton from '@/features/auth/components/ProviderButton/ProviderButton';
import { OAuthProvider } from '@/features/auth/types';
import { theme } from '@/styles/theme';

const PROVIDERS: OAuthProvider[] = ['kakao', 'google', 'naver'];

const FriendsIllustration = () => (
  <svg
    width="96"
    height="80"
    viewBox="0 0 96 80"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    {/* 왼쪽 사람 */}
    <circle cx="28" cy="22" r="12" fill={theme.color.point[100]} />
    <circle cx="28" cy="22" r="8" fill={theme.color.point[200]} />
    <path
      d="M8 60c0-11 9-18 20-18s20 7 20 18"
      stroke={theme.color.point[300]}
      strokeWidth="3"
      strokeLinecap="round"
      fill="none"
    />
    {/* 오른쪽 사람 */}
    <circle cx="68" cy="22" r="12" fill={theme.color.point[100]} />
    <circle cx="68" cy="22" r="8" fill={theme.color.point[200]} />
    <path
      d="M48 60c0-11 9-18 20-18s20 7 20 18"
      stroke={theme.color.point[300]}
      strokeWidth="3"
      strokeLinecap="round"
      fill="none"
    />
    {/* 연결선 */}
    <path
      d="M40 30 Q48 20 56 30"
      stroke={theme.color.point[300]}
      strokeWidth="2"
      strokeLinecap="round"
      strokeDasharray="3 3"
      fill="none"
    />
    {/* 하트 */}
    <path
      d="M48 18 c0-2.5 3.5-4 4.5-1.5 1 2.5-4.5 7-4.5 7 0 0-5.5-4.5-4.5-7 1-2.5 4.5-1 4.5 1.5z"
      fill={theme.color.point[400]}
    />
  </svg>
);

const LoginRequiredView = () => {
  const { login } = useAuth();

  return (
    <S.Container>
      <S.IllustrationWrap>
        <FriendsIllustration />
      </S.IllustrationWrap>

      <S.TextGroup>
        <S.Title>친구와 함께 더 재미있게</S.Title>
        <S.Desc>로그인하면 친구를 추가하고{'\n'}게임에 함께 초대할 수 있어요</S.Desc>
      </S.TextGroup>

      <S.LoginSection>
        {PROVIDERS.map((provider) => (
          <ProviderButton key={provider} provider={provider} onClick={login} />
        ))}
      </S.LoginSection>
    </S.Container>
  );
};

export default LoginRequiredView;

const S = {
  Container: styled.div`
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 48px 24px 32px;
    gap: 0;
  `,

  IllustrationWrap: styled.div`
    margin-bottom: 24px;
  `,

  TextGroup: styled.div`
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    margin-bottom: 36px;
    text-align: center;
  `,

  Title: styled.p`
    font-size: 20px;
    font-weight: 700;
    color: ${theme.color.gray[900]};
    letter-spacing: -0.02em;
  `,

  Desc: styled.p`
    ${theme.typography.small}
    color: ${theme.color.gray[400]};
    white-space: pre-line;
    line-height: 1.7;
  `,

  LoginSection: styled.div`
    display: flex;
    flex-direction: column;
    gap: 10px;
    width: 100%;
  `,
};
