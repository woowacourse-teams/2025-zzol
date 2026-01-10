import styled from '@emotion/styled';

export const ToggleButton = styled.button`
  position: fixed;
  bottom: 8px;
  right: 12px;
  z-index: 1001;
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

export const HiddenTrigger = styled.div`
  position: fixed;
  top: 0;
  right: 0;
  width: 60px;
  height: 60px;
  z-index: 1001;
  background: transparent;
  touch-action: none;
`;

export const Panel = styled.div<{ height: number }>`
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: ${({ height }) => height}px;
  z-index: 1000;
  background: #ffffff;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  font-family: 'Segoe UI', system-ui, sans-serif;
  font-size: 12px;
`;

export const ResizeHandle = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 5px;
  cursor: ns-resize;
  z-index: 1001;
  background: transparent;
  touch-action: none;

  &:hover {
    background: rgba(0, 0, 0, 0.05);
  }

  @media (hover: none) {
    height: 12px;
  }
`;

export const Header = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  background: #f8f9fa;
  gap: 12px;
`;

export const HeaderButtonWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  flex: 1;
`;

export const Title = styled.h3`
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: #222;
`;

export const HeaderActions = styled.div`
  display: flex;
  gap: 8px;
  align-items: center;
  flex: 1;
`;

export const ClearButton = styled.button`
  appearance: none;
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: #ffffff;
  color: #222;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;

  &:hover {
    background: #f0f0f0;
  }
`;

export const CloseButton = styled.button`
  appearance: none;
  border: none;
  background: transparent;
  color: #666;
  padding: 4px;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;

  &:hover {
    background: rgba(0, 0, 0, 0.05);
  }
`;

export const Content = styled.div<{ $isMobile: boolean }>`
  flex: 1;
  display: flex;
  flex-direction: ${({ $isMobile }) => ($isMobile ? 'column' : 'row')};
  overflow: hidden;
`;

export const RequestListSection = styled.div<{
  detailWidthPercent: number;
  $isMobile: boolean;
  $hasDetail: boolean;
  $topHeightPercent: number;
}>`
  width: ${({ detailWidthPercent, $isMobile }) =>
    $isMobile ? '100%' : `${100 - detailWidthPercent}%`};
  height: ${({ $isMobile, $hasDetail, $topHeightPercent }) =>
    $isMobile && $hasDetail ? `${$topHeightPercent}%` : 'auto'};
  min-width: 0;
  overflow-y: auto;
  position: relative;
`;

export const VerticalResizeHandle = styled.div`
  position: absolute;
  top: 0;
  right: -3px;
  width: 6px;
  height: 100%;
  cursor: ew-resize;
  z-index: 1002;
  background: transparent;

  &:hover {
    background: rgba(0, 0, 0, 0.1);
  }
`;

export const MobileResizeHandle = styled.div`
  width: 100%;
  height: 5px;
  cursor: ns-resize;
  z-index: 1002;
  background: transparent;
  touch-action: none;
  flex-shrink: 0;

  &:hover {
    background: rgba(0, 0, 0, 0.1);
  }

  @media (hover: none) {
    height: 12px;
  }
`;

export const DetailSection = styled.div<{
  widthPercent: number;
  $isMobile: boolean;
  $topHeightPercent: number;
}>`
  width: ${({ widthPercent, $isMobile }) => ($isMobile ? '100%' : `${widthPercent}%`)};
  height: ${({ $isMobile, $topHeightPercent }) =>
    $isMobile ? `${100 - $topHeightPercent}%` : 'auto'};
  min-width: ${({ $isMobile }) => ($isMobile ? '0' : '300px')};
  overflow-y: auto;
  background: #ffffff;
  border-top: ${({ $isMobile }) => ($isMobile ? '1px solid rgba(0, 0, 0, 0.1)' : 'none')};
  flex-shrink: 0;
`;
