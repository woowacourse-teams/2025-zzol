import type { ComponentProps, ReactNode } from 'react';
import { cn } from '@/lib/cn';

/**
 * 카드. 캔버스 위에 떠 있는 위젯이다.
 *
 * <p>한때 그림자를 쓰지 않고 1px 경계선만으로 층위를 만들었다. 표가 많은 화면에서
 * 그림자가 노이즈라고 봤기 때문인데, 실제로 만들어 놓고 보니 화면이 <b>문서처럼</b>
 * 읽혔다. 카드가 바닥에 붙어 있으면 그 안의 표와 카드 사이에 경계가 안 생긴다.
 *
 * <p>그래서 뒤집었다. 처음에는 아주 얕게(0.04, 0.06 두 겹) 뒀는데 흰 바닥 위의 흰
 * 카드에서 있는지 없는지 모를 정도였다. 지금은 세 겹으로 깔되 번짐 반경을 키워, 진해지는
 * 대신 부드럽게 뜬다. 경계선은 남긴다 - 그림자만으로는 흰 카드와 밝은 캔버스의
 * 경계가 화면 밝기를 낮춘 환경에서 사라진다.
 *
 * <p>밀도는 잃지 않았다. 표는 {@code CardBody} 가 아니라 카드 직속으로 들어가므로
 * 아래에서 늘린 여백이 행 높이에 닿지 않는다.
 */
export function Card({ className, ...props }: ComponentProps<'div'>) {
  return (
    <div
      className={cn('rounded-lg border border-border-default bg-surface shadow-card', className)}
      {...props}
    />
  );
}

type CardHeaderProps = {
  title: ReactNode;
  /** 제목 옆 보조 설명. 지표가 무엇을 세는지 한 줄로 밝힌다. */
  description?: ReactNode;
  actions?: ReactNode;
  className?: string;
};

export function CardHeader({ title, description, actions, className }: CardHeaderProps) {
  return (
    <div
      // 구분선을 긋지 않는다. 카드를 가로지르는 선은 그 자체로 "표의 머리글"처럼 읽혀서
      // 위젯이 아니라 문서 한 장으로 보이게 만든다. 제목과 본문은 여백으로 나눈다.
      className={cn('flex items-start justify-between gap-3 px-5 pb-3 pt-5', className)}
    >
      <div className="min-w-0">
        <h2 className="text-base font-semibold tracking-tight text-ink">{title}</h2>
        {description && (
          <p className="mt-0.5 text-xs text-ink-muted">{description}</p>
        )}
      </div>
      {actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
    </div>
  );
}

export function CardBody({ className, ...props }: ComponentProps<'div'>) {
  return <div className={cn('px-5 pb-5', className)} {...props} />;
}
