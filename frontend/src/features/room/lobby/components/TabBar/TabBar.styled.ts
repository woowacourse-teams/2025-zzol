import styled from '@emotion/styled';

export const Container = styled.div`
  position: relative;
  display: flex;
  border-bottom: 1px solid ${({ theme }) => theme.color.gray[200]};
`;

export const Tab = styled.button<{ isActive: boolean }>`
  flex: 1;
  padding: 1rem;
  font-weight: ${({ isActive }) => (isActive ? '600' : '400')};
  font-size: 16px;
  color: ${({ theme, isActive }) => (isActive ? theme.color.point[400] : theme.color.gray[600])};
  border: none;
  background: transparent;
  cursor: pointer;
  transition: color 0.2s ease;
  position: relative;

  &:hover {
    color: ${({ theme }) => theme.color.point[400]};
  }
`;

export const Indicator = styled.div`
  position: absolute;
  bottom: 0;
  height: 2px;
  background-color: ${({ theme }) => theme.color.point[400]};
  transition:
    transform 0.3s ease,
    width 0.3s ease;
`;
