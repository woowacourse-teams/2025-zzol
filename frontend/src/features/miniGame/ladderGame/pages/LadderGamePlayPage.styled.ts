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

export const TimerBarFill = styled.div<{ $ratio: number }>`
  height: 100%;
  width: ${({ $ratio }) => $ratio * 100}%;
  background: ${({ theme }) => theme.color.point[400]};
  transition: width 0.05s linear;
`;
