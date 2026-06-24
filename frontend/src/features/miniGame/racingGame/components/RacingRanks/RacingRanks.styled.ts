import styled from '@emotion/styled';
import { RACING_Z_INDEX } from '../../constants/zIndex';

export const Container = styled.div`
  position: absolute;
  z-index: ${RACING_Z_INDEX.RANK};
  top: 4rem;
  left: 1rem;
`;

export const RankList = styled.div`
  display: flex;
  flex-direction: column;
`;
