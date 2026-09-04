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
  background: ${({ theme }) => theme.color.gray[950]}C7;
  z-index: ${RACING_Z_INDEX.FINISH};
`;

export const Title = styled.p`
  ${({ theme }) => theme.typography.caption}
  letter-spacing: 0.22em;
  color: ${({ theme }) => theme.color.white}9E;
`;

/** 시안의 체커 플래그 띠. 결승 이미지와 같은 무늬를 CSS 로 낸다. */
export const CheckerRule = styled.div`
  width: 60px;
  height: 3px;
  margin-top: 16px;
  background-image:
    linear-gradient(
      45deg,
      ${({ theme }) => theme.color.gray[100]} 25%,
      transparent 25%,
      transparent 75%,
      ${({ theme }) => theme.color.gray[100]} 75%
    ),
    linear-gradient(
      45deg,
      ${({ theme }) => theme.color.gray[100]} 25%,
      transparent 25%,
      transparent 75%,
      ${({ theme }) => theme.color.gray[100]} 75%
    );
  background-size: 12px 12px;
  background-position:
    0 0,
    6px 6px;
`;
