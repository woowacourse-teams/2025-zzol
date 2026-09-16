import {
  CartesianGrid,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { EvalRun } from '@/api/types';
import { ACTIVE_DOT, AXIS_STYLE, GRID_STYLE, TOOLTIP_STYLE } from '@/components/charts/theme';
import { formatPercent } from '@/lib/format';

/**
 * 실행별 통과율. 이 화면이 존재하는 이유에 직접 답하는 그림이다.
 *
 * <p>표에는 실행마다 "17/20" 이 적혀 있다. 그런데 <b>프롬프트 v2 에서 v3 으로 넘어가며
 * 좋아졌는가</b>는 그 표를 위아래로 훑으며 암산해야 알 수 있었다. 시나리오 개수가 실행마다
 * 달라지면 암산도 틀린다.
 *
 * <h2>선인 이유</h2>
 *
 * <p>가로축이 시간 순이고 세로축이 비율이다. 값 사이의 <b>변화량</b>이 읽어야 할 것이라
 * 선이 맞다. 막대로 그리면 실행 하나하나의 절대 높이가 먼저 보이고 기울기가 뒤로 밀린다.
 *
 * <p>세로축을 0~100%로 고정한다. 데이터 범위에 맞춰 자동으로 잡으면 85%와 87% 사이의
 * 잡음이 화면 전체를 오르내리는 급등락으로 보인다. 평가 통과율에서 그 착시는 배포 판단을
 * 바꾼다.
 *
 * <p>가장 최근 실행의 통과율에 가로 기준선을 긋는다. 과거 실행이 그 선 위에 있는지
 * 아래에 있는지가 "이번에 나빠졌나"의 답이다.
 */
export function EvalPassRateChart({ runs, height = 200 }: { runs: EvalRun[]; height?: number }) {
  // 서버는 최근 것부터 준다. 시간 순으로 뒤집어야 왼쪽이 과거가 된다.
  // 끝나지 않은 실행은 뺀다. 통과 수가 아직 차오르는 중이라 통과율이 0에 가깝게 찍히고,
  // 그 점 하나가 "방금 망가졌다"로 읽힌다.
  const points = runs
    .filter((run) => run.finishedAt !== null && run.scenarioCount > 0)
    .map((run) => ({
      id: run.id,
      label: run.label,
      rate: run.passCount / run.scenarioCount,
      passCount: run.passCount,
      scenarioCount: run.scenarioCount,
    }))
    .reverse();

  const latest = points[points.length - 1];

  return (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart data={points} margin={{ top: 8, right: 8, bottom: 0, left: -14 }}>
        <CartesianGrid {...GRID_STYLE} />
        <XAxis
          dataKey="label"
          {...AXIS_STYLE}
          minTickGap={12}
        />
        <YAxis
          domain={[0, 1]}
          ticks={[0, 0.25, 0.5, 0.75, 1]}
          tickFormatter={(value: number) => formatPercent(value, 0)}
          {...AXIS_STYLE}
          width={46}
        />
        <Tooltip
          cursor={{ stroke: 'var(--border-strong)' }}
          contentStyle={TOOLTIP_STYLE}
          formatter={(value, _name, item) => {
            const point = item?.payload as (typeof points)[number] | undefined;
            return [
              `${formatPercent(Number(value ?? 0), 0)} (${point?.passCount}/${point?.scenarioCount})`,
              '통과율',
            ];
          }}
        />

        {latest && (
          <ReferenceLine
            y={latest.rate}
            stroke="var(--border-strong)"
            strokeDasharray="4 4"
            label={{
              value: '최근',
              position: 'right',
              fontSize: 10,
              fill: 'var(--chart-axis)',
            }}
          />
        )}

        <Line
          type="monotone"
          dataKey="rate"
          name="통과율"
          stroke="var(--chart-1)"
          strokeWidth={2}
          // 실행이 스무 건 이하라 점을 전부 찍는다. 실행 하나하나가 사람이 누른 사건이고,
          // 몇 번 돌았는지가 선의 모양만큼 중요하다.
          dot={{ r: 3, strokeWidth: 0, fill: 'var(--chart-1)' }}
          activeDot={{ ...ACTIVE_DOT, r: 5 }}
        />
      </LineChart>
    </ResponsiveContainer>
  );
}
