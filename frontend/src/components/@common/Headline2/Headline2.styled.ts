import styled from '@emotion/styled';

type Props = { $color: string };

export const Container = styled.h2<Props>`
  ${({ theme }) => theme.typography.h2};
  color: ${({ $color }) => $color};
`;
