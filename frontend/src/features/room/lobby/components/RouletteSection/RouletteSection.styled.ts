import styled from '@emotion/styled';

export const Wrapper = styled.section`
  display: flex;
  flex-direction: column;
  align-items: center;
`;

/** 제목 줄 오른쪽 끝으로 민다 — SectionTitle 의 suffix 는 제목 바로 뒤에 붙는다 */
export const ToggleWrapper = styled.div`
  margin-left: auto;
`;

/**
 * 대기방 섹션은 세로로 스크롤되는 블록이라 남은 높이가 정해져 있지 않다.
 * 그래서 휠은 높이가 아니라 폭에 맞춘다 — 넘치면 잘리지 않고 스크롤된다.
 */
export const RouletteWheelArea = styled.div`
  display: flex;
  justify-content: center;
  /* 핀이 휠 위로 24px 튀어나온다 — 그만큼 비워야 제목과 겹치지 않는다 */
  padding-top: 24px;
`;

export const RouletteWheelWrapper = styled.div`
  width: 100%;
  aspect-ratio: 1;
`;
