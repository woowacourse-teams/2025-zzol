import { cn } from '@/lib/cn';

/**
 * 표식 모양은 <b>차트에서 그 계열이 그려진 모양</b>과 같아야 한다.
 *
 * <p>막대인 것은 네모, 선인 것은 선, 채워진 면은 사각 칩이다. 셋 다 같은 점으로 그리면
 * 범례를 보고 나서 차트에서 어느 것이 막대이고 어느 것이 선인지 다시 찾아야 한다.
 */
export type LegendShape = 'bar' | 'line' | 'chip';

type SwatchProps = {
  color: string;
  shape?: LegendShape;
  /**
   * 테두리를 두른다. 카카오 노랑처럼 흰 바탕에서 대비가 부족한 색은 테두리가 없으면
   * 표식 자체가 안 보인다.
   */
  bordered?: boolean;
  className?: string;
};

export function Swatch({ color, shape = 'chip', bordered, className }: SwatchProps) {
  const shapeClass =
    shape === 'bar'
      ? 'h-3 w-2 rounded-[2px]'
      : shape === 'line'
        ? 'h-0.5 w-4 rounded-full'
        : 'size-2.5 rounded-sm';

  return (
    <span
      className={cn('shrink-0', shapeClass, bordered && 'border border-border-strong', className)}
      style={{ backgroundColor: color }}
      // 검사 스크립트가 "한 그림 안의 조각 색이 서로 구분되는가"를 잴 때 이 표시를 찾는다.
      // 상태 배지의 점과 생김새가 같아 클래스만으로는 갈라낼 수 없다.
      data-swatch=""
      aria-hidden
    />
  );
}

export type LegendItem = {
  label: string;
  color: string;
  shape?: LegendShape;
};

/**
 * 계열 이름 줄. 카드 머리에 한 줄로 눕는다.
 *
 * <p>계열이 둘 이상이면 <b>범례는 늘 있다</b>. 색만으로 계열을 구분하게 두면 색각 이상인
 * 사람과 흑백 인쇄에서 그림이 무의미해진다.
 *
 * <p>값까지 함께 읽어야 하는 자리(도넛 옆)는 이걸 쓰지 않는다. 거기서는 범례가 곧 표라
 * 이름과 값과 비율이 세로로 정렬돼야 한다. {@link Swatch} 만 가져다 쓴다.
 */
export function Legend({ items, className }: { items: LegendItem[]; className?: string }) {
  return (
    <div className={cn('flex flex-wrap items-center gap-3 text-xs text-ink-secondary', className)}>
      {items.map((item) => (
        <span key={item.label} className="inline-flex items-center gap-1.5">
          <Swatch color={item.color} shape={item.shape} />
          {item.label}
        </span>
      ))}
    </div>
  );
}
