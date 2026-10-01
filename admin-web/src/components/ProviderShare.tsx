import type { ProviderStats } from '@/api/types';
import { DonutChart } from '@/components/charts/DonutChart';
import { formatNumber } from '@/lib/format';
import { providerLabel } from '@/lib/labels';

/**
 * 소셜 제공자 분포.
 *
 * <h2>왜 여기만 도넛인가</h2>
 *
 * <p>제공자는 셋이고, 묻는 것은 "카카오가 절반을 넘나"처럼 <b>전체 대비 크기</b>다. 조각
 * 셋의 부채꼴은 눈으로 바로 비교되고, 합이 하나의 원을 이룬다는 사실 자체가 정보다.
 * 게임별 비중처럼 항목이 여덟을 넘고 꼴찌를 봐야 하는 자리는 반대 이유로 순위 막대다.
 *
 * <p>조각 색은 로고색 하나와 회색들이다. 색을 세 번 갈아엎고 나온 답이다. 자세한 경위는
 * {@code DonutChart} 주석에 있다.
 *
 * <p><b>합이 회원 수가 아니다.</b> 한 사람이 여러 소셜을 연결할 수 있어서 이 그림은
 * "회원의 구성"이 아니라 "연결의 구성"이다. 그 차이를 카드 바닥에 적어 둔다.
 */
export function ProviderShare({
  stats,
  layout,
}: {
  stats: ProviderStats;
  layout?: 'row' | 'column';
}) {
  const total = stats.providers.reduce((sum, row) => sum + row.count, 0);

  return (
    <div className="flex flex-col gap-4">
      <DonutChart
        slices={stats.providers.map((row) => ({
          label: providerLabel(row.provider),
          count: row.count,
        }))}
        centerLabel="연결"
        layout={layout}
      />

      {/* 단서는 목록 안이 아니라 카드 바닥에 한 줄로 둔다. 목록의 마지막 항목처럼 놓여
       * 있으면 네 번째 제공자로 잘못 읽힌다. */}
      <p className="text-2xs leading-relaxed text-ink-muted">
        연결 {formatNumber(total)}건, 회원 {formatNumber(stats.userCount)}명. 한 사람이 여러 소셜을
        연결할 수 있어 두 숫자는 맞지 않습니다.
      </p>
    </div>
  );
}
