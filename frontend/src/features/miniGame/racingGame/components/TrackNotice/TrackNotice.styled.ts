import styled from '@emotion/styled';
import { RACING_Z_INDEX } from '../../constants/zIndex';

export const Notice = styled.div`
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 80%;
  padding: 12px 16px;
  border-radius: 10px;
  text-align: center;
  background: ${({ theme }) => theme.color.gray[900]}9E;
  z-index: ${RACING_Z_INDEX.PLAYER};
`;

export const Title = styled.p`
  ${({ theme }) => theme.typography.h4}
  color: ${({ theme }) => theme.color.white};
`;

export const Hint = styled.p`
  ${({ theme }) => theme.typography.caption}
  margin-top: 4px;
  color: ${({ theme }) => theme.color.gray[300]};
`;
