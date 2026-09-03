import styled from '@emotion/styled';
import { RACING_Z_INDEX } from '../../constants/zIndex';

export const Container = styled.div`
  position: absolute;
  z-index: ${RACING_Z_INDEX.RANK};
  /* 진행바 아래에 둔다. 시안이 순위표를 바 아래로 내려 마커와 겹치던 자리를 피한다.
     바는 위 여백 1.8rem + 라벨 줄 + 마커 줄 + 트랙으로 약 103px 이고, 시안의 바와
     순위표 사이 간격이 25px 이라 8rem 에 세운다. */
  top: calc(8rem + env(safe-area-inset-top));
  left: 1rem;
`;

export const RankList = styled.div`
  display: flex;
  flex-direction: column;
`;
