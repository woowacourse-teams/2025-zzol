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

export const Container = styled.div`
  position: absolute;
  top: 6rem;
  right: 1rem;
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-end;
  pointer-events: none;
  z-index: 10;
`;

export const RankList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

export const RankItem = styled.div<{ isMe?: boolean }>`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  background: ${({ isMe }) => (isMe ? 'rgba(72, 219, 251, 0.25)' : 'rgba(0, 0, 0, 0.4)')};
  border: 1px solid ${({ isMe }) => (isMe ? 'rgba(72, 219, 251, 0.4)' : 'rgba(255, 255, 255, 0.1)')};
  border-radius: 6px;
  backdrop-filter: blur(8px);
  min-width: 100px;
  animation: ${slideIn} 0.3s ease-out forwards;
`;

export const Rank = styled.span`
  font-size: 10px;
  font-weight: 800;
  color: #48dbfb;
  width: 14px;
`;

export const Name = styled.span`
  flex: 1;
  font-size: 12px;
  font-weight: 500;
  color: white;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const Floor = styled.span`
  font-size: 12px;
  font-weight: 700;
  color: #48dbfb;
`;
