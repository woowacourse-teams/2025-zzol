import styled from '@emotion/styled';

type Props = { $color: string };

export const Container = styled.span<Props>`
  ${({ theme }) => theme.typography.small}
  color: ${({ $color }) => $color};
`;
