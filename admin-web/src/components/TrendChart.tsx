import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { DailyTrend } from '@/api/types';
import { ACTIVE_DOT, AXIS_STYLE, GRID_STYLE, TOOLTIP_STYLE } from '@/components/charts/theme';
import { formatNumber } from '@/lib/format';

type TrendChartProps = {
  data: DailyTrend[];
  height?: number;
};

/** 두 그림이 같은 날짜 위에 서려면 왼쪽 눈금 칸이 같아야 한다. */
const AXIS_WIDTH = 40;
const MARGIN = { top: 4, right: 4, bottom: 0, left: 0 };

/**
 * 최근 흐름. 오늘 숫자만으로는 "0인데 정상인가"에 답할 수 없다.
 *
 * <h2>한 판이 아니라 두 판이다</h2>
 *
 * <p>한때 막대(방)와 선(참여자)을 <b>한 축에</b> 겹쳐 그렸다. 이중 축을 쓰지 않으려는
 * 판단은 맞았지만 결과가 못 읽혔다. 참여자가 방보다 자릿수가 한 단계 커서 축이 그쪽에
 * 맞춰지고, 막대는 전부 바닥 1/6 구간에 깔려 <b>일별 차이가 사라졌다.</b> 날마다 방이
 * 세 개인지 아홉 개인지가 안 보이는 추이 그래프는 그 자리에 있을 이유가 없다.
 *
 * <p>그래서 위아래로 나눴다. 각자 자기 축을 갖되 <b>가로축을 공유</b>한다. 이중 축이
 * 금지된 이유는 두 계열을 한 그림에 겹쳐 놓고 교차점이 무슨 뜻인 것처럼 보이게 만들기
 * 때문인데, 판을 나누면 겹치는 자리 자체가 없어 그 거짓말이 성립하지 않는다. 같은 날짜가
 * 같은 가로 위치에 오므로 "방이 튄 날 참여자도 튀었나"는 그대로 읽힌다.
 *
 * <p>위가 막대인 것은 방 생성과 완주가 <b>하루에 몇 건</b>인 이산 합계이기 때문이다.
 * 둘을 나란히 세우면 완주가 생성을 얼마나 따라갔는지가 막대 높이 차이로 바로 읽힌다.
 * 아래가 선인 것은 참여자가 "몇 명이 있었나"라 이어지는 양으로 읽어도 되기 때문이다.
 *
 * <p>가로축 눈금은 아래에만 둔다. 두 번 적으면 같은 날짜가 화면에 두 줄로 서서 판이
 * 둘이라는 것만 강조된다. 위 그림은 아래 그림의 날짜를 빌려 쓴다.
 */
export function TrendChart({ data, height = 220 }: TrendChartProps) {
  // 막대가 주인공이라 조금 더 준다. 선은 모양만 읽으면 되므로 낮아도 손해가 없다.
  const barHeight = Math.round(height * 0.58);
  const lineHeight = height - barHeight;

  return (
    <div className="flex flex-col gap-1">
      <ResponsiveContainer width="100%" height={barHeight}>
        <BarChart data={data} margin={MARGIN} barGap={2}>
          <CartesianGrid {...GRID_STYLE} />
          <XAxis dataKey="date" hide />
          <YAxis {...AXIS_STYLE} width={AXIS_WIDTH} allowDecimals={false} />
          <Tooltip
            // 그날 칸 전체를 옅게 누른다. 선 차트의 세로 실선을 그대로 쓰면 막대 사이
            // 어디를 가리키는지 애매해진다.
            cursor={{ fill: 'var(--chart-grid)', fillOpacity: 0.55 }}
            contentStyle={TOOLTIP_STYLE}
            formatter={(value, name) => [formatNumber(Number(value ?? 0)), String(name ?? '')]}
          />
          {/* 90일까지 열려 있어 막대가 얇아진다. 최대 폭을 잡아 두면 구간이 짧을 때 막대
           * 하나가 통짜로 뚱뚱해지는 것을 막고, 길어지면 알아서 얇아진다.
           *
           * 색은 로고색과 회색 둘뿐이다. 한때 완주를 파랑, 참여자를 초록으로 뒀는데
           * 그 셋이 한 카드에 모이면서 화면에서 여기만 알록달록해졌다. 주인공인 방 생성이
           * 로고색이고 완주는 회색이다. 아래 판은 계열이 하나뿐이라 다시 로고색을 쓴다 -
           * 판이 나뉘어 있어 두 로고색이 서로 다른 것을 뜻한다고 오해될 자리가 없다. */}
          <Bar
            dataKey="created"
            name="방 생성"
            fill="var(--chart-1)"
            maxBarSize={14}
            radius={[3, 3, 0, 0]}
          />
          <Bar
            dataKey="completed"
            name="완주"
            fill="var(--gray-300)"
            maxBarSize={14}
            radius={[3, 3, 0, 0]}
          />
        </BarChart>
      </ResponsiveContainer>

      {/* 두 판 사이에 실선을 긋는다.
       *
       * 눈금이 서로 다른데 경계가 없으면 아래 선이 위 막대와 같은 축의 연장으로 읽혀서,
       * 참여자 20이 방 20과 같은 높이인 줄 알게 된다. 선 하나가 "여기서부터 다른 눈금"을
       * 말하고, 그 위의 이름이 무엇의 눈금인지를 말한다. */}
      <div className="mt-1 flex items-center border-t border-border-default pl-[40px] pt-1.5">
        <span className="text-2xs text-ink-muted">참여자</span>
      </div>

      <ResponsiveContainer width="100%" height={lineHeight}>
        <LineChart data={data} margin={MARGIN}>
          <CartesianGrid {...GRID_STYLE} />
          <XAxis
            dataKey="date"
            tickFormatter={(value: string) => value.slice(5).replace('-', '/')}
            {...AXIS_STYLE}
            minTickGap={16}
          />
          {/* 눈금을 셋으로 묶는다. 낮은 판에 다섯을 넣으면 숫자가 서로 붙어 읽히지 않고,
           * 여기서 정확한 값은 툴팁이 말한다. */}
          <YAxis {...AXIS_STYLE} width={AXIS_WIDTH} allowDecimals={false} tickCount={3} />
          <Tooltip
            cursor={{ stroke: 'var(--chart-grid)' }}
            contentStyle={TOOLTIP_STYLE}
            formatter={(value, name) => [formatNumber(Number(value ?? 0)), String(name ?? '')]}
          />
          <Line
            type="monotone"
            dataKey="players"
            name="참여자"
            stroke="var(--chart-1)"
            strokeWidth={2}
            dot={false}
            activeDot={ACTIVE_DOT}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
