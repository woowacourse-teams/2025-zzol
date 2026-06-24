import styled from '@emotion/styled';

type Props = {
  $color?: string;
  $padding?: string;
};

export const LayoutContainer = styled.div<Props>`
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  padding: ${({ $padding }) => $padding};
  background-color: ${({ $color }) => $color};
`;
