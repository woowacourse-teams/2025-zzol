import { ArrowDown, ArrowUp } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import type { ReactNode } from 'react';
import { Sparkline } from '@/components/Sparkline';
import { cn } from '@/lib/cn';
import { formatNumber } from '@/lib/format';

type TileProps = {
  label: string;
  value: number | string;
  /** 값이 무엇을 세는지. 지표 이름만으로 모호한 것은 여기서 밝힌다. */
  hint?: string;
  /** 값 뒤에 붙는 단위나 보조 수치. "명", "12건" 같은 것. */
  suffix?: ReactNode;
  /** 어제 대비 증감. 없으면 표시하지 않는다. 0을 "변화 없음"으로 꾸미지 않는다. */
  delta?: number;
  /** 같은 지표의 일자별 값. 있으면 숫자 오른쪽에 흐름을 그린다. */
  trend?: number[];
  /**
   * 라벨 오른쪽 끝에 흐리게 세우는 표식.
   *
   * <p>같은 화면의 처리 대기 줄에는 아이콘이 있는데 지표 칸에는 없어서, 같은 종류의
   * 물체가 두 규칙으로 서 있었다. 훑을 때 글자를 읽기 전에 자리부터 잡히는 효과도 있다.
   *
   * <p>흐리게 두는 것이 규칙이다. 또렷하면 라벨보다 먼저 보이는데, 아이콘 혼자로는 무슨
   * 지표인지 말하지 못한다. 자리만 지키는 역할이다.
   */
  icon?: LucideIcon;
  className?: string;
};

/**
 * 지표 한 칸. 백오피스에서 숫자 하나를 보여주는 자리는 <b>전부 이것</b>이다.
 *
 * <p>예전에는 화면마다 따로 있었다. 분석 화면의 {@code StatCard}, 홈의 운영 품질 타일,
 * 신고 화면의 SLA 칸이 각각 손으로 짜여 있었고 라벨 크기와 여백과 숫자 크기가 조금씩
 * 달랐다. 화면을 옮길 때마다 같은 종류의 정보가 다르게 생겨 있으면 읽는 사람이 매번
 * 다시 자리를 잡아야 한다.
 *
 * <h2>규격</h2>
 *
 * <ul>
 *   <li>라벨 12px medium, 보조 잉크. 한 줄로 자른다
 *   <li>값 20px bold, 자간 -0.02em. <b>화면 전체에서 숫자 크기는 이것 하나뿐이다</b>.
 *       한때 타일만 24px 이고 목록 줄과 도넛 가운데는 20px 이었는데, 타일 줄이 옆 카드보다
 *       한 단계 크게 보여 그 줄만 튀었다. 24px 이 필요할 만큼 이 화면에 압도해야 할 숫자는
 *       없다. 13px 본문 위에서 20px bold 면 훑을 때 충분히 걸린다
 *   <li>보조 문구 12px 흐린 잉크. 비어 있어도 자리를 차지한다 - 그래야 문구 없는 칸만
 *       짧아져 줄이 들쭉날쭉해지지 않는다
 * </ul>
 *
 * <p>증감에 색을 붙이지 않는다. 방 생성이 준 것이 나쁜 일인지 좋은 일인지는 지표마다
 * 다르고, 초록이나 빨강을 달면 그 판단을 화면이 대신해 버린다. 방향은 화살표가 말하고
 * 판단은 사람이 한다.
 */
export function Tile({
  label,
  value,
  hint,
  suffix,
  delta,
  trend,
  icon: Icon,
  className,
}: TileProps) {
  const rising = delta !== undefined && delta > 0;

  return (
    <div
      className={cn(
        'flex h-full flex-col rounded-lg border border-border-default bg-surface px-5 py-4 shadow-card',
        className,
      )}
    >
      <div className="flex items-start justify-between gap-2">
        <p className="truncate text-xs font-medium text-ink-secondary" title={label}>
          {label}
        </p>
        {Icon && <Icon className="size-4 shrink-0 text-ink-muted/70" strokeWidth={2} aria-hidden />}
      </div>

      <div className="mt-2 flex items-end justify-between gap-3">
        <div className="flex min-w-0 items-baseline gap-1.5">
          <span className="whitespace-nowrap text-xl font-bold leading-none tracking-metric text-ink">
            {typeof value === 'number' ? formatNumber(value) : value}
          </span>
          {suffix && <span className="whitespace-nowrap text-xs text-ink-muted">{suffix}</span>}

          {delta !== undefined && delta !== 0 && (
            <span className="inline-flex items-center gap-0.5 text-xs font-medium text-ink-secondary">
              {rising ? (
                <ArrowUp className="size-3" aria-hidden />
              ) : (
                <ArrowDown className="size-3" aria-hidden />
              )}
              {formatNumber(Math.abs(delta))}
              <span className="sr-only">어제 대비</span>
            </span>
          )}
        </div>

        {trend && trend.length > 1 && (
          <Sparkline values={trend} className="shrink-0" width={64} height={22} />
        )}
      </div>

      <p className="mt-auto truncate pt-2 text-xs text-ink-muted" title={hint}>
        {hint ?? ' '}
      </p>
    </div>
  );
}
