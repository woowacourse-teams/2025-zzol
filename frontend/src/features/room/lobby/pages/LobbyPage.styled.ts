import styled from '@emotion/styled';

export const Container = styled.div`
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
  padding-bottom: 4rem;
`;

export const Wrapper = styled.div`
  position: sticky;
  bottom: 1rem;
  margin: 0 2rem;
  z-index: 1;
`;
