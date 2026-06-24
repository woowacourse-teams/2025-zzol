import { LAYOUT_PADDING } from '@/constants/padding';
import styled from '@emotion/styled';

type Props = {
  $height?: string;
};

export const Container = styled.section<Props>`
  width: calc(100% + ${LAYOUT_PADDING} * 2);
  height: ${({ $height }) => $height};
  background-color: ${({ theme }) => theme.color.point[400]};

  margin: -${LAYOUT_PADDING} -${LAYOUT_PADDING} ${LAYOUT_PADDING};

  border-radius: 0 0 12px 12px;
`;
