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
