import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background-color: ${({ theme }) => theme.color.gray[800]};
  border-radius: 12px;
  width: fit-content;
  margin: 0 auto;
`;

export const Label = styled.span`
  font-size: 1rem;
  font-weight: 500;
  color: ${({ theme }) => theme.color.gray[300]};
`;

export const Time = styled.span`
  font-size: 1.4rem;
  font-weight: 700;
  color: ${({ theme }) => theme.color.yellow};
  font-variant-numeric: tabular-nums;
`;
