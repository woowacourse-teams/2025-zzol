import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 16px 16px 40px;
`;

export const CardGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
`;

export const Section = styled.div`
  display: flex;
  flex-direction: column;
  gap: 14px;
`;

export const SectionHeader = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: space-between;
`;

export const SectionTitle = styled.h2`
  font-size: 16px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.gray[900]};
  letter-spacing: -0.02em;
`;

export const SectionSub = styled.span`
  font-size: 11px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[500]};
  background: ${({ theme }) => theme.color.gray[100]};
  padding: 3px 10px;
  border-radius: 20px;
  line-height: 1.4;
`;

export const MyInfoCard = styled.div`
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  overflow: hidden;
`;


export const MyInfoStatGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
`;

export const MyInfoStat = styled.div`
  padding: 18px 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;

  &:first-of-type {
    border-right: 1px solid ${({ theme }) => theme.color.gray[100]};
  }
`;

export const MyInfoLabel = styled.span`
  font-size: 12px;
  font-weight: 500;
  color: ${({ theme }) => theme.color.gray[400]};
  letter-spacing: -0.01em;
`;

export const MyInfoValueRow = styled.div`
  display: flex;
  align-items: baseline;
  gap: 2px;
`;

export const MyInfoNumber = styled.span`
  font-size: 30px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.gray[900]};
  letter-spacing: -0.04em;
  line-height: 1;
`;

export const MyInfoUnit = styled.span`
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[500]};
  letter-spacing: -0.02em;
`;

export const ScrollIndicator = styled.div`
  position: sticky;
  bottom: 0;
  left: 0;
  right: 0;
  height: 48px;
  display: flex;
  justify-content: center;
  align-items: flex-end;
  padding-bottom: 8px;
  background: linear-gradient(to bottom, transparent, ${({ theme }) => theme.color.gray[50]}cc);
  pointer-events: none;
`;

export const ScrollChevron = styled.span`
  font-size: 18px;
  color: ${({ theme }) => theme.color.gray[400]};
  animation: bounce 1.4s ease-in-out infinite;

  @keyframes bounce {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(4px); }
  }
`;
