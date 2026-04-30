import styled from '@emotion/styled';
import { OAuthProvider } from '../../types';

const BG: Record<OAuthProvider, string> = {
  google: '#FFFFFF',
  kakao: '#FEE500',
  naver: '#03C75A',
};

const TEXT: Record<OAuthProvider, string> = {
  google: '#191919',
  kakao: '#191919',
  naver: '#FFFFFF',
};

const BORDER: Record<OAuthProvider, string> = {
  google: '#E5E7EB',
  kakao: 'transparent',
  naver: 'transparent',
};

export const Button = styled.button<{ $provider: OAuthProvider }>`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  padding: 14px 0;
  border: 1px solid ${({ $provider }) => BORDER[$provider]};
  border-radius: 12px;
  background-color: ${({ $provider }) => BG[$provider]};
  color: ${({ $provider }) => TEXT[$provider]};
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s ease;

  &:active {
    opacity: 0.85;
  }
`;
