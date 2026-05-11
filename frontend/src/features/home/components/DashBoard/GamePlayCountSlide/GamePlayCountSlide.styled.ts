import styled from '@emotion/styled';
import { keyframes } from '@emotion/react';

export const Card = styled.div`
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
`;

export const CardTitle = styled.h3`
  ${({ theme }) => theme.typography.paragraph}
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[800]};
  letter-spacing: -0.01em;
`;

export const List = styled.ul`
  display: flex;
  flex-direction: column;
  gap: 10px;
  list-style: none;
  padding: 0;
  margin: 0;
`;

const fadeIn = keyframes`
  from { opacity: 0; }
  to   { opacity: 1; }
`;

export const Item = styled.li<{ $index: number }>`
  display: flex;
  align-items: center;
  gap: 10px;
  animation: ${fadeIn} 0.3s ease both;
  animation-delay: ${({ $index }) => `${$index * 0.08}s`};
`;

export const GameRank = styled.span`
  width: 18px;
  ${({ theme }) => theme.typography.caption}
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[400]};
  flex-shrink: 0;
  text-align: center;
`;

export const GameInfo = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
`;

export const GameName = styled.span`
  ${({ theme }) => theme.typography.small}
  font-weight: 500;
  color: ${({ theme }) => theme.color.gray[800]};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

export const BarTrack = styled.div`
  width: 100%;
  height: 6px;
  background: ${({ theme }) => theme.color.gray[100]};
  border-radius: 3px;
  overflow: hidden;
`;

export const BarFill = styled.div<{ $ratio: number }>`
  height: 100%;
  width: ${({ $ratio }) => Math.round($ratio * 100)}%;
  background: linear-gradient(
    90deg,
    ${({ theme }) => theme.color.point[400]},
    ${({ theme }) => theme.color.point[300]}
  );
  border-radius: 3px;
  transition: width 0.6s ease;
`;

export const PlayCount = styled.span`
  ${({ theme }) => theme.typography.caption}
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[500]};
  flex-shrink: 0;
  min-width: 32px;
  text-align: right;
`;

export const Empty = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[400]};
  text-align: center;
  padding: 16px 0;
`;
