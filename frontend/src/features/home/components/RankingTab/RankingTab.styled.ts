import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  padding: 8px 0;
`;

export const AccordionItem = styled.div`
  border-bottom: 1px solid ${({ theme }) => theme.color.gray[100]};

  &:last-of-type {
    border-bottom: none;
  }
`;

export const AccordionHeader = styled.button`
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 14px 4px;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;

  &:active {
    background: ${({ theme }) => theme.color.gray[50]};
  }
`;

export const AccordionTitle = styled.span`
  display: flex;
  align-items: center;
  gap: 8px;
  ${({ theme }) => theme.typography.h4}
  color: ${({ theme }) => theme.color.gray[800]};
`;

export const ChevronIcon = styled.span<{ $isOpen: boolean }>`
  font-size: 18px;
  color: ${({ theme }) => theme.color.gray[400]};
  transform: ${({ $isOpen }) => ($isOpen ? 'rotate(180deg)' : 'rotate(0deg)')};
  transition: transform 0.2s ease;
  line-height: 1;
`;

export const AccordionBody = styled.div<{ $isOpen: boolean }>`
  max-height: ${({ $isOpen }) => ($isOpen ? '400px' : '0')};
  overflow: hidden;
  transition: max-height 0.25s ease;
`;

export const AccordionContent = styled.div`
  display: flex;
  flex-direction: column;
  padding: 0 0 12px;
  gap: 0.5rem;
`;

export const LoadingText = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[400]};
  text-align: center;
  padding: 12px 0;
`;

export const EmptyText = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[400]};
  text-align: center;
  padding: 12px 0;
`;
