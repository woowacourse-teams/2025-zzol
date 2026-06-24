import styled from '@emotion/styled';
import { RACING_Z_INDEX } from '../../constants/zIndex';

const TRANSITION_DURATION_MS = 100;

type Props = {
  $isMe: boolean;
  $position: number;
  $myPosition: number;
};

export const Container = styled.div<Props>`
  transform: ${({ $isMe, $position, $myPosition }) => {
    if ($isMe) return 'translateX(0)';
    const relativeX = $position - $myPosition;
    return `translateX(${relativeX}px)`;
  }};
  transition: transform ${TRANSITION_DURATION_MS}ms linear;
  z-index: ${RACING_Z_INDEX.PLAYER};
`;

export const RotatingWrapper = styled.div`
  will-change: transform;
`;

export const PlayerName = styled.div`
  position: absolute;
  top: -1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  width: 100%;
`;
