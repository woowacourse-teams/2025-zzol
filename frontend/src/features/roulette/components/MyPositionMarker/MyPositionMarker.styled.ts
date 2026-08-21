import styled from '@emotion/styled';

type MarkerProps = { $isHidden: boolean };

/**
 * 돌아가는 동안에는 감춘다. 그때부터는 당첨 핀이 주인공이고,
 * 빠르게 도는 화면에서 표시가 둘이면 서로 방해한다.
 */
export const Marker = styled.g<MarkerProps>`
  opacity: ${({ $isHidden }) => ($isHidden ? 0 : 1)};
  transition: opacity 0.25s ease;
  /* 원판 위에 떠 있는 느낌을 준다 */
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.28));
`;
