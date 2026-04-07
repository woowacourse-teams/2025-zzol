import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  padding: 20px;
  gap: 10px;
`;

/* 개발중 placeholder */
export const PlaceholderCard = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 48px 24px;
  border-radius: 14px;
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  background: ${({ theme }) => theme.color.gray[50]};
`;

export const PlaceholderIcon = styled.span`
  font-size: 32px;
  line-height: 1;
`;

export const PlaceholderTitle = styled.p`
  ${({ theme }) => theme.typography.paragraph}
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[700]};
  margin: 0;
`;

export const PlaceholderSub = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[400]};
  margin: 0;
  text-align: center;
`;

/* 아코디언 카드 */
export const AccordionCard = styled.div`
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  border-radius: 14px;
  overflow: hidden;
  background: ${({ theme }) => theme.color.white};
`;

export const AccordionHeader = styled.button<{ $open: boolean }>`
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
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
  align-items: baseline;
  gap: 8px;
`;

export const VersionBadge = styled.span`
  ${({ theme }) => theme.typography.paragraph}
  font-weight: 700;
  color: ${({ theme }) => theme.color.point[400]};
`;

export const VersionDate = styled.span`
  ${({ theme }) => theme.typography.small}
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
  padding: 11px 16px;

  &:not(:last-child) {
    border-bottom: 1px solid ${({ theme }) => theme.color.gray[100]};
  }
`;

export const ChangeType = styled.span<{ $type: 'new' | 'fix' | 'improve' }>`
  ${({ theme }) => theme.typography.small}
  font-weight: 600;
  padding: 1px 7px;
  border-radius: 6px;
  flex-shrink: 0;
  margin-top: 1px;

  ${({ theme, $type }) => {
    if ($type === 'new') {
      return `
        background: ${theme.color.point[50]};
        color: ${theme.color.point[400]};
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
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[700]};
  line-height: 1.6;
`;
