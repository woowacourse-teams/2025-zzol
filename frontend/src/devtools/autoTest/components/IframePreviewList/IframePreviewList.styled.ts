import styled from '@emotion/styled';

export const IframePanel = styled.div`
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px;
  background: #ffffff;
  overflow: auto;
  align-items: flex-start;
  justify-content: center;
  height: 100%;
`;

type IframeWrapperProps = {
  $height?: string;
  $useMinHeight?: boolean;
};

export const IframeWrapper = styled.div<IframeWrapperProps>`
  position: relative;
  width: 320px;
  height: ${(props) => props.$height || '100%'};
  ${(props) => props.$useMinHeight && `height: 680px;`};
  flex-shrink: 0;

  &:hover button[data-delete-button] {
    opacity: 1;
  }
`;

export const PreviewIframe = styled.iframe`
  width: 100%;
  height: 100%;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  background: #fff;
`;

export const IframeLabel = styled.div`
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 1;
  padding: 4px 8px;
  background: rgba(0, 0, 0, 0.7);
  color: #ffffff;
  font-size: 11px;
  font-weight: 500;
  border-radius: 4px;
  pointer-events: none;
  font-family:
    system-ui,
    -apple-system,
    sans-serif;
  max-width: calc(100% - 16px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const DeleteButton = styled.button`
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 2;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.7);
  color: #ffffff;
  font-size: 18px;
  font-weight: 600;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  opacity: 0;
  transition:
    opacity 0.15s ease,
    background 0.15s ease;

  &:hover {
    background: rgba(220, 38, 38, 0.9);
  }

  &:active {
    background: rgba(185, 28, 28, 0.9);
  }
`;

export const AddIframeButton = styled.button`
  width: 100%;
  height: 100%;
  border: 2px dashed rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  background: #f8f8f8;
  color: #666;
  font-size: 48px;
  font-weight: 300;
  cursor: pointer;
  transition: all 0.15s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;

  &:hover {
    background: #e8e8e8;
    border-color: rgba(0, 0, 0, 0.3);
    color: #333;
  }

  &:active {
    background: #d8d8d8;
  }
`;
