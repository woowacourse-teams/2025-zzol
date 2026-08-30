import styled from '@emotion/styled';

export const Container = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
`;

export const Canvas = styled.canvas`
  display: block;
  /* 전역 manipulation 오버라이드 — 조향 포인터 입력이 스크롤·줌으로 새지 않게 */
  touch-action: none;
`;
