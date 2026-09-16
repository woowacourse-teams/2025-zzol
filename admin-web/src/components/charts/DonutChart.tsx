import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import type { Bucket } from '@/api/types';
import { TOOLTIP_STYLE } from '@/components/charts/theme';
import { EmptyState } from '@/components/ui/EmptyState';
import { Swatch } from '@/components/ui/Legend';
import { cn } from '@/lib/cn';
import { formatNumber, formatPercent } from '@/lib/format';

/**
 * 조각 색. 값이 큰 순서로 가져다 쓴다.
 *
 * <p>여기까지 오는 데 네 번 갈아엎었다. 브랜드 색(카카오 노랑, 네이버 초록)은 백오피스에서
 * 이 카드 하나만 알록달록하게 만들었고, 회색 농담만 쓰니 이 카드만 칙칙하게 가라앉았고,
 * 로고색 농담만 쓰니 값이 비슷한 두 조각이 같은 색으로 보였다.
 *
 * <p>답은 <b>둘을 섞는 것</b>이었다. 가장 큰 조각이 로고색이고 나머지는 회색이다. 화면의
 * 다른 카드들이 쓰는 색 그대로라 톤이 깨지지 않으면서, 로고색과 회색은 색조 자체가 달라
 * 농담 두 단계보다 훨씬 멀리 떨어진다. 회색끼리도 400과 200으로 두 단계를 건너뛴다.
 *
 * <p>넷을 넘기지 않는다. 다섯째부터는 회색이 서로 붙는다. 조각이 더 많아지면 색을 늘리는
 * 것이 아니라 순위 막대로 그린다.
 */
const SLICE_COLORS = ['var(--accent)', 'var(--gray-400)', 'var(--gray-200)', 'var(--gray-600)'];

/** 도넛에 올릴 수 있는 조각 수. 이보다 많으면 순위 막대를 쓴다. */
export const MAX_SLICES = SLICE_COLORS.length;

type DonutChartProps = {
  slices: Bucket[];
  /** 가운데에 적을 말. 총합 아래에 붙는다. */
  centerLabel: string;
  /**
   * 좌우로 놓을지 위아래로 쌓을지.
   *
   * <p>카드가 본문 폭 전체를 쓰면 좌우가 맞다. 3분의 1 칸에 들어가면 범례 한 칸이 100px 이
   * 되어 제공자 이름이 통째로 잘린다. 좁고 높은 자리에서는 쌓는 편이 폭도 높이도 맞는다.
   */
  layout?: 'row' | 'column';
  size?: number;
};

/**
 * 부분이 모여 전체를 이루는 구성비.
 *
 * <h2>언제 도넛이고 언제 막대인가</h2>
 *
 * <p>도넛은 <b>합이 하나의 전체</b>이고 항목이 넷 이하이며 순서가 없을 때만 쓴다. 소셜
 * 제공자 분포가 그렇다. 원 하나가 곧 "전체 연결"이라 조각 크기가 바로 비율로 읽힌다.
 *
 * <p>순서가 있는 수치 구간(1명, 2~3명, 4~5명…)에는 쓰지 않는다. 원은 시작도 끝도 없어
 * 구간의 순서를 표현하지 못한다. 그건 히스토그램이 맡는다. 항목이 다섯을 넘는 목록도
 * 아니다. 조각을 구분할 색이 모자라고, 작은 조각끼리는 눈으로 비교가 안 된다.
 *
 * <p>가운데를 비워 총합을 적는다. 꽉 찬 원이면 그 자리가 놀고, 총합을 옆에 따로 적으면
 * 이 그림과 무관한 숫자처럼 보인다.
 *
 * <p>어느 조각이 무엇인지는 <b>색이 혼자 지지 않는다.</b> 옆의 범례에 이름과 값과 비율이
 * 나란히 적혀 있고 툴팁도 이름을 읽어 준다. 색만으로 구분하게 두면 흑백 인쇄와 색각
 * 이상에서 그림이 무의미해진다.
 */
