import type { CSSProperties, ReactNode } from 'react';
import { cn } from '@/lib/cn';

type EmptyStateProps = {
  /** 무엇이 없는지. "결과 없음" 대신 "미처리 신고가 없습니다"처럼 구체적으로. */
  title: string;
  description?: string;
  action?: ReactNode;
  className?: string;
};

/**
 * 빈 상태. 화면마다 문구를 따로 준다.
 *
 * "데이터가 없습니다" 하나로 돌려쓰면 운영자가 "필터를 잘못 걸었나, 원래 없나"를 구분하지
 * 못한다. 비어 있는 이유를 아는 것은 화면이고, 그 문구는 화면이 정해야 한다.
 */
export function EmptyState({ title, description, action, className }: EmptyStateProps) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center gap-1 px-4 py-12 text-center',
        className,
      )}
    >
      <p className="text-sm font-medium text-ink-secondary">{title}</p>
      {description && <p className="text-xs text-ink-muted">{description}</p>}
      {action && <div className="mt-3">{action}</div>}
    </div>
  );
}

type ErrorStateProps = {
  message?: string;
  onRetry?: () => void;
  className?: string;
};

export function ErrorState({ message, onRetry, className }: ErrorStateProps) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center gap-2 px-4 py-12 text-center',
        className,
      )}
    >
      {/* 글자는 잉크로 두고 옆의 코랄 점이 신호를 진다. 문장을 로고색으로 물들이면
       * 읽히도록 어둡게 내려야 하고, 그 순간 브랜드가 아니라 에러 빨강이 된다. */}
      <p className="flex items-center gap-1.5 text-sm font-medium text-attention">
        <span className="size-1.5 rounded-full bg-attention-mark" aria-hidden />
        불러오지 못했습니다
      </p>
      {message && <p className="max-w-md text-xs text-ink-muted">{message}</p>}
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-1 text-xs font-medium text-accent-ink underline underline-offset-2"
        >
          다시 시도
        </button>
      )}
    </div>
  );
}

/** 로딩 자리 표시. 실제 행 높이와 같게 두어 데이터가 오면 화면이 튀지 않는다. */
export function Skeleton({
  className,
  style,
}: {
  className?: string;
  style?: CSSProperties;
}) {
  return <div className={cn('animate-pulse rounded-md bg-subtle', className)} style={style} />;
}
