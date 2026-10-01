import {
  flexRender,
  getCoreRowModel,
  getSortedRowModel,
  useReactTable,
  type ColumnDef,
  type SortingState,
} from '@tanstack/react-table';
import { ArrowDown, ArrowUp, ChevronsUpDown } from 'lucide-react';
import { useState } from 'react';
import { cn } from '@/lib/cn';
import { EmptyState, ErrorState, Skeleton } from '@/components/ui/EmptyState';

/**
 * 열 정렬 방향. 숫자 열은 오른쪽으로 붙여야 자릿수가 눈으로 비교된다.
 * `meta: { align: 'right' }` 로 지정한다.
 */
declare module '@tanstack/react-table' {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  interface ColumnMeta<TData, TValue> {
    align?: 'left' | 'right';
    /** 좁게 유지할 열(ID, 상태 등). 내용이 길어져도 늘어나지 않는다. */
    width?: string;
  }
}

type DataTableProps<T> = {
  columns: ColumnDef<T, unknown>[];
  data: T[];
  loading?: boolean;
  /** 비어 있을 때 문구. 화면마다 다르게 준다. */
  emptyTitle: string;
  emptyDescription?: string;
  /** 행 클릭으로 드릴다운. 주면 커서와 호버가 붙고 위아래 화살표로 행을 옮길 수 있다. */
  onRowClick?: (row: T) => void;
  /**
   * 지금 열려 있는 행. 상세가 옆 패널에서 열리면서 생긴 자리다.
   *
   * <p>패널만 열고 표에 표시를 안 하면 스무 행 중 무엇을 보고 있는지가 화면에서 사라진다.
   * 패널을 닫았다 다시 열 때 어디까지 봤는지를 매번 다시 찾게 된다.
   */
  isRowSelected?: (row: T) => boolean;
  /**
   * 조회 실패. 주면 표 자리에 실패 문구를 그린다.
   *
   * <p>화면이 {@code error ? <ErrorState/> : <DataTable/>} 로 갈라 쓰던 것을 안으로
   * 들였다. 그렇게 쓰면 실패했을 때 <b>표 머리까지 통째로 사라져</b>, 비었을 때와 실패했을
   * 때가 다른 모양이 된다. 로딩과 빈 상태는 이미 이 컴포넌트가 머리를 남긴 채 그린다.
   */
  error?: unknown;
  onRetry?: () => void;
  /** 로딩 스켈레톤 행 수. 실제 표시 행 수와 맞추면 데이터가 와도 화면이 튀지 않는다. */
  skeletonRows?: number;
  className?: string;
};

/**
 * 모든 목록 화면이 쓰는 하나의 표.
 *
 * <p>화면마다 표를 따로 만들면 정렬 표시, 빈 상태, 로딩, 행 높이가 조금씩 달라진다.
 * 운영자는 화면을 옮길 때마다 다시 배워야 하고, 그 차이는 버그처럼 느껴진다.
 *
 * <p>정렬은 클라이언트에서 한다. 한 페이지가 20행이라 서버 왕복이 필요 없고,
 * 페이지 안에서 즉시 뒤집히는 편이 훑기에 낫다.
 */
/**
 * 위아래 화살표로 포커스를 옆 행에 옮긴다.
 *
 * <p>큐를 비우는 일은 같은 동작의 반복이라, 손이 마우스와 키보드를 오가는 비용이 스무 번
 * 그대로 쌓인다. 표 안에서만 움직이므로 tbody 를 벗어나지 않는다. 끝에 닿으면 아무 일도
 * 일어나지 않는다. 순환시키면 마지막 행에서 한 번 더 눌렀을 때 맨 위로 튀어, 어디 있는지를
 * 놓친다.
 */
function moveFocus(current: HTMLElement, direction: 1 | -1) {
  const rows = Array.from(
    current.closest('tbody')?.querySelectorAll<HTMLElement>('tr[tabindex]') ?? [],
  );
  rows[rows.indexOf(current) + direction]?.focus();
}

