import styled from '@emotion/styled';

type Props = { $color: string };

export const Container = styled.h4<Props>`
  ${({ theme }) => theme.typography.h4};
  color: ${({ $color }) => $color};
`;
