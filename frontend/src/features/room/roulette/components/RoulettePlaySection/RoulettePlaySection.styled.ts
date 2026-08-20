import styled from '@emotion/styled';

export const Container = styled.section`
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  gap: 1rem;
`;

/**
 * 휠이 쓸 수 있는 공간. 제목과 리스트가 자리를 잡고 남은 만큼을 받는다.
 * 크기를 재는 기준(container)이 되어야 휠이 고정 px 없이 여기 맞춰 줄고 늘 수 있다.
 */
export const RouletteWheelArea = styled.div`
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 핀이 휠 위로 24px 튀어나온다 — 그만큼 비워야 제목과 겹치지 않는다 */
  padding-top: 24px;
  container-type: size;
`;

/** 남은 공간에 들어가는 가장 큰 정사각형 */
export const RouletteWheelWrapper = styled.div`
  width: min(100cqw, 100cqh);
  aspect-ratio: 1;
`;
