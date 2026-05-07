import styled from '@emotion/styled';
import { keyframes } from '@emotion/react';
import { rankColorMap, type RankColorKey } from '@/constants/color';

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
  gap: 2px;
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
  gap: 12px;
  padding: 10px 8px;
  border-bottom: 1px solid ${({ theme }) => theme.color.gray[50]};
  border-radius: 8px;
  transition: background 0.15s ease;
  animation: ${fadeIn} 0.6s ease both;
  animation-delay: ${({ $index }) => `${$index * 0.16}s`};

  &:first-of-type {
    background: ${rankColorMap[1]}0F;
  }

  &:last-of-type {
    border-bottom: none;
  }
`;

export const Rank = styled.span<{ $rank: number }>`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  ${({ theme }) => theme.typography.caption}
  font-weight: 800;
  flex-shrink: 0;
  background: ${({ $rank }) => rankColorMap[$rank as RankColorKey] ?? 'transparent'};
  color: ${({ $rank, theme }) => ($rank <= 3 ? theme.color.white : theme.color.gray[900])};
  border: ${({ $rank, theme }) => ($rank > 3 ? `1px solid ${theme.color.gray[200]}` : 'none')};
`;

export const NameWrapper = styled.div`
  flex: 1;
  display: flex;
  align-items: baseline;
  gap: 5px;
  min-width: 0;
`;

export const Name = styled.span`
  ${({ theme }) => theme.typography.h4}
  color: ${({ theme }) => theme.color.gray[800]};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

export const UserCode = styled.span`
  ${({ theme }) => theme.typography.caption}
  font-weight: 500;
  color: ${({ theme }) => theme.color.gray[400]};
  flex-shrink: 0;
`;

export const Count = styled.span`
  ${({ theme }) => theme.typography.small}
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[500]};
  flex-shrink: 0;
`;

export const Empty = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[400]};
  text-align: center;
  padding: 16px 0;
`;
