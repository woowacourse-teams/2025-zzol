import styled from '@emotion/styled';

export const Container = styled.article`
  height: 100%;
  overflow-y: auto;
  padding: 24px 20px 48px;
  display: flex;
  flex-direction: column;
  gap: 32px;
`;

export const Title = styled.h1`
  font-size: 22px;
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[900]};
  line-height: 1.4;
`;

export const UpdatedAt = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.color.gray[500]};
  margin-top: -20px;
`;

export const Section = styled.section`
  display: flex;
  flex-direction: column;
  gap: 12px;
`;

export const SectionTitle = styled.h2`
  font-size: 16px;
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[800]};
`;

export const Body = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.color.gray[700]};
  line-height: 1.8;
  white-space: pre-line;
`;

export const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  color: ${({ theme }) => theme.color.gray[700]};
`;

export const Th = styled.th`
  background: ${({ theme }) => theme.color.gray[100]};
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  padding: 8px 10px;
  text-align: left;
  font-weight: 600;
`;

export const Td = styled.td`
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  padding: 8px 10px;
  vertical-align: top;
  line-height: 1.6;
`;
