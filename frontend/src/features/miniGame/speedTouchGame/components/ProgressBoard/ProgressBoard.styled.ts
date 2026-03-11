import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  max-width: 360px;
  margin: 0 auto;
  padding: 0 8px;
`;

export const PlayerRow = styled.div<{ $isMe: boolean }>`
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
  font-weight: ${({ $isMe }) => ($isMe ? 700 : 500)};
  color: ${({ theme, $isMe }) => ($isMe ? theme.color.point[500] : theme.color.gray[600])};
`;

export const PlayerName = styled.span`
  min-width: 48px;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const BarWrapper = styled.div`
  flex: 1;
  height: 14px;
  background-color: ${({ theme }) => theme.color.gray[100]};
  border-radius: 7px;
  overflow: hidden;
`;

export const Bar = styled.div<{ $progress: number; $isMe: boolean }>`
  height: 100%;
  width: ${({ $progress }) => ($progress / 25) * 100}%;
  background-color: ${({ theme, $isMe }) =>
    $isMe ? theme.color.point[400] : theme.color.gray[300]};
  border-radius: 7px;
  transition: width 0.2s ease;
`;

export const Count = styled.span`
  min-width: 32px;
  text-align: left;
  font-size: 0.75rem;
`;
