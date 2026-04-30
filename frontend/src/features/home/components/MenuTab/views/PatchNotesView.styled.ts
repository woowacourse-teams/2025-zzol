import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  padding: 20px 16px 32px;
  gap: 10px;
`;

export const PageHeader = styled.div`
  display: flex;
  flex-direction: column;
  gap: 3px;
  margin-bottom: 4px;
`;

export const PageTitle = styled.h3`
  font-size: 17px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.gray[900]};
  letter-spacing: -0.02em;
  margin: 0;
`;

export const PageSub = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.color.gray[400]};
  margin: 0;
`;

/* 개발중 placeholder */
export const PlaceholderCard = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 52px 24px;
  border-radius: 16px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
`;

export const PlaceholderIconWrap = styled.div`
  width: 52px;
  height: 52px;
  border-radius: 16px;
  background: ${({ theme }) => theme.color.point[50]};
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  line-height: 1;
`;

export const PlaceholderTitle = styled.p`
  font-size: 15px;
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[700]};
  margin: 0;
  letter-spacing: -0.01em;
`;

export const PlaceholderSub = styled.p`
  font-size: 12px;
  color: ${({ theme }) => theme.color.gray[400]};
  margin: 0;
  text-align: center;
  line-height: 1.5;
`;

/* 아코디언 카드 */
export const AccordionCard = styled.div`
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 16px;
  overflow: hidden;
  background: ${({ theme }) => theme.color.white};
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
`;

export const AccordionHeader = styled.button<{ $open: boolean }>`
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  background: ${({ theme, $open }) => ($open ? theme.color.point[50] : theme.color.white)};
  border: none;
  cursor: pointer;
  transition: background 0.15s ease;

  &:active {
    background: ${({ theme }) => theme.color.point[50]};
  }
`;

export const AccordionHeaderLeft = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
`;

export const VersionBadge = styled.span`
  font-size: 14px;
  font-weight: 700;
  color: ${({ theme }) => theme.color.point[500]};
`;

export const VersionDate = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.color.gray[400]};
`;

export const ChevronIcon = styled.span<{ $open: boolean }>`
  color: ${({ theme }) => theme.color.gray[400]};
  font-size: 18px;
  transition: transform 0.2s ease;
  transform: ${({ $open }) => ($open ? 'rotate(90deg)' : 'rotate(0deg)')};
  display: inline-block;
  line-height: 1;
`;

export const AccordionBody = styled.div`
  border-top: 1px solid ${({ theme }) => theme.color.gray[100]};
`;

export const ChangeItem = styled.div`
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 18px;

  &:not(:last-child) {
    border-bottom: 1px solid ${({ theme }) => theme.color.gray[50]};
  }
`;

export const ChangeType = styled.span<{ $type: 'new' | 'fix' | 'improve' }>`
  font-size: 10px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 6px;
  flex-shrink: 0;
  margin-top: 2px;
  letter-spacing: 0.03em;

  ${({ theme, $type }) => {
    if ($type === 'new') {
      return `
        background: ${theme.color.point[50]};
        color: ${theme.color.point[500]};
        border: 1px solid ${theme.color.point[100]};
      `;
    }
    if ($type === 'fix') {
      return `
        background: #EFF6FF;
        color: #3B82F6;
        border: 1px solid #BFDBFE;
      `;
    }
    return `
      background: ${theme.color.gray[100]};
      color: ${theme.color.gray[600]};
      border: 1px solid ${theme.color.gray[200]};
    `;
  }}
`;

export const ChangeText = styled.span`
  font-size: 13px;
  color: ${({ theme }) => theme.color.gray[700]};
  line-height: 1.6;
`;
