import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  max-width: 360px;
  margin: 0 auto;
  padding: 0 8px;
`;

export const BarWrapper = styled.div`
  flex: 1;
  height: 8px;
  background-color: ${({ theme }) => theme.color.gray[100]};
  border-radius: 4px;
  overflow: hidden;
`;

export const Bar = styled.div<{ $progress: number; $urgent: boolean }>`
  height: 100%;
  width: ${({ $progress }) => $progress * 100}%;
  background-color: ${({ theme, $urgent }) => ($urgent ? theme.color.red : theme.color.point[400])};
  border-radius: 4px;
  transition: width 0.1s linear;
`;

export const TimeText = styled.span<{ $urgent: boolean }>`
  min-width: 32px;
  font-size: 1rem;
  font-weight: 700;
  text-align: right;
  color: ${({ theme, $urgent }) => ($urgent ? theme.color.red : theme.color.gray[700])};
`;
