import {
  Area,
  Bar,
  CartesianGrid,
  ComposedChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { ACTIVE_DOT, AXIS_STYLE, GRID_STYLE, TOOLTIP_STYLE } from '@/components/charts/theme';
import { Legend } from '@/components/ui/Legend';
import { formatNumber } from '@/lib/format';

type Series = {
  /** 데이터의 필드 이름. */
  key: string;
  name: string;
  color: string;
  /**
   * 그리는 모양. 기본은 막대다.
   *
   * <p>기준은 <b>계열이 몇 개인가</b>다. 둘을 견줘야 하면(접수와 처리, 성공과 실패) 막대를
   * 나란히 세운다. 같은 날의 두 값이 바닥을 공유해야 높이 차이가 곧 답이 된다. 하나뿐이면
   * 견줄 상대가 없고 읽을 것은 <b>흐름</b>뿐이라 선이 낫다. 막대 서른 개는 그 흐름을
   * 톱니로 만든다.
   */
  shape?: 'bar' | 'line';
};

type DailyChartProps = {
  data: Record<string, unknown>[];
  series: Series[];
  /**
   * 숫자면 그 높이로 고정하고, {@code '100%'} 면 부모를 채운다.
   *
   * <p>{@code '100%'} 는 격자가 카드를 늘려 줄 때만 쓴다. 늘려 주는 형제가 없으면 카드
   * 높이가 내용에서 나오고, 그 내용이 다시 카드 높이를 물어 서로를 기다리다 0이 된다.
   */
  height?: number | string;
};

/**
 * 일자별 흐름.
 *
 * <p>값이 없는 날도 서버가 0으로 채워 보낸다. 화면에서 달력을 만들면 시간대 판단이 서버와
 * 화면 두 곳에 생기고, 빠진 날이 있으면 선이 이웃한 두 점을 이어 그 날을 지워 버린다.
 * 0으로 채워져 있으면 선도 정직하게 바닥을 찍는다.
 *
 * <p>선은 곡선이 아니라 <b>꺾은선</b>이다. 하루치 합계 사이에 중간값이 없는데 곡선으로
 * 이으면 없던 값을 그린 것이 된다. 0에서 2로 튄 날이 완만한 언덕으로 보이는 것도
 * 실제보다 점진적으로 늘어난 것처럼 읽힌다.
 *
 * <p>선은 면으로 채운다. 얇은 선 하나는 30일 폭에서 존재감이 없고, 옅게 깔린 면이 있으면
 * "이 기간에 얼마나 있었나"가 면적으로 읽힌다. 면은 늘 0에서 시작한다. 축을 데이터
 * 최솟값에서 시작하면 작은 차이가 급등락으로 보인다.
 */
export function DailyChart({ data, series, height = 180 }: DailyChartProps) {
  return (
    <div className="flex h-full w-full flex-col gap-2">
      {/* 차트를 두 겹으로 감싼다.
       *
       * ResponsiveContainer 의 {@code height="100%"} 는 부모의 <b>지정된</b> 높이를 찾는다.
       * 부모 높이가 {@code flex-1} 이나 {@code min-height} 에서 나온 것이면 CSS 규칙상
       * 백분율이 auto 로 풀려 0이 되고, 차트가 오류도 빈 상태도 아닌 <b>아무것도 없는
       * 카드</b>로 남는다. 실제로 이 화면에서 그 일이 있었다.
       *
       * 바깥 칸이 남는 높이를 차지하고, 안쪽 칸이 {@code absolute inset-0} 으로 그 높이를
       * 확정된 값으로 바꾼다. 그 위에서는 백분율이 제대로 풀린다. */}
      <div
        className="relative min-h-36 w-full flex-1"
        style={typeof height === 'number' ? { height } : undefined}
      >
        <div className="absolute inset-0">
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={data} margin={{ top: 4, right: 4, bottom: 0, left: 0 }} barGap={2}>
              <defs>
                {series
                  .filter((line) => line.shape === 'line')
                  .map((line) => (
                    <linearGradient
                      key={line.key}
                      id={`fill-${line.key}`}
                      x1="0"
                      y1="0"
                      x2="0"
                      y2="1"
                    >
                      <stop offset="0%" stopColor={line.color} stopOpacity={0.22} />
                      <stop offset="100%" stopColor={line.color} stopOpacity={0.02} />
                    </linearGradient>
                  ))}
              </defs>
              <CartesianGrid {...GRID_STYLE} />
              <XAxis
                dataKey="date"
                tickFormatter={(value: string) => value.slice(5).replace('-', '/')}
                {...AXIS_STYLE}
                minTickGap={16}
              />
              <YAxis {...AXIS_STYLE} width={32} allowDecimals={false} tickCount={4} />
              <Tooltip
                cursor={{ fill: 'var(--chart-grid)', fillOpacity: 0.55 }}
                contentStyle={TOOLTIP_STYLE}
                formatter={(value, name) => [formatNumber(Number(value ?? 0)), String(name ?? '')]}
              />
              {series.map((line) =>
                line.shape === 'line' ? (
                  <Area
                    key={line.key}
                    type="linear"
                    dataKey={line.key}
                    name={line.name}
                    stroke={line.color}
                    strokeWidth={2}
                    fill={`url(#fill-${line.key})`}
                    dot={false}
                    activeDot={ACTIVE_DOT}
                    isAnimationActive={false}
                  />
                ) : (
                  <Bar
                    key={line.key}
                    dataKey={line.key}
                    name={line.name}
                    fill={line.color}
                    maxBarSize={14}
                    radius={[3, 3, 0, 0]}
                    isAnimationActive={false}
                  />
                ),
              )}
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* 계열이 하나면 범례를 숨긴다. 카드 제목이 이미 그 이름이다. */}
      {series.length > 1 && (
        <Legend
          items={series.map((line) => ({
            label: line.name,
            color: line.color,
            shape: line.shape === 'line' ? ('line' as const) : ('bar' as const),
          }))}
        />
      )}
    </div>
  );
}
