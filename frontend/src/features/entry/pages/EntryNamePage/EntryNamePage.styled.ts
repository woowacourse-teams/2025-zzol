import styled from '@emotion/styled';

export const Container = styled.section`
  display: flex;
  flex-direction: column;
  gap: 35px;
  justify-content: center;
  height: 100%;
`;

export const Wrapper = styled.section`
  display: flex;
  flex-direction: column;
  gap: 10px;
`;

export const HeadlineRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
`;

export const RandomButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  flex-shrink: 0;

  height: 32px;
  padding: 0 12px;
  border: none;
  border-radius: 16px;
  background: ${({ theme }) => theme.color.point[50]};
  cursor: pointer;
  transition: all 0.15s ease;

  color: ${({ theme }) => theme.color.point[400]};
  font-size: 14px;
  font-weight: 600;

  &:active {
    transform: scale(0.95);
    background: ${({ theme }) => theme.color.point[100]};
  }

  &:disabled {
    cursor: default;
    opacity: 0.5;
    transform: none;
  }
`;

export const ProgressWrapper = styled.div`
  display: flex;
  justify-content: flex-end;
`;
