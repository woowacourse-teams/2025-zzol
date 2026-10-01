import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/lib/cn';
import { formatNumber } from '@/lib/format';

type PaginationProps = {
  /** 0-based. 서버 응답 규격과 같게 둔다. 화면에서만 +1 해서 보여준다. */
  page: number;
  totalPages: number;
  totalElements: number;
  onChange: (page: number) => void;
  className?: string;
};

/**
 * 이전/다음 두 버튼과 위치 표시.
 *
 * <p>페이지 번호를 나열하지 않는다. 운영 목록은 대개 최신순이라 7페이지로 바로 뛸 일이
 * 거의 없고, 번호 버튼은 총 페이지가 늘면 말줄임 규칙까지 따라붙어 코드가 커진다.
 * 특정 건을 찾는 것은 페이지 넘기기가 아니라 검색과 필터가 할 일이다.
 */
export function Pagination({
  page,
  totalPages,
  totalElements,
  onChange,
  className,
}: PaginationProps) {
  if (totalElements === 0) {
    return null;
  }

  const first = page <= 0;
  const last = page >= totalPages - 1;

  return (
    <div
      className={cn(
        'flex items-center justify-between gap-3 border-t border-border-default px-5 py-2.5',
        className,
      )}
    >
      <p className="text-xs text-ink-muted">
        전체 <span className="font-medium text-ink-secondary">{formatNumber(totalElements)}</span>건
        <span className="mx-1.5 text-border-strong">·</span>
        {formatNumber(page + 1)} / {formatNumber(Math.max(totalPages, 1))} 페이지
      </p>

      <div className="flex items-center gap-1">
        <PageButton onClick={() => onChange(page - 1)} disabled={first} label="이전 페이지">
          <ChevronLeft className="size-4" aria-hidden />
        </PageButton>
        <PageButton onClick={() => onChange(page + 1)} disabled={last} label="다음 페이지">
          <ChevronRight className="size-4" aria-hidden />
        </PageButton>
      </div>
    </div>
  );
}

function PageButton({
  onClick,
  disabled,
  label,
  children,
}: {
  onClick: () => void;
  disabled: boolean;
  label: string;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      className={cn(
        'flex size-7 items-center justify-center rounded-md border border-border-default text-ink-secondary transition-colors',
        'hover:border-border-strong hover:text-ink',
        'disabled:cursor-not-allowed disabled:border-border-default disabled:text-border-strong disabled:hover:text-border-strong',
      )}
    >
      {children}
    </button>
  );
}
