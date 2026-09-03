import styled from '@emotion/styled';
import { RACING_Z_INDEX } from '../../constants/zIndex';

const TRANSITION_DURATION_MS = 100;

export const Ground = styled.div`
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 44px;
  padding-bottom: env(safe-area-inset-bottom);
  overflow: hidden;
  border-top: 1px solid ${({ theme }) => theme.color.white}47;
  /* 반복 타일. 눈금 사이가 비어 보이지 않게 바닥에 결이 있다는 것만 알린다. */
  background: repeating-linear-gradient(
    90deg,
    ${({ theme }) => theme.color.white}14 0 2px,
    transparent 2px 12px
  );
  z-index: ${RACING_Z_INDEX.LINE};
`;

export const Tick = styled.div`
  position: absolute;
  left: 50%;
  top: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  transition: transform ${TRANSITION_DURATION_MS}ms linear;
`;

export const TickMark = styled.span`
  width: 2px;
  height: 10px;
  background: ${({ theme }) => theme.color.white}8C;
`;

export const TickLabel = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.white}BF;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.8);
`;
