import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';

type PageHeaderProps = {
  title: string;
  /** 이 화면이 무엇을 위한 것인지 한 줄. 지표 화면이면 무엇을 세는지 밝힌다. */
  description?: ReactNode;
  actions?: ReactNode;
  className?: string;
};

/**
 * 모든 페이지의 첫 줄. 화면마다 제목 크기와 여백이 달라지면 화면을 옮길 때마다
 * 눈이 다시 자리를 잡아야 한다.
 *
 * <p>{@code h1} 이다. 상단 바에 메뉴 이름을 작게 한 번 더 찍던 것을 걷어내면서
 * 이 줄이 화면의 유일한 제목이 됐다. 예전에는 상단 바가 {@code h1} 을 물고 있어서
 * 여기가 {@code h2} 였고, 화면에 제목이 두 개로 보였다.
 */
export function PageHeader({ title, description, actions, className }: PageHeaderProps) {
  return (
    <header className={cn('flex items-start justify-between gap-4', className)}>
      <div className="min-w-0">
        <h1 className="text-xl font-semibold tracking-tight text-ink">{title}</h1>
        {description && (
          <p className="mt-1 max-w-2xl text-xs leading-relaxed text-ink-muted">{description}</p>
        )}
      </div>
      {actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
    </header>
  );
}

/** 화면 안의 구획. 카드 여러 장을 묶을 때 쓴다. */
export function Section({
  title,
  description,
  actions,
  children,
  className,
}: {
  title?: string;
  description?: string;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={cn('flex flex-col gap-2.5', className)}>
      {(title || actions) && (
        <div className="flex items-end justify-between gap-3">
          <div>
            {title && <h2 className="text-base font-semibold tracking-tight text-ink">{title}</h2>}
            {description && <p className="mt-0.5 text-xs text-ink-muted">{description}</p>}
          </div>
          {actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
        </div>
      )}
      {children}
    </section>
  );
}
