import styled from '@emotion/styled';

export const TabBar = styled.nav`
  display: flex;
  flex-shrink: 0;
  background: ${({ theme }) => theme.color.white};
  border-top: 1px solid ${({ theme }) => theme.color.gray[200]};
  /* Layout padding(1rem) 상쇄 → 화면 하단 엣지까지 확장 */
  margin: 0 -1rem -1rem;
  padding-bottom: env(safe-area-inset-bottom);
`;

export const TabButton = styled.button<{ $active: boolean }>`
  flex: 1;
  height: 44px;
  border: none;
  background: transparent;
  cursor: pointer;
  position: relative;

  ${({ theme }) => theme.typography.h4}
  color: ${({ theme, $active }) => ($active ? theme.color.gray[800] : theme.color.gray[400])};
  font-weight: ${({ $active }) => ($active ? 600 : 500)};
  transition: color 0.15s ease;

  &::after {
    content: '';
    position: absolute;
    top: -1px;
    left: 0;
    width: 100%;
    height: 2px;
    background: ${({ theme }) => theme.color.point[400]};
    opacity: ${({ $active }) => ($active ? 1 : 0)};
    transition: opacity 0.15s ease;
  }

  &:active {
    background: ${({ theme }) => theme.color.gray[50]};
  }
`;
