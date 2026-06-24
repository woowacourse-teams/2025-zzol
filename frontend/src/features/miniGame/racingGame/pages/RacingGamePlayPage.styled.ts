import styled from '@emotion/styled';
import skyImage from '@/assets/sky.png';

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

export const PlayersWrapper = styled.div`
  height: 100%;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 3rem;
  justify-content: center;
  align-items: center;
`;
