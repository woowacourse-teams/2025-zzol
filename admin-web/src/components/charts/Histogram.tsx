import type { Bucket } from '@/api/types';
import { EmptyState } from '@/components/ui/EmptyState';
import { cn } from '@/lib/cn';
import { formatNumber, formatPercent } from '@/lib/format';

type HistogramProps = {
  data: Bucket[];
  /**
   * 기둥 영역의 <b>최소</b> 높이. 값 라벨과 구간 이름은 이 높이 밖에 붙는다.
   *
   * <p>최대가 아니라 최소인 이유는, 격자가 한 줄의 카드를 가장 높은 것에 맞춰 늘리기
   * 때문이다. 높이를 고정하면 늘어난 만큼이 카드 바닥의 빈 공간으로 남는다. 기둥이
   * 그 자리를 쓰게 둔다.
   */
  minBarHeight?: number;
  emptyTitle: string;
  emptyDescription?: string;
};

/**
 * 구간 분포. <b>순서가 있는 수치 구간</b>을 그린다. 인원수, 플레이 횟수, 소요 시간,
 * 신뢰도처럼 "작은 쪽에서 큰 쪽으로" 늘어서는 값이다.
 *
 * <h2>왜 세로이고, 왜 붙어 있나</h2>
 *
 * <p>가로로 눕히면 구간의 순서가 위에서 아래로 흐른다. 수치 축은 왼쪽에서 오른쪽으로
 * 읽는 것이 몸에 배어 있어서, 눕히는 순간 "2~3명이 1명보다 오른쪽"이라는 감각이 사라진다.
 * 히스토그램이 세로인 데는 이유가 있다.
 *
 * <p>대신 기둥을 <b>붙인다.</b> recharts 기본값처럼 막대 사이를 벌리면 칸이 다섯뿐인
 * 그림에서 막대보다 틈이 넓어지고, 카드 폭의 절반이 빈 공간이 된다. 구간이 연속된
 * 값이라는 사실도 틈이 있으면 안 보인다. 1px 간격만 두어 경계만 남긴다.
 *
 * <p>recharts 를 쓰지 않는다. 기둥 다섯과 숫자 다섯이 전부라 축도 격자도 툴팁도 필요
 * 없고, ResponsiveContainer 가 부모 높이를 못 재서 통째로 사라지는 사고도 없앤다.
 *
 * <h2>왜 1등만 색이 있나</h2>
 *
 * <p>분포에서 먼저 읽어야 하는 것은 어느 칸이 가장 두꺼운가다. 값이 같아 1등이 여럿이면
 * 전부 칠한다. 임의로 하나를 고르면 새로고침할 때마다 색이 옮겨 다닌다.
 */
export function Histogram({
  data,
  minBarHeight = 120,
  emptyTitle,
  emptyDescription,
}: HistogramProps) {
  const total = data.reduce((sum, bucket) => sum + bucket.count, 0);

  if (total === 0) {
    return <EmptyState title={emptyTitle} description={emptyDescription} />;
  }

  const top = Math.max(...data.map((bucket) => bucket.count));

  return (
    <div className="flex h-full items-stretch gap-px">
      {data.map((bucket) => (
        <div key={bucket.label} className="flex min-w-0 flex-1 flex-col items-center gap-1">
          {/* 0인 칸에는 숫자를 적지 않는다. 바닥에 0이 줄줄이 서면 그 줄이 축처럼 보인다. */}
          <span className="text-2xs tabular-nums text-ink-muted">
            {bucket.count === 0 ? '' : formatNumber(bucket.count)}
          </span>

          <span className="flex w-full flex-1 items-end" style={{ minHeight: minBarHeight }}>
            <span
              className={cn(
                'w-full rounded-t-[3px]',
                bucket.count === top ? 'bg-accent' : 'bg-accent/30',
              )}
              // 0이면 아무것도 그리지 않는다. 1px 이라도 남기면 "적지만 있다"로 읽히고,
              // 그건 0과 완전히 다른 상태다.
              style={{
                height: bucket.count === 0 ? 0 : `${Math.max((bucket.count / top) * 100, 3)}%`,
              }}
              title={`${bucket.label} ${formatNumber(bucket.count)} (${formatPercent(bucket.count / total, 0)})`}
            />
          </span>

          <span
            className="w-full truncate text-center text-2xs text-ink-muted"
            title={bucket.label}
          >
            {bucket.label}
          </span>
        </div>
      ))}
    </div>
  );
}
