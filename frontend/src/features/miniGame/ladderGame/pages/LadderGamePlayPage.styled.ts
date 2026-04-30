import styled from '@emotion/styled';

const DANGER_THRESHOLD_SECONDS = 5;

const TIMER_COLORS = {
  danger: { from: '#ff4d4d', to: '#ff9f43', shadow: 'rgba(255, 77, 77, 0.5)' },
  normal: { from: '#48dbfb', to: '#1dd1a1', shadow: 'rgba(72, 219, 251, 0.3)' },
} as const;

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
  background: ${({ $timeLeft }) => {
    const c = $timeLeft < DANGER_THRESHOLD_SECONDS ? TIMER_COLORS.danger : TIMER_COLORS.normal;
    return `linear-gradient(to right, ${c.from}, ${c.to})`;
  }};
  box-shadow: 0 0 8px
    ${({ $timeLeft }) =>
      $timeLeft < DANGER_THRESHOLD_SECONDS
        ? TIMER_COLORS.danger.shadow
        : TIMER_COLORS.normal.shadow};
`;
