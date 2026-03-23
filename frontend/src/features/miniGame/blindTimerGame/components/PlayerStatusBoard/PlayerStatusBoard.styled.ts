import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  width: 100%;
  max-width: 360px;
  margin: 0 auto;
  padding: 0 8px;
`;

export const PlayerChip = styled.div<{ $stopped: boolean; $timedOut: boolean; $isMe: boolean }>`
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: ${({ $isMe }) => ($isMe ? 700 : 500)};
  background-color: ${({ theme, $stopped, $timedOut }) =>
    $timedOut ? theme.color.gray[200] : $stopped ? theme.color.point[50] : theme.color.gray[50]};
  color: ${({ theme, $stopped, $timedOut }) =>
    $timedOut ? theme.color.gray[400] : $stopped ? theme.color.point[500] : theme.color.gray[600]};
  border: 1px solid
    ${({ theme, $stopped, $timedOut, $isMe }) =>
      $isMe
        ? theme.color.point[400]
        : $timedOut
          ? theme.color.gray[300]
          : $stopped
            ? theme.color.point[200]
            : theme.color.gray[200]};
`;

export const StatusDot = styled.span<{ $stopped: boolean; $timedOut: boolean }>`
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: ${({ theme, $stopped, $timedOut }) =>
    $timedOut ? theme.color.gray[400] : $stopped ? theme.color.point[400] : '#4caf50'};
`;
