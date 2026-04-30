import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 12px 0 32px;
`;

export const SectionLabel = styled.p`
  font-size: 12px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[400]};
  padding: 0 20px;
  margin-bottom: 6px;
  letter-spacing: 0.01em;
`;

export const MenuCard = styled.div`
  margin: 0 16px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow: hidden;
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
  padding: 16px 20px;
  border: none;
  border-bottom: 1px solid ${({ theme }) => theme.color.gray[50]};
  background: transparent;
  cursor: pointer;
  transition: background 0.12s ease;
  text-align: left;

  li:last-of-type & {
    border-bottom: none;
  }

  &:active {
    background: ${({ theme }) => theme.color.gray[50]};
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
  font-size: 15px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[900]};
  letter-spacing: -0.01em;
`;

export const MenuItemDesc = styled.span`
  font-size: 12px;
  font-weight: 400;
  color: ${({ theme }) => theme.color.gray[400]};
  line-height: 1.4;
`;

export const MenuItemChevron = styled.span`
  color: ${({ theme }) => theme.color.gray[300]};
  font-size: 18px;
  font-weight: 400;
  line-height: 1;
  flex-shrink: 0;
`;

export const VersionText = styled.p`
  font-size: 11px;
  color: ${({ theme }) => theme.color.gray[300]};
  text-align: center;
  padding: 20px 0 0;
  letter-spacing: 0.02em;
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
