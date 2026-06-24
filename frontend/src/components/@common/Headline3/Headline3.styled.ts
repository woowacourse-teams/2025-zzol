import styled from '@emotion/styled';

type Props = { $color: string };

export const Container = styled.h3<Props>`
  ${({ theme }) => theme.typography.h3};
  color: ${({ $color }) => $color};
`;
