import styled from '@emotion/styled';
import { RACING_Z_INDEX } from '../../constants/zIndex';

export const Container = styled.div`
  position: absolute;
  z-index: ${RACING_Z_INDEX.RANK};
  /* 진행바 아래에 둔다. 바는 위 여백 1.8rem(28.8px) + 마커 줄 24px + 아래 여백 8px +
     트랙 15px 으로 75.8px 이고, 시안의 바와 순위표 사이 간격이 25px 이라 6.25rem 에 세운다. */
  top: calc(6.25rem + env(safe-area-inset-top));
  left: 1rem;
`;

export const RankList = styled.div`
  display: flex;
  flex-direction: column;
`;
