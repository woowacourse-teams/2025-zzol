import styled from '@emotion/styled';
import { css } from '@emotion/react';

export const Container = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
`;

export const PlayerChip = styled.div<{ $eliminated: boolean; $isCurrentTurn: boolean }>`
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 600;
  transition: all 0.2s;

  ${({ $eliminated, $isCurrentTurn }) => {
    if ($eliminated) {
      return css`
        background-color: #f0f0f0;
        color: #bbb;
        text-decoration: line-through;
      `;
    }
    if ($isCurrentTurn) {
      return css`
        background-color: #ff6b6b;
        color: white;
        animation: pulse 1s ease-in-out infinite;

        @keyframes pulse {
          0%,
          100% {
            transform: scale(1);
          }
          50% {
            transform: scale(1.05);
          }
        }
      `;
    }
    return css`
      background-color: #fff0f0;
      color: #333;
    `;
  }}
`;

export const BombEmoji = styled.span`
  font-size: 1rem;
`;
