import { keyframes } from '@emotion/react';

export const scoreboardReveal = keyframes`
  0%   { opacity: 0; transform: translateX(-28px); }
  55%  { opacity: 1; transform: translateX(4px); }
  78%  { transform: translateX(-2px); }
  100% { opacity: 1; transform: translateX(0); }
`;
