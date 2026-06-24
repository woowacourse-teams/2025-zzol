import styled from '@emotion/styled';

export const Container = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[400]};
`;
