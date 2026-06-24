import styled from '@emotion/styled';

export const SuffixRow = styled.div`
  display: flex;
  align-items: center;
  flex: 1;
  gap: 8px;
`;

export const SettingsButton = styled.button`
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  margin-left: auto;
  background-color: ${({ theme }) => theme.color.point[50]};
  border: 1px solid ${({ theme }) => theme.color.point[200]};
  border-radius: 20px;
  color: ${({ theme }) => theme.color.point[400]};
  ${({ theme }) => theme.typography.small};
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color 0.15s ease;

  &:active {
    background-color: ${({ theme }) => theme.color.point[100]};
  }
`;

export const ScrollableWrapper = styled.div`
  overflow-y: auto;
  margin-bottom: 1rem;
  height: 100%;
`;

export const BottomGap = styled.div`
  height: 3rem;
`;

export const Empty = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  font-size: 1rem;
  color: ${({ theme }) => theme.color.gray[500]};
`;
