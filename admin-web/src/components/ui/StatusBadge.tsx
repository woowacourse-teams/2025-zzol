import { cva, type VariantProps } from 'class-variance-authority';
import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';

/**
 * 상태 배지. 톤이 셋뿐이다.
 *
 * <p>초록도 노랑도 파랑도 없다. 상태 색이 여럿이면 한 화면에 다 떠서 어느 것이 급한지가
 * 흐려진다. 운영자가 색을 보고 손을 움직이는 순간은 "손이 필요하다" 하나뿐이고
 * 그 자리를 브랜드 코랄이 맡는다. 나머지는 회색의 명암으로 가른다.
 *
 * <p>코랄 배지에는 점을 찍고 회색 배지에는 찍지 않는다. 색 + 점 + 텍스트 3중 인코딩이라
 * 색각 이상에서도, 흑백 스크린샷에서도 "이것만 다르다"가 읽힌다. 운영 대화는 대개
 * 스크린샷으로 시작한다.
 */
const badge = cva(
  // whitespace-nowrap 이 없으면 좁은 열에서 "UI 추가"가 두 줄로 접히면서 배지가
  // 세로로 부풀어 그 행만 키가 커진다. 배지는 낱말이 아니라 <b>표식</b>이라 줄을 바꾸면
  // 안 된다. 열이 좁으면 열을 넓히는 것이 맞다.
  'inline-flex items-center gap-1.5 whitespace-nowrap rounded-sm px-1.5 py-0.5 text-2xs font-medium',
  {
    variants: {
      tone: {
        /** 끝난 것. 물러난다. 처리 완료, 정상, 이미 지난 일. */
        muted: 'bg-subtle text-ink-muted',
        /** 지금 상태. 기본값이다. */
        neutral: 'bg-subtle text-ink-secondary',
        /** 손이 필요하거나 막혔거나 되돌릴 수 없는 것. 화면에서 유일하게 색이 붙는다. */
        attention: 'bg-attention-bg text-attention',
      },
    },
    defaultVariants: { tone: 'neutral' },
  },
);

type StatusBadgeProps = VariantProps<typeof badge> & {
  children: ReactNode;
  className?: string;
};

export function StatusBadge({ tone = 'neutral', children, className }: StatusBadgeProps) {
  return (
    <span className={cn(badge({ tone }), className)}>
      {tone === 'attention' && (
        <span className="size-1.5 rounded-full bg-attention-mark" aria-hidden />
      )}
      {children}
    </span>
  );
}
