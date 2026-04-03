import styled from '@emotion/styled';

export const Wrapper = styled.div`
  display: flex;
  gap: 12px;
  width: 100%;
  height: 100%;
  max-width: 360px; /* Fixed width for better game balance */
  margin: 0 auto;
  user-select: none;
  touch-action: none;
`;

export const GameContainer = styled.div`
  position: relative;
  flex: 1;
  height: 100%;
  background: #222;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  cursor: pointer;
`;

export const Canvas = styled.canvas`
  display: block;
  width: 100%;
  height: 100%;
`;

export const TimerContainer = styled.div`
  width: 12px;
  height: 100%;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  overflow: hidden;
  position: relative;
  backdrop-filter: blur(4px);
`;

export const TimerFill = styled.div<{ timeLeft: number; totalTime: number }>`
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: ${({ timeLeft, totalTime }) => (timeLeft / totalTime) * 100}%;
  background: ${({ timeLeft }) =>
    timeLeft < 5
      ? 'linear-gradient(to top, #ff4d4d, #ff9f43)'
      : 'linear-gradient(to top, #48dbfb, #1dd1a1)'};
  transition:
    height 1s linear,
    background 0.3s ease;
  box-shadow: 0 0 8px ${({ timeLeft }) => (timeLeft < 5 ? 'rgba(255, 77, 77, 0.5)' : 'rgba(72, 219, 251, 0.3)')};
`;

export const MuteButton = styled.button`
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.4);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 11px;
  font-family: 'Pretendard Variable', Pretendard, sans-serif;
  cursor: pointer;
  z-index: 10;
  backdrop-filter: blur(8px);
  transition: all 0.2s ease;

  &:hover {
    background: rgba(0, 0, 0, 0.6);
    border-color: rgba(255, 255, 255, 0.3);
  }

  &:active {
    transform: scale(0.95);
  }
`;
