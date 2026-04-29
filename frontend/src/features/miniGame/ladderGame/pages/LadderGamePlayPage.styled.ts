import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
`;

export const BoardWrapper = styled.div`
  flex: 1;
  overflow: hidden;
`;

export const TimerBarWrapper = styled.div`
  height: 5px;
  background: ${({ theme }) => theme.color.gray[200]};
  flex-shrink: 0;
  overflow: hidden;
`;

export const TimerBarFill = styled.div<{ $timeLeft: number; $totalTime: number }>`
  height: 100%;
  width: ${({ $timeLeft, $totalTime }) => ($timeLeft / $totalTime) * 100}%;
  background: ${({ $timeLeft }) =>
    $timeLeft < 5
      ? 'linear-gradient(to right, #ff4d4d, #ff9f43)'
      : 'linear-gradient(to right, #48dbfb, #1dd1a1)'};
  box-shadow: 0 0 8px
    ${({ $timeLeft }) => ($timeLeft < 5 ? 'rgba(255, 77, 77, 0.5)' : 'rgba(72, 219, 251, 0.3)')};
`;
