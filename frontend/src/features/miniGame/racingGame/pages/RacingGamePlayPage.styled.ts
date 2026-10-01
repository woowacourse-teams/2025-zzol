import styled from '@emotion/styled';
import skyImage from '@/assets/sky.webp';

export const Container = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
  background-image: url(${skyImage});
  background-size: cover;
  background-position: center;
  background-repeat: repeat-x;
  display: flex;
  flex-direction: column;
  will-change: background-position;
`;

export const ContentWrapper = styled.div`
  width: 100%;

  flex: 1;
  overflow: hidden;
`;

/* 행은 슬롯 번호로 배치한다. flex 순서로 세우면 추월할 때 자리가 즉시 튄다. */
export const PlayersWrapper = styled.div`
  height: 100%;
  position: relative;
`;
