import { Link } from 'react-router-dom';
import type { LucideIcon } from 'lucide-react';
import { Meter } from '@/components/ui/Meter';
import { cn } from '@/lib/cn';
import { formatNumber } from '@/lib/format';

export type QueueItem = {
  label: string;
  count: number;
  to: string;
  icon: LucideIcon;
  /** 평소 0이고 0이 아닌 순간이 곧 사고인 것. 이 줄에만 색이 붙는다. */
  critical?: boolean;
};

/**
 * 처리 대기 줄.
 *
 * <p>숫자 옆에 <b>막대</b>를 깐다. 숫자만 다섯 개 세워 두면 어디가 제일 밀렸는지를 알려고
 * 매번 다섯을 읽고 머릿속에서 비교해야 한다. 39와 30은 눈으로 구분되지 않지만 막대 길이는
 * 구분된다. 대시보드에서 비교는 글이 아니라 길이가 해야 한다.
 *
 * <p>기준은 <b>이 다섯 중 가장 큰 값</b>이다. 큐마다 "많다"의 기준이 달라서(신고 39는 흔하고
 * 격리 5는 사고다) 절대 기준을 세울 수 없다. 서로 견주는 것까지만 하고, 그 이상은 각 화면이
 * 맡는다.
 *
 * <p>색은 격리 메시지에만 붙는다. 신고와 검열은 평소에도 쌓이는 것이 정상이라 늘 코랄이면
 * 그 색이 뜻을 잃는다. 격리는 평소 0이고, 회색이던 자리가 코랄로 <b>바뀌는</b> 것이 신호다.
 */
export function ActionQueueStrip({ items }: { items: QueueItem[] }) {
  // 전부 0이면 나누기가 무너진다. 그때는 모든 막대가 0이어야 맞다.
  const top = Math.max(...items.map((item) => item.count), 0);

  return (
    // flex-1 과 justify-between 으로 남는 높이를 줄 사이에 나눠 가진다. 옆 카드와 높이를
    // 맞추려고 그냥 늘리면 목록 아래가 빈 판이 되는데, 간격으로 흡수하면 여백으로 읽힌다.
    <ul className="flex flex-1 flex-col justify-between px-5 pb-5">
      {items.map((item) => (
        <QueueRow key={item.label} item={item} top={top} />
      ))}
    </ul>
  );
}

function QueueRow({ item, top }: { item: QueueItem; top: number }) {
  const idle = item.count === 0;
  const alarming = item.critical === true && !idle;

  return (
    <li>
      <Link
        to={item.to}
        className="-mx-2 block rounded-md px-2 py-1.5 transition-colors hover:bg-subtle"
      >
        <span className="flex items-baseline gap-2">
          <item.icon
            className={cn(
              'size-3.5 shrink-0 translate-y-0.5',
              alarming ? 'text-attention-mark' : 'text-ink-muted',
            )}
            strokeWidth={2}
            aria-hidden
          />
          <span className="min-w-0 flex-1 truncate text-xs text-ink" title={item.label}>
            {item.label}
          </span>
          {/* 0은 흐리게 둔다. 지울 수는 없다 - 칸이 사라지면 그 큐가 없어진 것처럼 보이고,
            * 다섯 줄의 자리가 매번 바뀌면 늘 보던 자리에서 숫자를 찾지 못한다. */}
          <span
            className={cn(
              'shrink-0 text-xs font-semibold tabular-nums',
              idle ? 'text-ink-muted' : 'text-ink',
            )}
          >
            {formatNumber(item.count)}
          </span>
        </span>

        {/* 막대는 이름 아래 전체 폭을 쓴다. 이름과 한 줄에 두면 긴 라벨 하나가 막대 자리를
          * 잡아먹어 줄마다 막대 시작점이 달라진다. 게임별 플레이와 같은 규칙이다. */}
        <Meter
          ratio={top === 0 ? 0 : item.count / top}
          size="sm"
          muted={!alarming}
          className="mt-1.5"
        />
      </Link>
    </li>
  );
}
