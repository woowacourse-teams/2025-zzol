import styled from '@emotion/styled';

export type Tone = 'light' | 'dark';

// light: gray-500은 흰 배경 대비 4.8:1로 AA를 넘는 가장 연한 회색이다.
// dark: 빨간 슬라이드에서는 흰색보다 연하면 대비가 3:1 아래로 떨어져 색 대신 굵기를 낮춘다.
export const Pending = styled.span<{ $tone: Tone }>`
  color: ${({ $tone, theme }) => ($tone === 'light' ? theme.color.gray[500] : theme.color.white)};
  font-weight: 500;
`;

export const VisuallyHidden = styled.span`
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