export function DataTable<T>({
  columns,
  data,
  loading,
  emptyTitle,
  emptyDescription,
  onRowClick,
  isRowSelected,
  error,
  onRetry,
  skeletonRows = 6,
  className,
}: DataTableProps<T>) {
  const [sorting, setSorting] = useState<SortingState>([]);

  const table = useReactTable({
    data,
    columns,
    state: { sorting },
    onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
  });

  const failed = error != null;
  const showEmpty = !failed && !loading && data.length === 0;
  // 실패했으면 스켈레톤을 돌리지 않는다. 재시도 버튼 위에서 회색 막대가 깜빡이면
  // 아직 불러오는 중인 것처럼 보인다.
  const showSkeleton = loading && !failed;

  return (
    /* 넓은 표는 자기 컨테이너 안에서 가로 스크롤한다. 페이지가 통째로 밀리면
     * 좌측 레일과 헤더까지 함께 움직여 화면이 무너진다.
     *
     * 셀 좌우 여백은 12px 인데 <b>맨 앞뒤 열만 20px</b>이다. 12px 로 통일하면 표의 첫
     * 글자가 카드 제목보다 8px 안쪽에서 시작해 카드가 어긋나 보이고, 20px 로 통일하면
     * 열이 많은 표에서 가로 스크롤이 그만큼 빨리 생긴다. 정렬이 필요한 것은 바깥 모서리뿐이다. */
    <div className={cn('overflow-x-auto', className)}>
      <table className="w-full border-collapse text-sm">
        <thead>
          {table.getHeaderGroups().map((headerGroup) => (
            // 회색 띠를 걷어냈다. 카드가 이미 흰 판이라 그 안에 또 다른 색면을 두면
            // 표가 카드 안에 낀 다른 물체처럼 보인다. 머리글과 본문은 굵기와 크기가
            // 이미 다르므로 아래 경계선 하나면 갈린다.
            <tr key={headerGroup.id} className="border-b border-border-default">
              {headerGroup.headers.map((header) => {
                const meta = header.column.columnDef.meta;
                const sortable = header.column.getCanSort();
                const sorted = header.column.getIsSorted();

                return (
                  <th
                    key={header.id}
                    scope="col"
                    style={meta?.width ? { width: meta.width } : undefined}
                    className={cn(
                      'whitespace-nowrap px-3 pb-2 pt-1 text-2xs font-semibold tracking-wide text-ink-muted',
                      'first:pl-5 last:pr-5',
                      meta?.align === 'right' ? 'text-right' : 'text-left',
                    )}
                  >
                    {header.isPlaceholder ? null : sortable ? (
                      <button
                        type="button"
                        onClick={header.column.getToggleSortingHandler()}
                        className={cn(
                          'group/sort inline-flex items-center gap-1 transition-colors hover:text-ink',
                          meta?.align === 'right' && 'flex-row-reverse',
                        )}
                      >
                        {flexRender(header.column.columnDef.header, header.getContext())}
                        {sorted === 'asc' ? (
                          <ArrowUp className="size-3 text-accent" aria-hidden />
                        ) : sorted === 'desc' ? (
                          <ArrowDown className="size-3 text-accent" aria-hidden />
                        ) : (
                          // 정렬 가능한 열임을 늘 알리되, 호버 전에는 눈에 띄지 않게 둔다.
                          <ChevronsUpDown
                            className="size-3 opacity-0 transition-opacity group-hover/sort:opacity-60"
                            aria-hidden
                          />
                        )}
                        <span className="sr-only">
                          {sorted === 'asc'
                            ? '오름차순 정렬됨'
                            : sorted === 'desc'
                              ? '내림차순 정렬됨'
                              : '정렬하려면 누르세요'}
                        </span>
                      </button>
                    ) : (
                      flexRender(header.column.columnDef.header, header.getContext())
                    )}
                  </th>
                );
              })}
            </tr>
          ))}
        </thead>

        <tbody>
          {showSkeleton &&
            Array.from({ length: skeletonRows }).map((_, rowIndex) => (
              <tr key={rowIndex} className="border-b border-border-default">
                {columns.map((_column, columnIndex) => (
                  <td key={columnIndex} className="h-row px-3 first:pl-5 last:pr-5">
                    <Skeleton
                      className="h-3"
                      // 폭을 조금씩 다르게 두면 실제 내용처럼 보여 로딩이 덜 답답하다.
                      style={{ width: `${45 + ((rowIndex * 7 + columnIndex * 13) % 45)}%` }}
                    />
                  </td>
                ))}
              </tr>
            ))}

          {!showSkeleton &&
            !failed &&
            table.getRowModel().rows.map((row) => {
              const selected = isRowSelected?.(row.original) === true;

              return (
                <tr
                  key={row.id}
                  onClick={onRowClick ? () => onRowClick(row.original) : undefined}
                  // 행에 실제 포커스를 준다. 직접 만든 "커서 행" 상태로는 포커스 링과
                  // 스크린리더 낭독을 각각 따로 구현해야 하는데, 브라우저가 이미 한다.
                  tabIndex={onRowClick ? 0 : undefined}
                  aria-selected={isRowSelected ? selected : undefined}
                  onKeyDown={
                    onRowClick
                      ? (event) => {
                          if (event.key === 'Enter' || event.key === ' ') {
                            event.preventDefault();
                            onRowClick(row.original);
                            return;
                          }
                          if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
                            // 표 안에서 움직일 때 페이지가 같이 스크롤되면 포커스가 화면
                            // 밖으로 나간다. 브라우저의 기본 스크롤을 막고 focus 가 옮긴다.
                            event.preventDefault();
                            moveFocus(event.currentTarget, event.key === 'ArrowDown' ? 1 : -1);
                          }
                        }
                      : undefined
                  }
                  className={cn(
                    'border-b border-border-default transition-colors',
                    // 포커스 링 자체는 전역 :focus-visible 이 그린다. 여기서는 안쪽으로만
                    // 당긴다. 기본 offset 2px 이면 링이 행 밖으로 나가 위아래 행에 걸치고,
                    // 표가 가로 스크롤할 때는 좌우가 컨테이너에 잘린다.
                    'focus-visible:-outline-offset-2',
                    // 호버는 회색, 선택은 코랄 틴트다. 둘 다 코랄이면 지나가는 손가락과
                    // 지금 보고 있는 행이 같은 모양이 되어, 마우스를 움직일 때마다
                    // 선택이 옮겨 다니는 것처럼 보인다.
                    //
                    // 회색은 subtle 이다. 캔버스가 흰색이 되면서 bg-canvas 로는 흰 카드
                    // 위에서 아무 일도 일어나지 않았다.
                    onRowClick && 'cursor-pointer hover:bg-subtle',
                    selected && 'bg-selected hover:bg-selected',
                  )}
                >
                  {row.getVisibleCells().map((cell) => {
                    const meta = cell.column.columnDef.meta;
                    return (
                      <td
                        key={cell.id}
                        className={cn(
                          'h-row px-3 text-ink',
                          'first:pl-5 last:pr-5',
                          meta?.align === 'right' && 'text-right tabular-nums',
                        )}
                      >
                        {flexRender(cell.column.columnDef.cell, cell.getContext())}
                      </td>
                    );
                  })}
                </tr>
              );
            })}
        </tbody>
      </table>

      {failed && <ErrorState message={(error as Error)?.message} onRetry={onRetry} />}
      {showEmpty && <EmptyState title={emptyTitle} description={emptyDescription} />}
    </div>
  );
}
