import type { ReactNode } from 'react';
import { Skeleton } from '@/components/ui/EmptyState';
import { cn } from '@/lib/cn';

/** 한 줄에 몇 칸인가. 넷이 기본이고, 셋은 유저 상세, 다섯은 홈의 처리 대기 줄이다. */
type Columns = 3 | 4 | 5;

const COLUMNS: Record<Columns, string> = {
  3: 'grid-cols-2 xl:grid-cols-3',
  4: 'grid-cols-2 xl:grid-cols-4',
  5: 'grid-cols-2 lg:grid-cols-5',
};

/**
 * 타일 줄. 좁은 화면에서는 두 칸으로 접힌다.
 *
 * <p>격자 정의를 여기 하나로 모은 이유는 화면마다 적던 것이 조금씩 갈렸기 때문이다.
 * 어떤 화면은 {@code gap-3}, 어떤 화면은 {@code gap-4} 였고 접히는 기준점도 달랐다.
 * 같은 타일 넷이 화면에 따라 다른 간격으로 놓여 있으면 다른 종류의 정보처럼 보인다.
 */
export function TileGrid({
  children,
  columns = 4,
  className,
}: {
  children: ReactNode;
  columns?: Columns;
  className?: string;
}) {
  return <div className={cn('grid gap-3', COLUMNS[columns], className)}>{children}</div>;
}

/**
 * 타일 줄의 로딩 자리.
 *
 * <p>격자 없이 자리 표시만 낸다. 실패 자리를 {@code Loaded} 에 넘길 때 격자 안쪽에
 * 들어가는 경우가 있어서다. 격자가 필요하면 감싸는 쪽이 {@link TileGrid} 로 감싼다.
 */
export function TileSkeletons({
  count = 4,
  height = 'h-[6.25rem]',
}: {
  count?: number;
  /** 실제 타일 높이와 같게 준다. 어긋나면 데이터가 올 때 화면이 튄다. */
  height?: string;
}) {
  return (
    <>
      {Array.from({ length: count }).map((_, index) => (
        <Skeleton key={index} className={cn('rounded-lg', height)} />
      ))}
    </>
  );
}
