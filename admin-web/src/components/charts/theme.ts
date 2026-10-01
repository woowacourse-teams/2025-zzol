import type { CSSProperties } from 'react';

/**
 * 차트 공통 규격.
 *
 * <p>recharts 를 import 하지 않는다. 순수한 값만 두어야 이 파일을 어디서 가져가도
 * 차트 청크가 딸려 오지 않는다.
 *
 * <p>세 차트에 같은 스타일 객체를 세 번 적어 두었더니 한 곳만 고쳐진 적이 있다.
 * 툴팁 모서리가 차트마다 다른 것은 아무도 버그로 신고하지 않지만, 화면 전체를
 * 조금씩 성기게 만든다.
 */

/** 툴팁 상자. 떠 있는 것이라 카드보다 진한 그림자를 쓴다. */
export const TOOLTIP_STYLE: CSSProperties = {
  borderRadius: 'var(--radius-md)',
  border: '1px solid var(--border)',
  fontSize: '12px',
  boxShadow: 'var(--shadow-popover)',
};

/**
 * 축. 눈금선과 축선을 지운다.
 *
 * <p>읽어야 하는 것은 데이터의 모양이지 눈금이 아니다. 축을 상자처럼 두르면 그 선들이
 * 데이터와 같은 굵기로 눈에 들어온다.
 */
export const AXIS_STYLE = {
  tick: { fontSize: 11, fill: 'var(--chart-axis)' },
  tickLine: false,
  axisLine: false,
} as const;

/** 격자. 가로선만 남긴다. 세로선은 값을 읽는 데 쓰이지 않으면서 칸을 잘게 쪼갠다. */
export const GRID_STYLE = {
  stroke: 'var(--chart-grid)',
  vertical: false,
} as const;

/**
 * 선 위의 활성 점. 막대나 다른 선 위를 지나므로 표면 색 테두리를 둘러
 * 배경에 먹히지 않게 한다.
 */
export const ACTIVE_DOT = { r: 4, strokeWidth: 2, stroke: 'var(--bg-surface)' } as const;
