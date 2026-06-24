import useModal from '@/components/@common/Modal/useModal';
import { useAuth } from '../../contexts/AuthContext';
import { OAuthProvider } from '../../types';
import ProviderButton from '../ProviderButton/ProviderButton';
import * as S from './LoginSheet.styled';

const PROVIDERS: OAuthProvider[] = ['kakao', 'google', 'naver'];

const LoginSheet = () => {
  const { login } = useAuth();
  const { closeModal } = useModal();

  const handleLogin = (provider: OAuthProvider) => {
    closeModal();
    login(provider);
  };

  return (
    <S.Wrapper>
      <S.Title>로그인</S.Title>
      <S.Description>소셜 계정으로 간편하게 시작하세요</S.Description>
      {PROVIDERS.map((provider) => (
        <ProviderButton key={provider} provider={provider} onClick={handleLogin} />
      ))}
      <S.Divider />
      <S.AnonButton type="button" onClick={closeModal}>
        로그인 없이 계속하기
      </S.AnonButton>
    </S.Wrapper>
  );
};

export default LoginSheet;
