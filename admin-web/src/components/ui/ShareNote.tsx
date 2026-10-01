import { formatNumber, formatPercent } from '@/lib/format';

type ShareNoteProps = {
  value: number;
  total: number;
  /** 숫자 뒤에 붙는 말. "개가 완주", "명이 최근 7일" 처럼 문장이 되게 적는다. */
  suffix: string;
};

/**
 * 분포 카드 제목 옆에 붙는 결론 한 줄.
 *
 * <p>막대 그래프는 <b>모양</b>을 말하지 결론을 말하지 않는다. "완주한 방이 몇 퍼센트인가"는
 * 막대 다섯 개를 눈으로 더해야 나오는 값이고, 그 덧셈을 볼 때마다 다시 하게 두면 카드를
 * 스쳐 지나가게 된다. 한 칸을 골라 전체 대비로 적어 두면 그림을 안 읽고도 판단이 선다.
 *
 * <p>숫자만 20px 이고 뒤따르는 말은 작다. 화면의 지표 숫자가 전부 같은 크기여야 어느 것이
 * 더 중요한 숫자인지를 크기로 착각하지 않는다.
 */
export function ShareNote({ value, total, suffix }: ShareNoteProps) {
  return (
    <span className="flex items-baseline gap-1.5 tabular-nums">
      <span className="text-xl font-bold leading-none tracking-metric text-ink">
        {formatNumber(value)}
      </span>
      <span className="text-2xs text-ink-muted">
        {suffix}
        {total > 0 && ` (${formatPercent(value / total, 0)})`}
      </span>
    </span>
  );
}
