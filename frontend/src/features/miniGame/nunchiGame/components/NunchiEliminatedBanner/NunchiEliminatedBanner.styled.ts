import styled from '@emotion/styled';
import { keyframes } from '@emotion/react';

const dropIn = keyframes`
  from { transform: translateY(-8px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
`;

// 플레이 화면 최상단 탈락자 배너(요구사항 2) — 또렷하게 보이도록 point 톤 박스로 띄운다.
export const Banner = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 12px;
  border-radius: 12px;
  background: ${({ theme }) => theme.color.point[50]};
  border: 1px solid ${({ theme }) => theme.color.point[200]};
  flex-shrink: 0;
  animation: ${dropIn} 0.25s ease-out;

  @media (prefers-reduced-motion: reduce) {
    animation: none;
  }
`;

export const Label = styled.span`
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 999px;
  ${({ theme }) => theme.typography.caption};
  font-weight: 800;
  letter-spacing: 0.06em;
  color: ${({ theme }) => theme.color.white};
  background: ${({ theme }) => theme.color.point[500]};
`;

export const Names = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
`;

export const Chip = styled.span<{ $isMe?: boolean }>`
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 999px;
  ${({ theme }) => theme.typography.small};
  font-weight: ${({ $isMe }) => ($isMe ? 800 : 600)};
  color: ${({ theme, $isMe }) => ($isMe ? theme.color.point[500] : theme.color.gray[700])};
  background: ${({ theme }) => theme.color.white};
  border: 1px solid
    ${({ theme, $isMe }) => ($isMe ? theme.color.point[300] : theme.color.gray[200])};

  /* 깔끔한 ✕ 마커로 "out" 을 표시. */
  &::before {
    content: '✕';
    font-size: 0.85em;
    color: ${({ theme }) => theme.color.point[400]};
  }
`;
