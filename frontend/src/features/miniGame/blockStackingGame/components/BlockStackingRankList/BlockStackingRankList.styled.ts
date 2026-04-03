import styled from '@emotion/styled';
import { keyframes } from '@emotion/react';

const slideIn = keyframes`
  from {
    transform: translateX(20px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
`;

export const RankList = styled.div<{ isCentered?: boolean }>`
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: ${({ isCentered }) => (isCentered ? 'center' : 'flex-end')};
  width: ${({ isCentered }) => (isCentered ? '100%' : 'auto')};
`;

export const RankItem = styled.div<{ isMe?: boolean; isCentered?: boolean }>`
  display: flex;
  align-items: center;
  gap: 12px;
  padding: ${({ isCentered }) => (isCentered ? '8px 16px' : '4px 10px')};
  background: ${({ isMe }) => (isMe ? 'rgba(72, 219, 251, 0.25)' : 'rgba(0, 0, 0, 0.4)')};
  border: 1px solid ${({ isMe }) => (isMe ? 'rgba(72, 219, 251, 0.4)' : 'rgba(255, 255, 255, 0.1)')};
  border-radius: 8px;
  backdrop-filter: blur(8px);
  min-width: ${({ isCentered }) => (isCentered ? '220px' : '110px')};
  animation: ${slideIn} 0.3s ease-out forwards;
  transition: all 0.2s ease;
`;

export const Rank = styled.span`
  font-size: 11px;
  font-weight: 800;
  color: #48dbfb;
  width: 16px;
  text-align: center;
`;

export const Name = styled.span<{ isMe?: boolean }>`
  flex: 1;
  font-size: 13px;
  font-weight: ${({ isMe }) => (isMe ? '700' : '500')};
  color: white;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const Floor = styled.span`
  font-size: 13px;
  font-weight: 700;
  color: #48dbfb;
`;
