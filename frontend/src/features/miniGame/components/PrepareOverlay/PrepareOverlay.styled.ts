import { Z_INDEX } from '@/constants/zIndex';
import styled from '@emotion/styled';

export const Backdrop = styled.div`
  position: absolute;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: ${Z_INDEX.BACKDROP};
`;

export const Content = styled.div`
  display: flex;
  flex-direction: column;
  gap: 3rem;
`;

export const BubbleTextWrapper = styled.div`
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
`;

export const BubbleImage = styled.img`
  position: absolute;
  width: 200px;
  top: -40px;
`;

export const CoffeeImage = styled.img`
  width: 150px;
  height: 169px;
`;
