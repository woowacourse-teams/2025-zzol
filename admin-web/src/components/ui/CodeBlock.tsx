import { Check, Copy } from 'lucide-react';
import { useState } from 'react';
import { cn } from '@/lib/cn';

type CodeBlockProps = {
  value: string;
  /** 넘치면 스크롤한다. 기본 24rem. */
  maxHeightClassName?: string;
  className?: string;
};

/**
 * 로그, JSON, 트레이스 같은 원문 뷰어.
 *
 * <p>복사 버튼이 있다. 운영 대화는 대개 원문을 슬랙에 붙이며 시작하는데, 드래그로 긁으면
 * 줄바꿈이 깨지거나 옆 열까지 딸려 온다.
 */
export function CodeBlock({ value, maxHeightClassName = 'max-h-96', className }: CodeBlockProps) {
  const [copied, setCopied] = useState(false);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      // 클립보드 권한이 없으면 조용히 넘어간다. 원문은 화면에 그대로 있어 드래그로 복사할 수 있다.
    }
  };

  return (
    <div className={cn('group relative', className)}>
      <button
        type="button"
        onClick={copy}
        aria-label="복사"
        className={cn(
          'absolute right-2 top-2 flex size-7 items-center justify-center rounded-md border border-border-default bg-surface text-ink-muted transition',
          'opacity-0 group-hover:opacity-100 hover:text-ink focus-visible:opacity-100',
        )}
      >
        {copied ? (
          <Check className="size-3.5 text-ink" aria-hidden />
        ) : (
          <Copy className="size-3.5" aria-hidden />
        )}
      </button>

      <pre
        className={cn(
          'overflow-auto rounded-md border border-border-default bg-subtle p-3',
          'font-mono text-xs leading-relaxed text-ink',
          maxHeightClassName,
        )}
      >
        {value}
      </pre>
    </div>
  );
}
