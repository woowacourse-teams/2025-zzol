import styled from '@emotion/styled';
import { css, keyframes } from '@emotion/react';

const pulseGlow = keyframes`
  0%, 100% { box-shadow: 0 0 0 0 rgba(255, 107, 107, 0.3); }
  50% { box-shadow: 0 0 0 6px rgba(255, 107, 107, 0); }
`;

export const Container = styled.div<{ $isMyTurn: boolean }>`
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 12px 0 16px;
  transition: background-color 0.4s ease;
  background-color: ${({ $isMyTurn }) => ($isMyTurn ? '#fff8f6' : '#fafafa')};
`;

export const RoundSection = styled.div`
  flex-shrink: 0;
  margin-bottom: 8px;
`;

export const TurnBanner = styled.div<{ $isMyTurn: boolean }>`
  flex-shrink: 0;
  text-align: center;
  padding: 10px 20px;
  margin: 0 20px 12px;
  border-radius: 16px;
  font-weight: 700;
  font-size: 0.95rem;
  transition: all 0.3s ease;

  ${({ $isMyTurn }) =>
    $isMyTurn
      ? css`
          background: linear-gradient(135deg, #ff6b6b, #ff8e53);
          color: white;
          animation: ${pulseGlow} 1.5s ease-in-out infinite;
        `
      : css`
          background-color: #f0f0f0;
          color: #999;
        `}
`;

export const WordSection = styled.div`
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 0;
`;

export const FeedbackSection = styled.div`
  flex-shrink: 0;
  min-height: 36px;
  padding: 0 20px;
`;

export const PlayerSection = styled.div`
  flex-shrink: 0;
  padding: 12px 0;
`;

export const InputSection = styled.div`
  flex-shrink: 0;
  padding: 0 20px;
`;
