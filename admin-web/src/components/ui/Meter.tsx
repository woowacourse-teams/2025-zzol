import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';

type MeterProps = {
  /** 채움 비율. 0~1. 1을 넘으면 잘린다. */
  ratio: number;
  /**
   * 뒤에 옅게 깔 비율. "여기까지 있었다"를 남길 때 쓴다.
   * 퍼널에서 직전 단계의 길이를 자국으로 남기면 그 차이가 이번 단계의 이탈량이 된다.
   */
  ghostRatio?: number;
  /** {@code lg} 는 안에 값을 적을 수 있는 두께, {@code sm} 은 순위 비교용 얇은 선. */
  size?: 'sm' | 'lg';
  /** 강조를 낮춘다. 순위 2위 이하처럼 1위와 구분해야 할 때. */
  muted?: boolean;
  /** 트랙 위에 겹칠 내용. 두꺼운 막대에서만 쓴다. */
  children?: ReactNode;
  className?: string;
};

const TRACK: Record<'sm' | 'lg', string> = {
  sm: 'h-1.5 rounded-full',
  lg: 'h-7 rounded-md',
};

const FILL: Record<'sm' | 'lg', string> = {
  sm: 'rounded-full',
  lg: 'rounded-md',
};

/**
 * 트랙 위의 막대. 퍼널과 순위 목록이 같은 걸 쓴다.
 *
 * <p>둘을 따로 짰을 때 트랙 색, 모서리 반경, 0일 때의 처리가 갈렸다. 특히 <b>0을 어떻게
 * 그릴지</b>가 달랐는데, 이건 취향이 아니라 판단이다. 값이 0이면 <b>아무것도 안 그린다</b>.
 * 1픽셀이라도 남기면 "적지만 있다"로 읽히고, 그건 0과 완전히 다른 상태다. 반대로 0이
 * 아닌데 비율이 아주 작으면 최소 2%를 준다. 있는데 안 보이는 것도 거짓말이다.
 */
export function Meter({
  ratio,
  ghostRatio,
  size = 'lg',
  muted,
  children,
  className,
}: MeterProps) {
  return (
    <span className={cn('relative block overflow-hidden bg-subtle', TRACK[size], className)}>
      {ghostRatio !== undefined && ghostRatio > ratio && (
        <span
          className={cn('absolute inset-y-0 left-0 bg-accent/20', FILL[size])}
          style={{ width: `${clamp(ghostRatio) * 100}%` }}
          aria-hidden
        />
      )}

      {ratio > 0 && (
        <span
          className={cn(
            'absolute inset-y-0 left-0 transition-[width] duration-300',
            FILL[size],
            muted ? 'bg-accent/55' : 'bg-accent',
          )}
          style={{ width: `${Math.max(clamp(ratio) * 100, 2)}%` }}
          aria-hidden
        />
      )}

      {children && (
        <span className="relative flex h-full items-center px-2.5 text-xs font-semibold text-ink">
          {children}
        </span>
      )}
    </span>
  );
}

function clamp(ratio: number) {
  if (!Number.isFinite(ratio) || ratio < 0) {
    return 0;
  }
  return Math.min(ratio, 1);
}
