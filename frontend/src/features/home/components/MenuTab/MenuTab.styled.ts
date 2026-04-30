import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 8px 0;
`;

export const MenuList = styled.ul`
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
`;

export const MenuItemButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 14px 20px;
  border: none;
  border-radius: 12px;
  background: transparent;
  cursor: pointer;
  transition: background 0.15s ease;
  text-align: left;

  &:hover {
    background: ${({ theme }) => theme.color.gray[50]};
  }

  &:active {
    background: ${({ theme }) => theme.color.gray[100]};
    transform: scale(0.99);
  }
`;

export const MenuItemLeft = styled.div`
  display: flex;
  align-items: center;
  gap: 14px;
`;

export const MenuItemIcon = styled.div`
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: ${({ theme }) => theme.color.point[50]};
  border: 1px solid ${({ theme }) => theme.color.point[100]};
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
`;

export const MenuItemTexts = styled.div`
  display: flex;
  flex-direction: column;
  gap: 3px;
`;

export const MenuItemTitle = styled.span`
  ${({ theme }) => theme.typography.paragraph}
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[800]};
`;

export const MenuItemDesc = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[400]};
`;

export const MenuItemChevron = styled.span`
  color: ${({ theme }) => theme.color.gray[300]};
  font-size: 20px;
  font-weight: 300;
  line-height: 1;
`;

/* 서브뷰 레이아웃 */
export const SubViewContainer = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
`;

export const SubViewHeader = styled.div`
  padding: 8px 20px 0;
  flex-shrink: 0;
`;

export const SubViewContent = styled.div`
  flex: 1;
  overflow-y: auto;
  min-height: 0;
`;
