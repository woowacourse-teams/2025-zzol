import { X } from 'lucide-react';
import { useEffect, type ReactNode } from 'react';
import { useSearchParams } from 'react-router-dom';
import { cn } from '@/lib/cn';

/**
 * 목록 옆에서 상세를 여는 패널.
 *
 * <p>상세를 별도 라우트로 두면 화면이 통째로 바뀐다. 신고를 읽고 그 방을 보고 다시
 * 목록으로 돌아오면 필터와 페이지가 초기화돼 있어서, 운영자는 스무 건을 처리하는 동안
 * 같은 필터를 스무 번 다시 건다. <b>조사는 목록을 떠나지 않고 이뤄져야 한다.</b>
 *
 * <p>모달이 아니다. 뒤를 어둡게 덮지 않고 포커스도 가두지 않는다. 덮는 순간 "이것부터
 * 끝내라"는 뜻이 되는데, 여기서 하려는 것은 정반대로 목록과 상세를 <b>같이 보는 것</b>이다.
 * 1320px 본문에서 패널이 420px 를 가려도 목록 900px 가 남아 행을 계속 읽을 수 있다.
 *
 * <p>레일과 같은 방식으로 가장자리에서 12px 떠 있다. 화면에 붙은 서랍이 아니라 카드와
 * 같은 종류의 물체로 읽혀야 위젯이 놓인 판이라는 화면의 성격이 유지된다.
 */

/** 폭은 둘뿐이다. 내용마다 최적값을 찾으면 패널을 열 때마다 다른 물체가 나온 것처럼 보인다. */
const WIDTH = {
  /** 기본. 키-값 목록, 처리 폼, 한 건의 요약 */
  narrow: 'w-[26.25rem]', // 420px
  /** 원문, JSON, 표가 들어갈 때만 */
  wide: 'w-[45rem]', // 720px
} as const;

type ContextPanelProps = {
  open: boolean;
  onClose: () => void;
  title: ReactNode;
  description?: ReactNode;
  /** 바닥에 고정되는 조치 버튼. 본문이 길어도 조치가 화면 밖으로 밀리지 않는다. */
  footer?: ReactNode;
  width?: keyof typeof WIDTH;
  children: ReactNode;
};

export function ContextPanel({
  open,
  onClose,
  title,
  description,
  footer,
  width = 'narrow',
  children,
}: ContextPanelProps) {
  // Esc 로 닫는다. 모달이 아니라 포커스를 가두지 않으므로 창 단위로 듣는다.
  useEffect(() => {
    if (!open) {
      return;
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [open, onClose]);

  return (
    <aside
      role="complementary"
      aria-hidden={!open}
      // 닫혀 있을 때 탭 이동으로 안쪽에 들어가지지 않게 한다. 화면 밖으로 밀어 둔 것뿐이라
      // inert 가 없으면 보이지 않는 버튼에 포커스가 걸려 커서가 사라진 것처럼 보인다.
      inert={!open}
      className={cn(
        // 높이는 <b>내용이 정한다.</b> 위아래를 꽉 채우면 키-값 여섯 줄짜리 신고를 열었을 때
        // 본문과 바닥 버튼 사이가 1000px 넘게 빈다. 카드가 캔버스 위에 떠 있는 화면에서
        // 속이 빈 판 하나가 서 있으면 그 자리만 미완성으로 읽힌다.
        //
        // 화면을 넘길 만큼 길면 그때 max-h 가 잡고 본문이 스크롤한다.
        'fixed right-rail-inset top-rail-inset z-30 flex max-h-[calc(100vh-1.5rem)] flex-col overflow-hidden',
        'rounded-lg border border-border-default bg-surface shadow-dialog',
        // 120ms 는 열린 줄 모르게 지나가고 240ms 는 기다려진다. 그 사이 하나만 쓴다.
        'transition-[transform,opacity] duration-[160ms] ease-out',
        WIDTH[width],
        open ? 'translate-x-0 opacity-100' : 'pointer-events-none translate-x-3 opacity-0',
      )}
    >
      <header className="flex shrink-0 items-start justify-between gap-3 px-5 pb-3 pt-5">
        <div className="min-w-0">
          {/* 카드 제목과 같은 급이다. 패널이라고 한 단계 키우면 본문의 카드들보다
            * 위에 있는 것처럼 읽혀서, 목록과 나란히 보라는 의도와 어긋난다. */}
          <h2 className="truncate text-base font-semibold tracking-tight text-ink">{title}</h2>
          {description && <p className="mt-0.5 text-xs text-ink-muted">{description}</p>}
        </div>
        <button
          type="button"
          onClick={onClose}
          aria-label="패널 닫기"
          className="flex size-7 shrink-0 items-center justify-center rounded-md text-ink-muted transition-colors hover:bg-subtle hover:text-ink"
        >
          <X className="size-4" aria-hidden />
        </button>
      </header>

      <div className="min-h-0 flex-1 overflow-y-auto px-5 pb-5">{children}</div>

      {footer && (
        <footer className="flex shrink-0 items-center justify-end gap-2 border-t border-border-default px-5 py-3">
          {footer}
        </footer>
      )}
    </aside>
  );
}

/**
 * 열린 패널을 주소에 남긴다.
 *
 * <p>`?open=room:ABC12` 형태다. 주소에 없으면 새로고침에 패널이 사라지고, 조사하다 찾은
 * 화면을 동료에게 링크로 넘길 수도 없다. 운영 중에 "이거 좀 봐 주세요"는 늘 링크로 오간다.
 *
 * <p>히스토리에 쌓지 않고 <b>갈음한다</b>(replace). 패널을 열고 닫은 횟수만큼 뒤로가기를
 * 눌러야 목록을 벗어난다면 뒤로가기가 쓸모없어진다.
 */
export function usePanelParam(kind: string) {
  const [params, setParams] = useSearchParams();
  const raw = params.get('open');
  const value = raw?.startsWith(`${kind}:`) ? raw.slice(kind.length + 1) : null;

  const open = (next: string) => {
    const updated = new URLSearchParams(params);
    updated.set('open', `${kind}:${next}`);
    setParams(updated, { replace: true });
  };

  const close = () => {
    const updated = new URLSearchParams(params);
    updated.delete('open');
    setParams(updated, { replace: true });
  };

  return { value, open, close };
}
