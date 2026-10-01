import * as AlertDialog from '@radix-ui/react-alert-dialog';
import { TriangleAlert } from 'lucide-react';
import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';
import { Button } from '@/components/ui/Button';

type ConfirmDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  /** 무엇이 일어나는지. 되돌릴 수 없으면 그 사실을 여기 쓴다. */
  description: ReactNode;
  /**
   * 조치 대상을 다시 보여준다. 어떤 IP 인지, 어떤 닉네임인지.
   * 표에서 잘못된 행의 버튼을 눌렀을 때 여기서 알아차릴 마지막 기회다.
   */
  target?: ReactNode;
  confirmLabel: string;
  onConfirm: () => void;
  pending?: boolean;
  /** 파괴적이지 않은 확인(예: 처리 완료 표시)이면 false 로 둔다. */
  destructive?: boolean;
};

/**
 * 되돌릴 수 없는 조치 앞에 서는 확인 창.
 *
 * <p>취소가 기본 포커스다. 엔터를 습관적으로 누르는 사람이 실행으로 직행하지 않게 한다.
 * Radix 의 AlertDialog 는 바깥 클릭으로 닫히지 않아 이런 용도에 맞다.
 */
export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  target,
  confirmLabel,
  onConfirm,
  pending,
  destructive = true,
}: ConfirmDialogProps) {
  return (
    <AlertDialog.Root open={open} onOpenChange={onOpenChange}>
      <AlertDialog.Portal>
        <AlertDialog.Overlay className="fixed inset-0 z-40 bg-ink/25 backdrop-blur-[2px] data-[state=open]:animate-in data-[state=open]:fade-in" />
        <AlertDialog.Content
          className={cn(
            'fixed left-1/2 top-1/2 z-50 w-[min(28rem,calc(100vw-2rem))] -translate-x-1/2 -translate-y-1/2',
            'rounded-lg border border-border-default bg-surface p-5 shadow-dialog',
          )}
        >
          <div className="flex gap-3">
            <span
              className={cn(
                'flex size-8 shrink-0 items-center justify-center rounded-md',
                destructive ? 'bg-attention-solid text-attention-icon-on-solid' : 'bg-subtle text-ink-secondary',
              )}
            >
              <TriangleAlert className="size-4" aria-hidden />
            </span>

            <div className="min-w-0 flex-1">
              <AlertDialog.Title className="text-base font-semibold tracking-tight text-ink">
                {title}
              </AlertDialog.Title>
              <AlertDialog.Description className="mt-1 text-xs leading-relaxed text-ink-secondary">
                {description}
              </AlertDialog.Description>

              {target && (
                <div className="mt-3 rounded-md border border-border-default bg-subtle px-3 py-2 font-mono text-xs text-ink">
                  {target}
                </div>
              )}
            </div>
          </div>

          <div className="mt-5 flex justify-end gap-2">
            {/* 취소가 먼저이자 기본 포커스다. */}
            <AlertDialog.Cancel asChild>
              <Button variant="secondary" disabled={pending}>
                취소
              </Button>
            </AlertDialog.Cancel>
            <AlertDialog.Action asChild>
              <Button
                variant={destructive ? 'dangerSolid' : 'primary'}
                onClick={(event) => {
                  // 비동기 처리 동안 창을 열어 둔다. 닫아 버리면 실패했을 때
                  // 무엇이 실패했는지 알려줄 자리가 사라진다.
                  event.preventDefault();
                  onConfirm();
                }}
                disabled={pending}
              >
                {pending ? '처리 중...' : confirmLabel}
              </Button>
            </AlertDialog.Action>
          </div>
        </AlertDialog.Content>
      </AlertDialog.Portal>
    </AlertDialog.Root>
  );
}
