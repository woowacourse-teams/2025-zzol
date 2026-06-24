import styled from '@emotion/styled';
import { RACING_Z_INDEX } from '../../constants/zIndex';

type Props = {
  $progress: number;
  $color: string;
  $isMe: boolean;
};

const FILL_TRANSITION_DURATION = 100;

export const Container = styled.div`
  width: 100%;
  position: relative;
  padding: 1.8rem 1rem 0 1rem;
`;

export const ProgressTrack = styled.div`
  position: relative;
  width: 100%;
  height: 15px;
  background-color: rgba(255, 255, 255, 0.3);
  border-radius: 10px;
  overflow: visible;
`;

export const ProgressFill = styled.div<Props>`
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  width: ${({ $progress }) => $progress}%;
  background-color: ${({ $color }) => $color};
  opacity: ${({ $isMe }) => ($isMe ? 1 : 0.6)};
  border-radius: ${({ $progress }) => ($progress >= 99 ? '10px' : '10px 0 0 10px')};
  transition:
    width ${FILL_TRANSITION_DURATION}ms ease-out,
    border-radius ${FILL_TRANSITION_DURATION}ms ease-out;
  z-index: ${({ $isMe }) => ($isMe ? RACING_Z_INDEX.PROGRESS_BAR_ME : RACING_Z_INDEX.PROGRESS_BAR)};
`;

export const ProgressMarker = styled.div<Props>`
  position: absolute;
  left: ${({ $progress }) => $progress}%;
  top: -20px;
  transform: translateX(-50%);
  width: ${({ $isMe }) => ($isMe ? '20px' : '16px')};
  height: ${({ $isMe }) => ($isMe ? '20px' : '16px')};
  background-color: ${({ $color }) => $color};
  border: 2px solid ${({ $isMe }) => ($isMe ? '#fff' : 'transparent')};
  border-radius: 50%;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  transition: left ${FILL_TRANSITION_DURATION}ms ease-out;
  z-index: ${({ $isMe }) =>
    $isMe ? RACING_Z_INDEX.PROGRESS_MARKER_ME : RACING_Z_INDEX.PROGRESS_MARKER};

  &::after {
    content: '';
    position: absolute;
    left: 50%;
    bottom: ${({ $isMe }) => ($isMe ? '-10px' : '-8px')};
    transform: translateX(-50%);
    width: 2px;
    height: ${({ $isMe }) => ($isMe ? '10px' : '8px')};
    background-color: ${({ $color }) => $color};
  }
`;
