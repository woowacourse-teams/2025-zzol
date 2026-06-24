import styled from '@emotion/styled';

export const PaginationContainer = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-around;
`;

export const PaginationButton = styled.button`
  background: none;
  border: none;
  color: ${({ theme }) => theme.color.gray[800]};
  ${({ theme }) => theme.typography.paragraph}
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  min-width: 60px;

  &:disabled {
    color: ${({ theme }) => theme.color.gray[300]};
    cursor: not-allowed;
  }
`;

export const DotsContainer = styled.div`
  display: flex;
  gap: 8px;
`;

export const Dot = styled.div<{ active: boolean }>`
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: ${({ theme, active }) => (active ? theme.color.point[500] : theme.color.gray[200])};
  transition: all 0.2s ease;
`;
