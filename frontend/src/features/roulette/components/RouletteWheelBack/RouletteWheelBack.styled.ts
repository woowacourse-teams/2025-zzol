import styled from '@emotion/styled';
import { WHEEL_CONFIG } from '../../constants/config';

export const Container = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
`;

export const Wrapper = styled.div`
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background-color: ${({ theme }) => theme.color.point[100]};
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;

  svg {
    width: 100%;
    height: 100%;
  }
`;

export const Pin = styled.div`
  width: 0;
  height: 0;
  border-left: 11px solid transparent;
  border-right: 11px solid transparent;
  border-top: 26px solid ${({ theme }) => theme.color.gray[500]};
  border-radius: 4px;
  position: absolute;
  top: -${WHEEL_CONFIG.PIN_OVERHANG}px;
  left: 50%;
  transform: translateX(-50%);
  z-index: -1;
`;

export const BreadCharacter = styled.img`
  width: 66%;
  height: auto;
  position: absolute;
  top: 50%;
  left: 52%;
  transform: translate(-50%, -50%);
`;
