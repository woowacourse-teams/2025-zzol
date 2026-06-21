import styled from '@emotion/styled';
import { NunchiResultTier } from '@/types/miniGame/nunchiGame';

export const ResultList = styled.ul`
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  padding: 8px 0;
  list-style: none;
`;

export const ResultItem = styled.li<{ $isMe?: boolean; $tier: NunchiResultTier }>`
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  background: ${({ theme, $isMe }) => ($isMe ? theme.color.point[50] : theme.color.gray[50])};
  border: 1px solid
    ${({ theme, $isMe }) => ($isMe ? theme.color.point[300] : theme.color.gray[200])};
  opacity: ${({ $tier }) => ($tier === 'MISS' ? 0.7 : 1)};
`;

export const Rank = styled.span`
  min-width: 24px;
  font-size: 18px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.gray[700]};
  text-align: center;
`;

export const Name = styled.span<{ $isMe?: boolean }>`
  flex: 1;
  font-size: 15px;
  font-weight: ${({ $isMe }) => ($isMe ? 800 : 600)};
  color: ${({ theme }) => theme.color.gray[900]};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const Empty = styled.div`
  width: 100%;
  padding: 32px 0;
  text-align: center;
  font-size: 14px;
  color: ${({ theme }) => theme.color.gray[500]};
`;
