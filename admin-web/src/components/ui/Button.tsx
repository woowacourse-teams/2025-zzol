import { Slot } from '@radix-ui/react-slot';
import { cva, type VariantProps } from 'class-variance-authority';
import type { ComponentProps } from 'react';
import { cn } from '@/lib/cn';

/**
 * Primary 는 로고색 채움, danger 는 아웃라인이다.
 *
 * <p>primary 의 글자는 흰색이다. 로고색 위 흰 글자는 2.78:1 로 대비 기준에 못 미치는데,
 * 알고 받아들인 예외다. 근거와 한계는 {@code tokens.css} 의 액션 절에 있다.
 *
 * 접근성 때문이 아니라 밀도 때문이다. 표의 모든 행에 빨간 채움 버튼이 있으면 화면이
 * 경고로 뒤덮여 진짜 경고가 안 보인다. 채움 빨강은 확인 다이얼로그의 최종 실행 버튼
 * (`dangerSolid`)에서만 쓴다. 그 자리에는 primary 버튼이 없어 인접 혼동이 없다.
 *
 * <p>누르면 살짝 줄어든다. 토스가 쓰는 방식으로, 클릭이 먹었다는 것을 서버 응답을
 * 기다리지 않고 손끝에 먼저 알려 준다. 응답이 200ms 걸리는 조치에서 차이가 크다.
 */
const button = cva(
  'inline-flex items-center justify-center gap-1.5 whitespace-nowrap rounded-md font-medium ' +
    'transition-[background-color,border-color,color,transform] duration-100 ' +
    'active:scale-[0.97] ' +
    'disabled:pointer-events-none disabled:opacity-40 ' +
    '[&_svg]:size-4 [&_svg]:shrink-0',
  {
    variants: {
      variant: {
        primary: 'bg-action text-action-text hover:bg-action-hover',
        secondary:
          'bg-surface text-ink border border-border-strong hover:border-ink-muted hover:bg-subtle',
        ghost: 'text-ink-secondary hover:bg-subtle hover:text-ink',
        danger:
          'bg-surface text-attention border border-attention-mark/45 hover:border-attention-mark hover:bg-attention-bg',
        dangerSolid: 'bg-attention-solid text-attention-on-solid hover:brightness-110',
      },
      size: {
        sm: 'h-7 px-2.5 text-xs',
        md: 'h-8 px-3.5 text-sm',
        lg: 'h-10 px-5 text-base',
        icon: 'size-8',
      },
    },
    defaultVariants: { variant: 'secondary', size: 'md' },
  },
);

type ButtonProps = ComponentProps<'button'> &
  VariantProps<typeof button> & {
    /** `<Link>` 같은 다른 요소로 렌더링한다. */
    asChild?: boolean;
  };

export function Button({ className, variant, size, asChild, ...props }: ButtonProps) {
  const Component = asChild ? Slot : 'button';
  return <Component className={cn(button({ variant, size }), className)} {...props} />;
}
