import styled from '@emotion/styled';

type Props = { $color: string };

export const Container = styled.h1<Props>`
  ${({ theme }) => theme.typography.h1};
  color: ${({ $color }) => $color};
`;
