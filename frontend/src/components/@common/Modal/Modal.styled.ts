import { Z_INDEX } from '@/constants/zIndex';
import styled from '@emotion/styled';

export const Backdrop = styled.div`
  position: fixed;
  display: flex;
  width: 100%;
  height: 100%;
  justify-content: center;
  align-items: center;
  background-color: rgba(0, 0, 0, 0.3);
  inset: 0;
  max-width: 430px;
  height: 100dvh;
  margin: 0 auto;
  z-index: ${Z_INDEX.MODAL};
`;

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  width: 100%;
  max-height: 80%;
  margin: 0 24px;
  padding: 16px;
  background-color: ${({ theme }) => theme.color.white};
  border-radius: 12px;
  box-shadow:
    0 3px 6px rgba(0, 0, 0, 0.16),
    0 3px 6px rgba(0, 0, 0, 0.23);
`;

export const ScrollContent = styled.div`
  flex: 1;
  overflow-y: auto;
  min-height: 0;
`;

export const BottomCloseButton = styled.button`
  flex-shrink: 0;
  width: 100%;
  padding: 12px 0;
  margin-top: 12px;
  border: none;
  border-radius: 10px;
  background-color: ${({ theme }) => theme.color.point[400]};
  color: ${({ theme }) => theme.color.white};
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.15s ease;

  &:active {
    opacity: 0.85;
  }
`;

