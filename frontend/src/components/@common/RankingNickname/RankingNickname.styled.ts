import styled from '@emotion/styled';
import { css, keyframes } from '@emotion/react';

export type Tone = 'light' | 'dark';

const shimmer = keyframes`
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: -100% 0;
  }
`;

// 글자색은 배경 밴드가 지나가도 바뀌지 않는다. 대비는 밴드의 가장 밝은 지점 기준으로 맞췄다.
// light: gray-600 글자 / gray-100 배경 7:1, dark: 흰 글자 / 검정 10% 를 덮은 슬라이드 배경(원래보다 어두움)
export const Pending = styled.span<{ $tone: Tone }>`
  padding: 0 6px;
  border-radius: 6px;
  background-size: 200% 100%;
  animation: ${shimmer} 1.8s ease-in-out infinite;

  ${({ $tone, theme }) =>
    $tone === 'light'
      ? css`
          color: ${theme.color.gray[600]};
          background-image: linear-gradient(
            90deg,
            ${theme.color.gray[100]} 25%,
            ${theme.color.gray[200]} 50%,
            ${theme.color.gray[100]} 75%
          );
        `
      : css`
          color: ${theme.color.white};
          background-image: linear-gradient(
            90deg,
            ${theme.color.black}1A 25%,
            ${theme.color.black}33 50%,
            ${theme.color.black}1A 75%
          );
        `}

  @media (prefers-reduced-motion: reduce) {
    animation: none;
  }
`;

export const VisuallyHidden = styled.span`
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
`;
