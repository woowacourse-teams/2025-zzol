import styled from '@emotion/styled';
import { theme } from '@/styles/theme';
import { OAuthProvider } from '../../types';

const BG: Record<OAuthProvider, string> = {
  google: theme.color.oauth.google.bg,
  kakao: theme.color.oauth.kakao.bg,
  naver: theme.color.oauth.naver.bg,
};

const TEXT: Record<OAuthProvider, string> = {
  google: theme.color.oauth.google.text,
  kakao: theme.color.oauth.kakao.text,
  naver: theme.color.oauth.naver.text,
};

const BORDER: Record<OAuthProvider, string> = {
  google: theme.color.oauth.google.border,
  kakao: 'transparent',
  naver: 'transparent',
};

const FONT_SIZE: Record<OAuthProvider, string> = {
  google: '14px',
  kakao: '15px',
  naver: '16px',
};

const FONT_WEIGHT: Record<OAuthProvider, number> = {
  google: 500,
  kakao: 600,
  naver: 600,
};

const GAP: Record<OAuthProvider, string> = {
  google: '10px',
  kakao: '10px',
  naver: '8px',
};

export const Button = styled.button<{ $provider: OAuthProvider }>`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: ${({ $provider }) => GAP[$provider]};
  width: 100%;
  padding: 14px 0;
  border: 1px solid ${({ $provider }) => BORDER[$provider]};
  border-radius: 12px;
  background-color: ${({ $provider }) => BG[$provider]};
  color: ${({ $provider }) => TEXT[$provider]};
  font-size: ${({ $provider }) => FONT_SIZE[$provider]};
  font-weight: ${({ $provider }) => FONT_WEIGHT[$provider]};
  cursor: pointer;
  transition: opacity 0.15s ease;

  &:active {
    opacity: 0.85;
  }
`;
