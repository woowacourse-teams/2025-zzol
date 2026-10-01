type SparklineProps = {
  values: number[];
  /** 선과 면적의 색. 기본은 잉크에 가까운 회색이라 옆의 숫자를 이기지 않는다. */
  color?: string;
  width?: number;
  height?: number;
  className?: string;
};

/**
 * 값 하나 옆에 붙는 최근 흐름. 눈금도 축도 없다.
 *
 * <p><b>recharts 를 쓰지 않는다.</b> 이 그림은 지연 로드할 수 없는 자리(지표 줄 옆)에
 * 들어가는데, recharts 를 정적으로 가져오면 gzip 108KB 가 메인 청크로 딸려 와서 큰 차트를
 * 지연 로드한 의미가 사라진다. 선 하나를 그리는 데 필요한 것은 {@code polyline} 하나다.
 *
 * <p>축을 뺀 것은 이 그림이 <b>값을 읽는 용도가 아니라 모양을 읽는 용도</b>이기 때문이다.
 * 정확한 값은 바로 옆 숫자가 말한다. 여기서 답하는 질문은 "오늘 12건이 평소만큼인가"
 * 하나이고, 그 답은 선의 오르내림으로 충분하다.
 *
 * <p>세로 범위는 <b>이 계열의 최소~최대</b>로 잡는다. 0부터 그리면 값이 늘 비슷한 계열은
 * 일직선이 되어 아무 정보도 주지 못한다. 대신 높이 비교는 계열 사이에서 성립하지 않으므로
 * 스파크라인끼리 크기를 견주지 않는다.
 */
export function Sparkline({
  values,
  color = 'var(--chart-axis)',
  width = 72,
  height = 24,
  className,
}: SparklineProps) {
  // 점이 하나면 선이 그려지지 않는다. 빈 자리를 남겨 줄 높이는 유지한다.
  if (values.length < 2) {
    return <span className={className} style={{ display: 'inline-block', width, height }} />;
  }

  const min = Math.min(...values);
  const max = Math.max(...values);
  const span = max - min;

  // 위아래로 1px 씩 남긴다. 최댓값이 꼭짓점에 닿으면 선 굵기 절반이 잘린다.
  const top = 1;
  const bottom = height - 1;
  const scaleY = (value: number) =>
    span === 0 ? (top + bottom) / 2 : bottom - ((value - min) / span) * (bottom - top);
  // 오른쪽 끝에 점을 찍으므로 그 반지름만큼 안으로 들인다. 안 그러면 점 절반이 잘린다.
  const right = width - 3;
  const scaleX = (index: number) => (index / (values.length - 1)) * right;

  const points = values.map((value, index) => `${scaleX(index)},${scaleY(value)}`).join(' ');
  const last = values[values.length - 1] ?? 0;

  return (
    <svg
      className={className}
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      // 장식이다. 같은 수치를 옆의 숫자가 이미 읽어 준다.
      aria-hidden
      focusable="false"
    >
      <polyline
        points={points}
        fill="none"
        stroke={color}
        strokeWidth={1.5}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      {/* 마지막 점만 찍는다. 어느 쪽이 오늘인지가 축 없이도 분명해진다. */}
      <circle cx={right} cy={scaleY(last)} r={2} fill={color} />
    </svg>
  );
}