export function DonutChart({ slices, centerLabel, layout = 'row', size = 148 }: DonutChartProps) {
  const total = slices.reduce((sum, slice) => sum + slice.count, 0);

  if (total === 0) {
    return <EmptyState title={`${centerLabel} 기록이 없습니다`} />;
  }

  // 값이 0인 조각은 그리지 않는다. 그려도 보이지 않으면서 범례에 줄만 늘린다.
  const visible = slices.filter((slice) => slice.count > 0);
  const rankOf = new Map(
    [...visible]
      .sort((a, b) => b.count - a.count)
      .map((slice, rank) => [slice.label, rank] as const),
  );
  const colorOf = (label: string) =>
    SLICE_COLORS[Math.min(rankOf.get(label) ?? 0, SLICE_COLORS.length - 1)] ?? SLICE_COLORS[0]!;

  return (
    // w-full 이 없으면 카드가 넓어도 그림이 내용 폭만 쓰고 오른쪽이 통째로 빈다.
    <div
      className={cn(
        'flex w-full gap-5',
        layout === 'column' ? 'flex-col items-center' : 'items-center',
      )}
    >
      <div className="relative shrink-0" style={{ width: size, height: size }}>
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={visible}
              dataKey="count"
              nameKey="label"
              innerRadius="62%"
              outerRadius="92%"
              // 조각 사이를 벌리고 표면 색 테두리를 둘러 옅은 조각이 흰 바탕에 녹지 않게 한다.
              paddingAngle={2}
              stroke="var(--border)"
              strokeWidth={1}
              // 12시에서 시계 방향. 기본값은 3시에서 반시계라 큰 조각이 아래에서 시작한다.
              startAngle={90}
              endAngle={-270}
              isAnimationActive={false}
            >
              {visible.map((slice) => (
                <Cell key={slice.label} fill={colorOf(slice.label)} />
              ))}
            </Pie>
            <Tooltip
              contentStyle={TOOLTIP_STYLE}
              formatter={(value, name) => [
                `${formatNumber(Number(value ?? 0))} (${formatPercent(Number(value ?? 0) / total, 0)})`,
                String(name ?? ''),
              ]}
            />
          </PieChart>
        </ResponsiveContainer>

        {/* 차트 위에 겹치므로 마우스 이벤트를 통과시킨다. */}
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-xl font-bold leading-none tracking-metric text-ink">
            {formatNumber(total)}
          </span>
          <span className="mt-1 text-2xs text-ink-muted">{centerLabel}</span>
        </div>
      </div>

      {/* 범례가 곧 표다. 값을 오른쪽 끝에 맞춰 자릿수가 비교되게 한다. */}
      {/* w-full 이 없으면 위아래로 쌓았을 때 목록이 내용 폭만 쓰고 좌우가 빈다.
       * 부모의 items-center 가 폭을 내용에 맞춰 줄이기 때문이다. 가운데로 모아야 하는
       * 것은 원이지 목록이 아니다. */}
      <ul className="flex w-full min-w-0 flex-1 flex-col">
        {visible.map((slice) => (
          <li
            key={slice.label}
            className="flex items-center justify-between gap-3 border-b border-border-default py-2 last:border-b-0"
          >
            <span className="flex min-w-0 items-center gap-2">
              {/* 옅은 회색 조각은 테두리가 없으면 흰 바탕에서 표식 자체가 안 보인다. */}
              <Swatch color={colorOf(slice.label)} bordered />
              <span className="min-w-0 truncate text-xs text-ink-secondary" title={slice.label}>
                {slice.label}
              </span>
            </span>
            <span className="flex shrink-0 items-baseline gap-1.5 tabular-nums">
              <span className="text-xs font-semibold text-ink">{formatNumber(slice.count)}</span>
              <span className="w-7 text-right text-2xs text-ink-muted">
                {formatPercent(slice.count / total, 0)}
              </span>
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
