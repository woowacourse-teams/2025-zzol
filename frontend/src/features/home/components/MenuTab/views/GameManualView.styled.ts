import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px 16px 32px;
`;

export const HintBanner = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: ${({ theme }) => theme.color.point[50]};
  border-radius: 12px;
  border-left: 3px solid ${({ theme }) => theme.color.point[400]};
`;

export const Subtitle = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.point[500]};
  margin: 0;
  line-height: 1.5;
`;
