import { Meter } from '@/components/ui/Meter';
import { TimelineDot } from '@/components/ui/Timeline';
import { cn } from '@/lib/cn';
import { formatNumber, formatPercent } from '@/lib/format';

export type FunnelStage = {
  label: string;
  count: number;
};

type FunnelBarProps = {
  stages: FunnelStage[];
  className?: string;
};

/**
 * 방 진행 퍼널. 이 서비스가 잘 되고 있는지에 가장 직접적으로 답하는 그림이다.
 *
 * <p>막대 길이는 <b>1단계 대비 비율</b>이다. 앞 단계 대비로 그리면 매 단계가 100%에서
 * 시작해 어디서 크게 새는지가 안 보인다. 대신 단계마다 앞 단계 대비 전환율을 따로 적어
 * 두 질문("전체에서 얼마나 남았나", "여기서 얼마나 빠졌나")에 모두 답한다.
 *
 * <p>세로 막대가 아니라 가로다. 단계 이름이 한글이라 세로 축에 두면 잘리거나 회전한다.
 *
 * <h2>단계 사이를 이어 그린다</h2>
 *
 * <p>예전에는 막대 다섯 개를 그냥 세로로 늘어놓았다. 그러면 <b>서로 무관한 다섯 개 지표</b>로
 * 읽힌다. 퍼널의 핵심은 각 칸의 크기가 아니라 <b>칸과 칸 사이에서 얼마가 빠졌는가</b>인데,
 * 그 빠짐이 화면에 아무 자리도 차지하지 않았다.
 *
 * <p>그래서 두 가지를 넣었다. 왼쪽에 {@link TimelineDot} 으로 단계를 잇고, 막대 뒤 빈 자리에
 * <b>직전 단계까지 있던 길이</b>를 옅은 자국으로 남겼다. 자국과 실제 막대의 차이가 그대로
 * 이탈량이다.
 */
export function FunnelBar({ stages, className }: FunnelBarProps) {
  const first = stages[0]?.count ?? 0;
  const ratioOf = (count: number) => (first === 0 ? 0 : count / first);

  return (
    <ol className={cn('flex flex-col', className)}>
      {stages.map((stage, index) => {
        const previous = index === 0 ? undefined : stages[index - 1]?.count;
        const stepRatio =
          previous === undefined || previous === 0 ? undefined : stage.count / previous;
        const dropped = previous === undefined ? 0 : previous - stage.count;
        const last = index === stages.length - 1;

        return (
          <li key={stage.label} className="grid grid-cols-[7.5rem_1fr] gap-3">
            <div className="flex items-start">
              <TimelineDot last={last} accent={last} />
              <span className="ml-2 truncate pt-0.5 text-xs text-ink-secondary" title={stage.label}>
                {stage.label}
              </span>
            </div>

            {/* 줄 사이 여백은 여기서 만든다. 왼쪽 칸에 넣으면 잇는 선이 그만큼 짧아진다. */}
            <div className={cn('flex items-center gap-3', last ? 'pb-0' : 'pb-2.5')}>
              <Meter
                ratio={ratioOf(stage.count)}
                ghostRatio={previous === undefined ? undefined : ratioOf(previous)}
                className="flex-1"
              >
                {formatNumber(stage.count)}
              </Meter>

              <span className="w-[5.5rem] shrink-0 text-right text-xs tabular-nums">
                {stepRatio === undefined ? (
                  <span className="text-ink-muted">기준</span>
                ) : (
                  <>
                    <span className="font-medium text-ink-secondary">
                      {formatPercent(stepRatio, 0)}
                    </span>
                    {dropped > 0 && (
                      <span className="ml-1 text-ink-muted">-{formatNumber(dropped)}</span>
                    )}
                  </>
                )}
              </span>
            </div>
          </li>
        );
      })}
    </ol>
  );
}
