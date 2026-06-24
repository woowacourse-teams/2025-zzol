import styled from '@emotion/styled';

type Props = {
  $color?: string;
  $height?: string;
  $width?: string;
};

export const Container = styled.hr<Props>`
  border: none;
  width: ${({ $width }) => $width};
  height: ${({ $height }) => $height};
  min-height: ${({ $height }) => $height};
  background-color: ${({ theme, $color }) => $color || theme.color.point[200]};
  margin: 0;
  padding: 0;
`;
