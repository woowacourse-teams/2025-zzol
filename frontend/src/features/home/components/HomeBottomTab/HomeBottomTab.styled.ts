import styled from '@emotion/styled';

export const TabBar = styled.nav`
  display: flex;
  flex-shrink: 0;
  background: ${({ theme }) => theme.color.white};
  border-top: 1px solid ${({ theme }) => theme.color.gray[100]};
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.06);
  padding-bottom: env(safe-area-inset-bottom);
`;

export const TabButton = styled.button<{ $active: boolean }>`
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  height: 56px;
  padding: 8px 0;
  border: none;
  background: transparent;
  cursor: pointer;
  color: ${({ theme, $active }) => ($active ? theme.color.point[500] : theme.color.gray[400])};
  transition: color 0.15s ease;

  &:active {
    background: ${({ theme }) => theme.color.gray[50]};
  }
`;

export const IconWrap = styled.div`
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
`;

export const TabBadge = styled.div`
  position: absolute;
  top: -2px;
  right: -4px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: ${({ theme }) => theme.color.point[400]};
  border: 2px solid ${({ theme }) => theme.color.white};
`;

export const TabLabel = styled.span<{ $active: boolean }>`
  font-size: 10px;
  font-weight: ${({ $active }) => ($active ? 600 : 500)};
  letter-spacing: -0.01em;
`;
