import type { ReactNode } from 'react';
import type { Bucket } from '@/api/types';
import { EmptyState } from '@/components/ui/EmptyState';
import { Meter } from '@/components/ui/Meter';
import { cn } from '@/lib/cn';
import { formatNumber, formatPercent } from '@/lib/format';

/**
 * 한 줄에 그릴 값.
 *
 * <p>{@code note} 는 오른쪽 끝의 비중 자리를 대신 차지한다. 비중이 그 줄에서 가장 할 말이
 * 아닌 경우가 있다. 게임별 플레이가 그렇다 - 어느 게임을 많이 하는지는 막대가 이미
 * 말하고, 정작 손댈 거리는 <b>그중 몇 판이 중간에 끊겼는가</b>다.
 *
 * <p>{@code completed} 를 주면 막대가 그만큼만 차고 {@code count} 까지는 옅은 자국으로
 * 남는다. 자국과 막대의 차이가 곧 못 채운 몫이다. 숫자를 읽지 않아도 어느 줄이 덜
 * 찼는지가 먼저 보인다.
 *
 * <p>{@code ratio} 는 막대 길이를 직접 정한다. 목록의 주인공이 개수가 아니라 <b>비율</b>일
 * 때 쓴다(게임별 이탈). 개수로 막대를 그리면 많이 하는 게임이 늘 위에 서서, 열 판 중
 * 여덟 판이 끊기는 게임이 아래에 묻힌다.
 */
export type DistributionRow = Bucket & {
  note?: ReactNode;
  completed?: number;
  ratio?: number;
};

type DistributionBarsProps = {
  data: DistributionRow[];
  /** 값이 전부 0일 때 칸 대신 보여줄 문구. */
  emptyTitle: string;
  emptyDescription?: string;
  /**
   * 줄 앞에 순위 숫자를 붙인다. 값 순으로 정렬된 목록에만 쓴다.
   *
   * <p>목록이 이미 정렬돼 있어도 숫자가 있어야 "3위와 4위가 붙어 있다"를 말로 옮길 수
   * 있다. 반대로 구간 분포(1명, 2~3명…)처럼 순서가 값이 아닌 목록에 숫자를 붙이면
   * 1등이 아닌 칸이 1번으로 읽힌다.
   */
  ranked?: boolean;
  className?: string;
};

/**
 * 구간 분포. 인원수, 플레이 횟수, 소요 시간처럼 <b>순서가 있는 칸</b>을 그린다.
 *
 * <p>가로 막대는 <b>이 컴포넌트 하나</b>다. 순위 목록(게임별 비중)과 구간 목록(진행 단계)이
 * 한때 서로 다른 모양이었다. 하나는 이름이 막대 위에 있고 하나는 왼쪽에 있어서, 같은
 * 화면에 둘이 나란히 서면 줄 높이도 막대 시작점도 맞지 않았다. 같은 일을 하는 그림이
 * 둘이면 한쪽만 고치는 날이 온다. 순위 숫자만 {@code ranked} 로 켜고 끈다.
 *
 * <h2>세로 막대였다가 가로 막대가 됐다</h2>
 *
 * <p>처음에는 세로 기둥이었다. 분포의 모양을 보여주는 데는 세로가 맞지만, 칸이 넷에서
 * 여섯뿐인 그림을 카드 폭 전체에 펴 놓으니 <b>막대 사이가 막대보다 넓어졌다.</b> 게다가
 * 한 칸이 나머지를 압도하면(정상 180 대 나머지 30) 작은 막대들이 바닥에 눌려 붙어 서로
 * 비교도 안 되면서 카드 위쪽 절반이 통째로 빈 공간으로 남았다.
 *
 * <p>가로로 눕히면 그 공백이 사라진다. 막대가 카드 폭을 쓰고, 줄 수가 곧 카드 높이라
 * 남는 자리가 생기지 않는다. 한글 구간 이름도 가로로 읽힌다.
 *
 * <p>값과 비중은 오른쪽 끝에 맞춰 적는다. 세로였을 때는 숫자를 막대 위에 얹어야 했고,
 * 그 숫자들이 서로 다른 높이에 떠 있어 자릿수 비교가 안 됐다.
 *
 * <h2>왜 1등만 색이 있나</h2>
 *
 * <p>분포에서 먼저 읽어야 하는 것은 <b>어느 칸이 가장 두꺼운가</b>다. 1등만 로고색으로
 * 채우고 나머지는 옅게 둔다. 게임별 비중 목록이 1위만 진하게 두는 것과 같은 규칙이다.
 * 값이 같아 1등이 여럿이면 전부 칠한다. 임의로 하나를 고르면 새로고침할 때마다 색이
 * 옮겨 다닌다.
 *
 * <p>막대 길이는 <b>1등 대비</b>다. 전체 대비로 그리면 칸이 여섯일 때 모든 막대가 짧아져
 * 서로 비교가 안 된다. 전체 대비 비중은 숫자로 따로 적으므로 막대는 순위 비교만 맡는다.
 */
