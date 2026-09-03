import styled from '@emotion/styled';
import { RACING_Z_INDEX } from '../../constants/zIndex';

export const Container = styled.div`
  position: absolute;
  z-index: ${RACING_Z_INDEX.RANK};
  /* 진행바에 출발·결승 라벨이 붙어 아래로 늘어났다. 마커와 겹치지 않게 내린다. */
  top: calc(5.5rem + env(safe-area-inset-top));
  left: 1rem;
`;

export const RankList = styled.div`
  display: flex;
  flex-direction: column;
`;
