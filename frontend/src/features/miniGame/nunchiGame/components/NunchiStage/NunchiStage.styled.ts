import styled from '@emotion/styled';
import { keyframes } from '@emotion/react';

const popIn = keyframes`
  from {
    transform: translateY(12px) scale(0.9);
    opacity: 0;
  }
  to {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
`;

const shake = keyframes`
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-6px); }
  40% { transform: translateX(6px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(4px); }
`;

export const Stage = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 100%;
  flex: 1;
  min-height: 0;
`;

export const Number = styled.div`
  font-size: 96px;
  font-weight: 800;
  line-height: 1;
  color: ${({ theme }) => theme.color.point[400]};
`;

export const IdleTimer = styled.div`
  font-size: 16px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[500]};
`;

export const StoodList = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 0 8px;
`;

export const StoodName = styled.span<{ $justStood?: boolean; $isMe?: boolean }>`
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: ${({ $isMe }) => ($isMe ? 800 : 600)};
  background: ${({ theme, $isMe }) => ($isMe ? theme.color.point[100] : theme.color.gray[100])};
  color: ${({ theme, $isMe }) => ($isMe ? theme.color.point[500] : theme.color.gray[700])};
  animation: ${({ $justStood }) => ($justStood ? popIn : 'none')} 0.25s ease-out;
`;

export const OutList = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 0 8px;
`;

export const OutName = styled.span<{ $isMe?: boolean }>`
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: ${({ $isMe }) => ($isMe ? 800 : 500)};
  color: ${({ theme }) => theme.color.gray[500]};
  background: ${({ theme }) => theme.color.gray[100]};
  text-decoration: line-through;
`;

export const Cooldown = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  animation: ${shake} 0.4s ease-in-out;
`;

export const CooldownTitle = styled.div`
  font-size: 40px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.red};
`;

export const CollidedList = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px;
`;

export const CollidedName = styled.span<{ $isMe?: boolean }>`
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: ${({ $isMe }) => ($isMe ? 800 : 600)};
  background: ${({ theme }) => theme.color.point[100]};
  color: ${({ theme }) => theme.color.point[500]};
  border: 2px solid ${({ theme }) => theme.color.point[400]};
`;

export const CooldownTimer = styled.div`
  font-size: 18px;
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[600]};
`;