export function DistributionBars({
  data,
  emptyTitle,
  emptyDescription,
  ranked,
  className,
}: DistributionBarsProps) {
  const total = data.reduce((sum, bucket) => sum + bucket.count, 0);

  if (total === 0) {
    return <EmptyState title={emptyTitle} description={emptyDescription} />;
  }

  const top = Math.max(...data.map((bucket) => bucket.count));
  // 폭은 목록 전체가 하나로 정한다. 줄마다 note 유무로 정하면 note 가 없는 줄만
  // 좁아져 숫자 끝이 어긋난다.
  const noted = data.some((bucket) => bucket.note !== undefined);

  return (
    <ul className={cn('flex w-full flex-col gap-2.5', className)}>
      {data.map((bucket, index) => (
        <li
          key={bucket.label}
          // 이름 칸을 고정 폭으로 둔다. 내용에 맞춰 늘어나게 두면 카드마다 막대 시작점이
          // 달라져, 나란히 선 카드 둘의 그림이 서로 어긋나 보인다. 폭은 가장 긴 이름
          // ("격리 메시지 재투입", 아홉 자)이 잘리지 않는 값이다. 잘라 놓으면 조치 이름은
          // 앞부분이 서로 비슷해서 무엇인지 구분이 안 된다.
          className={cn(
            'grid items-center gap-3',
            ranked ? 'grid-cols-[1.25rem_7.5rem_1fr_auto]' : 'grid-cols-[7.5rem_1fr_auto]',
          )}
        >
          {ranked && (
            // 1위만 진하게 둔다. 나머지는 순서를 확인하는 용도라 물러나 있어도 된다.
            <span
              className={cn(
                'text-2xs font-semibold tabular-nums',
                index === 0 ? 'text-ink-secondary' : 'text-ink-muted',
              )}
            >
              {index + 1}
            </span>
          )}
          <span className="truncate text-xs text-ink-secondary" title={bucket.label}>
            {bucket.label}
          </span>
          <Meter
            ratio={bucket.ratio ?? (top === 0 ? 0 : (bucket.completed ?? bucket.count) / top)}
            ghostRatio={bucket.completed === undefined || top === 0 ? undefined : bucket.count / top}
            size="sm"
            muted={bucket.count !== top}
          />
          <span className="flex shrink-0 items-baseline gap-1.5 tabular-nums">
            <span className="text-xs font-semibold text-ink">{formatNumber(bucket.count)}</span>
            {/* 비중은 전체 대비다. 막대(1등 대비)와 기준이 다르므로 숫자로만 둔다.
              * note 를 주면 그 자리를 note 가 가져간다. 그때는 폭을 내용에 맡긴다 -
              * 고정 폭을 주면 남는 자리만큼 note 가 앞 숫자에서 떨어져 "10  판 중" 으로
              * 벌어진다. note 는 안쪽 칸마다 폭이 박혀 있어 폭을 풀어도 줄끼리 어긋나지
              * 않는다. */}
            <span className={cn('text-right text-2xs text-ink-muted', noted || 'w-7')}>
              {bucket.note ?? formatPercent(bucket.count / total, 0)}
            </span>
          </span>
        </li>
      ))}
    </ul>
  );
}
