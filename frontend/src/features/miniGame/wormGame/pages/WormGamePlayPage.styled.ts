import styled from '@emotion/styled';

export const Container = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
  background: ${({ theme }) => theme.color.gray[900]};
  overflow: hidden;
  user-select: none;
  -webkit-user-select: none;
`;

export const SpectateBar = styled.button`
  position: absolute;
  left: 50%;
  bottom: calc(16px + env(safe-area-inset-bottom));
  transform: translateX(-50%);
  min-height: 44px;
  padding: 10px 18px;
  border: none;
  border-radius: 999px;
  background: ${({ theme }) => theme.color.gray[950]}B3;
  color: ${({ theme }) => theme.color.white};
  ${({ theme }) => theme.typography.h4}
  white-space: nowrap;
  cursor: pointer;

  &:active {
    opacity: 0.85;
  }
`;

export const FinishBadge = styled.div`
  position: absolute;
  top: calc(16px + env(safe-area-inset-top));
  left: 50%;
  transform: translateX(-50%);
  padding: 8px 16px;
  border-radius: 999px;
  background: ${({ theme }) => theme.color.point[400]};
  color: ${({ theme }) => theme.color.white};
  ${({ theme }) => theme.typography.h4}
  white-space: nowrap;
`;
