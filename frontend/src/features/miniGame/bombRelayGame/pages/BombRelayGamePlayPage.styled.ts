import styled from '@emotion/styled';
import { css, keyframes } from '@emotion/react';

const pulseGlow = keyframes`
  0%, 100% { box-shadow: 0 0 0 0 rgba(255, 107, 107, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(255, 107, 107, 0); }
`;

const bannerPulse = keyframes`
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.02); }
`;

export const Container = styled.div<{ $isMyTurn: boolean }>`
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  padding: 16px 0;
  transition: background-color 0.3s ease;

  ${({ $isMyTurn }) =>
    $isMyTurn &&
    css`
      background-color: rgba(255, 107, 107, 0.03);
    `}
`;

export const TurnBanner = styled.div<{ $isMyTurn: boolean }>`
  flex-shrink: 0;
  text-align: center;
  padding: 12px 16px;
  border-radius: 12px;
  margin: 0 16px;
  font-weight: 700;
  font-size: 1.1rem;
  transition: all 0.3s ease;

  ${({ $isMyTurn }) =>
    $isMyTurn
      ? css`
          background: linear-gradient(135deg, #ff6b6b, #ff8e53);
          color: white;
          animation: ${pulseGlow} 1.5s ease-in-out infinite, ${bannerPulse} 1.5s ease-in-out infinite;
        `
      : css`
          background-color: #f0f0f0;
          color: #888;
        `}
`;

export const RoundSection = styled.div`
  flex-shrink: 0;
`;

export const WordSection = styled.div`
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  padding: 16px 0;
`;

export const FeedbackSection = styled.div`
  flex-shrink: 0;
  min-height: 36px;
`;

export const PlayerSection = styled.div`
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
`;

export const InputSection = styled.div`
  flex-shrink: 0;
  padding: 0 16px;
`;
