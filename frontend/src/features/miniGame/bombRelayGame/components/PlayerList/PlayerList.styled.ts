import styled from '@emotion/styled';
import { css, keyframes } from '@emotion/react';

const bombBounce = keyframes`
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
`;

export const Container = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  padding: 0 16px;
`;

export const PlayerChip = styled.div<{
  $eliminated: boolean;
  $isCurrentTurn: boolean;
  $isMe: boolean;
}>`
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 24px;
  font-size: 0.85rem;
  font-weight: 600;
  transition: all 0.3s ease;
  position: relative;

  ${({ $eliminated, $isCurrentTurn, $isMe }) => {
    if ($eliminated) {
      return css`
        background-color: #f5f5f5;
        color: #ccc;
        text-decoration: line-through;
      `;
    }
    if ($isCurrentTurn) {
      return css`
        background: linear-gradient(135deg, #ff6b6b, #ff8e53);
        color: white;
        box-shadow: 0 3px 12px rgba(255, 107, 107, 0.35);
      `;
    }
    if ($isMe) {
      return css`
        background-color: #fff0f0;
        color: #ff6b6b;
        border: 1.5px solid #ffcdd2;
      `;
    }
    return css`
      background-color: #f8f8f8;
      color: #666;
    `;
  }}
`;

export const BombEmoji = styled.span`
  font-size: 1rem;
  animation: ${bombBounce} 0.8s ease-in-out infinite;
`;

export const MeTag = styled.span`
  font-size: 0.65rem;
  font-weight: 700;
  opacity: 0.7;
`;
