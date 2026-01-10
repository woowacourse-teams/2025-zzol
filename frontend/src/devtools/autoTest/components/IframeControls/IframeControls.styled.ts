import styled from '@emotion/styled';

export const ToggleBar = styled.div`
  position: fixed;
  top: 8px;
  right: 12px;
  z-index: 1001;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  padding: 0;
  background: transparent;
`;

export const ToggleButton = styled.button`
  appearance: none;
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: #ffffff;
  color: #222;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s ease;

  &:hover {
    background: #f6f6f6;
  }
`;

export const PlayButton = styled.button`
  appearance: none;
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: #4caf50;
  color: #ffffff;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s ease;

  &:hover:not(:disabled) {
    background: #45a049;
  }

  &:disabled {
    background: #cccccc;
    cursor: not-allowed;
    opacity: 0.6;
  }
`;

export const PauseButton = styled.button`
  appearance: none;
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: #ff9800;
  color: #ffffff;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s ease;

  &:hover {
    background: #f57c00;
  }

  &:active {
    background: #e65100;
  }
`;

export const ResumeButton = styled.button`
  appearance: none;
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: #2196f3;
  color: #ffffff;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s ease;

  &:hover {
    background: #1976d2;
  }

  &:active {
    background: #1565c0;
  }
`;

export const StopButton = styled.button`
  appearance: none;
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: #f44336;
  color: #ffffff;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s ease;

  &:hover {
    background: #d32f2f;
  }

  &:active {
    background: #b71c1c;
  }
`;

type GameSelectionContainerProps = {
  $isExpanded: boolean;
};

export const GameSelectionContainer = styled.div<GameSelectionContainerProps>`
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 8px;
  ${({ $isExpanded }) => ($isExpanded ? 'min-width: 150px;' : 'width: fit-content;')}
`;

export const GameSelectionLabel = styled.button`
  appearance: none;
  border: none;
  background: transparent;
  font-size: 12px;
  font-weight: 500;
  color: #666;
  margin-bottom: 0;
  padding: 0;
  cursor: pointer;
  text-align: left;
  transition: color 0.15s ease;
  line-height: 1.2;

  &:hover {
    color: #222;
  }

  &:active {
    color: #000;
  }
`;

export const GameSelectionButtons = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
  animation: slideDown 0.2s ease-out;
  overflow: hidden;

  @keyframes slideDown {
    from {
      opacity: 0;
      max-height: 0;
    }
    to {
      opacity: 1;
      max-height: 500px;
    }
  }
`;

type GameSelectionButtonProps = {
  $selected: boolean;
};

export const GameSelectionButton = styled.button<GameSelectionButtonProps>`
  appearance: none;
  border: 1px solid ${(props) => (props.$selected ? '#4caf50' : 'rgba(0, 0, 0, 0.12)')};
  background: ${(props) => (props.$selected ? '#4caf50' : '#ffffff')};
  color: ${(props) => (props.$selected ? '#ffffff' : '#222')};
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: left;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  width: 100%;
  min-width: 120px;
  box-sizing: border-box;

  &:hover:not(:disabled) {
    background: ${(props) => (props.$selected ? '#45a049' : '#f6f6f6')};
    border-color: ${(props) => (props.$selected ? '#45a049' : 'rgba(0, 0, 0, 0.2)')};
  }

  &:active:not(:disabled) {
    background: ${(props) => (props.$selected ? '#3d8b40' : '#e8e8e8')};
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.5;
  }
`;

type GameOrderBadgeProps = {
  $visible: boolean;
};

export const GameOrderBadge = styled.span<GameOrderBadgeProps>`
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  width: 20px;
  height: 20px;
  background: ${(props) => (props.$visible ? 'rgba(255, 255, 255, 0.3)' : 'transparent')};
  color: ${(props) => (props.$visible ? '#ffffff' : 'transparent')};
  font-size: 11px;
  font-weight: 600;
  border-radius: 50%;
  padding: 0;
  margin-left: auto;
  flex-shrink: 0;
`;
