import styled from '@emotion/styled';
import { RACING_Z_INDEX } from '../../constants/zIndex';

export const Overlay = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
  z-index: ${RACING_Z_INDEX.OVERLAY};
`;

export const DisconnectBanner = styled.div`
  position: absolute;
  left: 50%;
  bottom: calc(1.5rem + env(safe-area-inset-bottom));
  transform: translateX(-50%);
  width: 90%;
  max-width: 400px;
  padding: 10px 14px;
  border-radius: 10px;
  text-align: center;
  background: ${({ theme }) => theme.color.gray[900]}E0;
  z-index: ${RACING_Z_INDEX.BANNER};
`;

export const DisconnectTitle = styled.p`
  ${({ theme }) => theme.typography.h4}
  color: ${({ theme }) => theme.color.white};
`;

export const DisconnectHint = styled.p`
  ${({ theme }) => theme.typography.caption}
  margin-top: 2px;
  color: ${({ theme }) => theme.color.gray[300]};
`;
