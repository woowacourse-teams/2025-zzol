import styled from '@emotion/styled';

export const Grid = styled.div`
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 6px;
  width: 100%;
  max-width: 360px;
  margin: 0 auto;
  padding: 0 8px;
`;

export const NextIndicator = styled.div`
  text-align: center;
  margin-bottom: 12px;
  font-size: 1rem;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[700]};
`;

export const NextNumber = styled.span`
  color: ${({ theme }) => theme.color.point[500]};
  font-size: 1.4rem;
  font-weight: 700;
`;
