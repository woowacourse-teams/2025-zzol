import styled from '@emotion/styled';
import { css } from '@emotion/react';

export const srOnly = css`
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
`;

export const ScreenReaderContainer = styled.div`
  ${srOnly}
`;
