import styled from '@emotion/styled';
import { WHEEL_CONFIG } from '@/features/roulette/constants/config';

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
  /* 핀이 휠 위로 튀어나오는 만큼 비운다 — 안 비우면 제목과 겹친다 */
  padding-top: ${WHEEL_CONFIG.PIN_OVERHANG}px;
  container-type: size;
`;

/** 남은 공간에 들어가는 가장 큰 정사각형 */
export const RouletteWheelWrapper = styled.div`
  /*
   * 컨테이너 쿼리 단위를 모르는 브라우저는 아래 줄을 통째로 버린다. 폭 선언이 하나도 없으면
   * 자식이 전부 absolute 라 내용 폭이 0 이 되고, aspect-ratio 가 높이까지 0 으로 만들어
   * 휠이 사라진다. 폴백을 먼저 둬서 그 경우 대기방과 같은 폭 기준으로 떨어지게 한다.
   */
  width: 100%;
  width: min(100cqw, 100cqh);
  aspect-ratio: 1;
`;
