import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  padding-bottom: 16px;
`;

export const StatsSection = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 24px 0 8px;
`;

export const StatsSectionTitle = styled.h2`
  font-size: 16px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.gray[900]};
  letter-spacing: -0.02em;
  padding: 0 16px;
`;

export const Divider = styled.div`
  height: 8px;
  background: ${({ theme }) => theme.color.gray[50]};
  margin: 8px 0;
`;

export const RankingSection = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 8px 16px 16px;
`;

export const AccordionCard = styled.div`
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow: hidden;
`;

export const AccordionItem = styled.div`
  border-bottom: 1px solid ${({ theme }) => theme.color.gray[50]};

  &:last-of-type {
    border-bottom: none;
  }
`;

export const AccordionHeader = styled.button`
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 16px 20px;
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
  padding: 0 12px 16px;
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
