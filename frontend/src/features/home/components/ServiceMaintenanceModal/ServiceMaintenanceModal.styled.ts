// TODO: 점검 종료 후 삭제 예정 - 서비스 점검 안내 모달 스타일
import styled from '@emotion/styled';

export const ContentContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: 20px 24px;
  gap: 1rem;
`;

export const Description = styled.p`
  ${({ theme }) => theme.typography.paragraph}
  color: ${({ theme }) => theme.color.gray[700]};
  text-align: center;
  line-height: 1.6;
`;

export const PeriodContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  background-color: ${({ theme }) => theme.color.gray[100]};
  border-radius: 8px;
  width: 100%;
  max-width: 300px;
`;

export const PeriodLabel = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[500]};
  text-align: center;
  margin: 0;
`;

export const PeriodDate = styled.p`
  ${({ theme }) => theme.typography.paragraph}
  color: ${({ theme }) => theme.color.gray[700]};
  text-align: center;
  margin: 0;
  font-weight: 600;
`;
