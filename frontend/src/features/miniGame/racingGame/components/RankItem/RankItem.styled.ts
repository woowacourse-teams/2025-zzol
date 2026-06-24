import styled from '@emotion/styled';

type Props = {
  $isFixed?: boolean;
};

export const Container = styled.div<Props>`
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  width: fit-content;
  min-width: max-content;
  background: linear-gradient(to right, rgba(19, 8, 8, 0.56) 0%, rgba(46, 35, 35, 0.19) 100%);
  padding: 2px;
  border-radius: 4px;
  overflow: hidden;

  ${({ $isFixed }) =>
    $isFixed &&
    `
    ::before {
      content: '';
      position: absolute;
      top: 0;
      left: -100%;
      width: 100%;
      height: 100%;
      background: linear-gradient(
        90deg,
        transparent,
        rgba(255, 255, 255, 0.6),
        transparent
      );
      animation: shine 800ms ease-in-out;
    }
  `}

  @keyframes shine {
    0% {
      left: -100%;
    }
    100% {
      left: 100%;
    }
  }
`;

export const RankNumber = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1rem;
  height: auto;
`;
