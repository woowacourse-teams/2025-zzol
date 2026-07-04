import styled from '@emotion/styled';
import { keyframes } from '@emotion/react';

const fadeIn = keyframes`
  from { opacity: 0; }
  to { opacity: 1; }
`;

// 도장이 "쾅" 찍히는 느낌 — 크게 들어와 살짝 비틀며 안착.
const stampIn = keyframes`
  0% { transform: scale(2.2) rotate(-16deg); opacity: 0; }
  55% { transform: scale(0.9) rotate(-8deg); opacity: 1; }
  75% { transform: scale(1.06) rotate(-12deg); }
  100% { transform: scale(1) rotate(-10deg); opacity: 1; }
`;

const riseIn = keyframes`
  from { transform: translateY(12px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
`;

export const Backdrop = styled.div`
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 24px;
  text-align: center;
  background: rgba(0, 0, 0, 0.78);
  backdrop-filter: blur(4px);
  animation: ${fadeIn} 0.25s ease-out;
`;

export const Stamp = styled.div`
  padding: 8px 32px;
  border: 4px solid ${({ theme }) => theme.color.point[500]};
  border-radius: 16px;
  font-size: clamp(48px, 16vw, 72px);
  font-weight: 800;
  letter-spacing: 0.1em;
  color: ${({ theme }) => theme.color.point[500]};
  text-shadow: 0 0 24px ${({ theme }) => theme.color.point[500]}59;
  box-shadow: 0 0 24px ${({ theme }) => theme.color.point[500]}40;
  animation: ${stampIn} 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) both;

  @media (prefers-reduced-motion: reduce) {
    animation: ${fadeIn} 0.3s ease-out both;
    transform: rotate(-10deg);
  }
`;

export const Message = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  animation: ${riseIn} 0.4s ease-out 0.3s both;

  @media (prefers-reduced-motion: reduce) {
    animation: ${fadeIn} 0.3s ease-out 0.2s both;
  }
`;

export const Headline = styled.p`
  ${({ theme }) => theme.typography.h3};
  color: ${({ theme }) => theme.color.white};
`;

export const Sub = styled.p`
  ${({ theme }) => theme.typography.small};
  color: ${({ theme }) => theme.color.gray[300]};
`;
