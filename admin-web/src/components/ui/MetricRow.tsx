import { Sparkline } from '@/components/Sparkline';
import { formatNumber } from '@/lib/format';

type MetricRowProps = {
  label: string;
  value: number | string;
  /** 값이 무엇을 세는지. 지표 이름만으로 모호한 것은 여기서 밝힌다. */
  hint?: string;
  /**
   * 같은 지표의 일자별 값. 주면 값 왼쪽에 흐름을 그린다.
   *
   * <p>빈 배열을 주면 그림 없이 <b>자리만</b> 차지한다. 한 목록 안에서 어떤 줄만 계열이
   * 있을 때 쓴다. 아예 넘기지 않으면 자리도 안 생긴다.
   */
  series?: number[];
};

/**
 * 카드 안 목록의 지표 한 줄. 라벨은 왼쪽, 값은 오른쪽.
 *
 * <p>값을 오른쪽에 세로로 맞춰 두면 위아래로 훑을 때 자릿수가 눈으로 비교된다. 같은
 * 정보를 타일 여러 장으로 늘어놓으면 그 비교가 안 된다. 그래서 <b>서로 견줘야 하는 값들은
 * 타일이 아니라 이 줄로</b> 쌓는다.
 *
 * <p>숫자 칸에 <b>최소폭</b>을 준다. 폭이 없으면 자릿수에 따라 숫자가 좌우로 밀려 여러
 * 줄의 숫자가 제각각인 자리에 서고, 세로로 쌓은 이유 자체가 사라진다. 다만 고정폭이면
 * 안 된다. 이 자리에는 "1일 20시간" 같은 문자열도 들어오고, 고정폭 안에서 그런 값은
 * 줄바꿈으로 부서진다.
 *
 * <p>값 뒤에 단위 말고는 아무것도 붙이지 않는다. 비율이나 다른 지표를 뒤에 달면 그 줄만
 * 숫자가 왼쪽으로 밀린다. 그런 정보는 {@code hint} 로 내린다.
 */
export function MetricRow({ label, value, hint, series }: MetricRowProps) {
  return (
    <div className="flex items-center justify-between gap-3 py-2.5">
      <dt className="min-w-0">
        <span className="block text-sm text-ink-secondary">{label}</span>
        {hint && <span className="block text-2xs text-ink-muted">{hint}</span>}
      </dt>
      <dd className="flex shrink-0 items-center gap-3">
        {series && <Sparkline values={series} width={56} height={20} />}
        {/* 최소폭이지 고정폭이 아니다. w-12 로 못 박았더니 "1일 20시간" 같은 문자열 값이
          * 48px 안에서 세 줄로 쪼개졌다. 숫자를 오른쪽에 맞추려던 것이 값을 부숴 놨다.
          * 최소폭은 한두 자리 숫자끼리의 정렬을 지키고, 긴 값은 자기 폭만큼 밀고 나간다. */}
        <span className="min-w-12 whitespace-nowrap text-right text-xl font-bold leading-none tracking-metric text-ink">
          {typeof value === 'number' ? formatNumber(value) : value}
        </span>
      </dd>
    </div>
  );
}
