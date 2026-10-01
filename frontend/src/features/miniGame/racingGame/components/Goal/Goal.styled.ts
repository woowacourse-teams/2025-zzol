import styled from '@emotion/styled';
import { RACING_Z_INDEX } from '../../constants/zIndex';

export const Container = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: ${RACING_Z_INDEX.GOAL};
`;

/** 밝은 하늘 위의 흰 글자라 판 없이는 야외에서 안 읽힌다. */
export const Plate = styled.div`
  padding: 12px 28px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.55);
`;
