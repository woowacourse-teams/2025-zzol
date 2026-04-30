import type { ReactNode } from 'react';
import { OAuthProvider } from '../../types';
import { GoogleIcon, KakaoIcon, NaverIcon } from './providerIcons';
import * as S from './ProviderButton.styled';

const LABEL: Record<OAuthProvider, string> = {
  google: 'Google로 계속하기',
  kakao: '카카오로 계속하기',
  naver: '네이버로 계속하기',
};

const ICON: Record<OAuthProvider, ReactNode> = {
  google: <GoogleIcon />,
  kakao: <KakaoIcon />,
  naver: <NaverIcon />,
};

type Props = {
  provider: OAuthProvider;
  onClick: (provider: OAuthProvider) => void;
};

const ProviderButton = ({ provider, onClick }: Props) => (
  <S.Button $provider={provider} type="button" onClick={() => onClick(provider)}>
    {ICON[provider]}
    {LABEL[provider]}
  </S.Button>
);

export default ProviderButton;
