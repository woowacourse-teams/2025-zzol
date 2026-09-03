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
  /* 오른쪽 끝이 0.19까지 떨어져 이름 자리에 하늘이 그대로 비쳤다. */
  background: linear-gradient(to right, rgba(0, 0, 0, 0.56) 0%, rgba(0, 0, 0, 0.45) 100%);
  padding: 2px;
  border-radius: 4px;
  overflow: hidden;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.8);

  ${({ $isFixed }) =>
    $isFixed &&
    `
    /* & 가 없으면 stylis 가 자손 선택자로 컴파일해 반짝임이 숫자 칸과 이름 안쪽에서 따로 뜬다. */
    &::before {
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

  @media (prefers-reduced-motion: reduce) {
    &::before {
      animation: none;
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
