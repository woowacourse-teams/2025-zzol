import styled from '@emotion/styled';
import { RACING_Z_INDEX } from '../../constants/zIndex';

export const Notice = styled.div`
  position: absolute;
  top: 50%;
  left: 26px;
  right: 26px;
  transform: translateY(-50%);
  padding: 22px 20px;
  border: 1px solid ${({ theme }) => theme.color.white}33;
  border-radius: 8px;
  text-align: center;
  background: ${({ theme }) => theme.color.gray[950]}C7;
  z-index: ${RACING_Z_INDEX.PLAYER};
`;

export const Title = styled.p`
  ${({ theme }) => theme.typography.h4}
  margin-bottom: 8px;
  font-weight: ${({ theme }) => theme.typography.h1.fontWeight};
  color: ${({ theme }) => theme.color.white};
`;

export const Body = styled.p`
  ${({ theme }) => theme.typography.caption}
  line-height: 1.75;
  color: ${({ theme }) => theme.color.white}B8;
`;
