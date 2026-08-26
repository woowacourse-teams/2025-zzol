import styled from '@emotion/styled';
import { css } from '@emotion/react';

/** 모바일 터치 타깃 최소치(WCAG 2.5.5 / iOS HIG 44pt) */
const MIN_TOUCH_TARGET_PX = 44;
const EDGE_GAP_PX = 16;

export const Container = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
  background: ${({ theme }) => theme.color.gray[900]};
  overflow: hidden;
  user-select: none;
  -webkit-user-select: none;
`;

const pill = css`
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  border-radius: 999px;
  white-space: nowrap;
`;

export const SpectateBar = styled.button`
  ${pill}
  bottom: calc(${EDGE_GAP_PX}px + env(safe-area-inset-bottom));
  min-height: ${MIN_TOUCH_TARGET_PX}px;
  padding: 10px 18px;
  border: none;
  background: ${({ theme }) => theme.color.gray[950]}B3;
  color: ${({ theme }) => theme.color.white};
  ${({ theme }) => theme.typography.h4}
  cursor: pointer;

  &:active {
    opacity: 0.85;
  }
`;

export const FinishBadge = styled.div`
  ${pill}
  top: calc(${EDGE_GAP_PX}px + env(safe-area-inset-top));
  padding: 8px 16px;
  background: ${({ theme }) => theme.color.point[400]};
  color: ${({ theme }) => theme.color.white};
  ${({ theme }) => theme.typography.h4}
`;
