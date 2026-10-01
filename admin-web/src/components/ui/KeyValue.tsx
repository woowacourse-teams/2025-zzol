import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';

type Item = {
  label: string;
  value: ReactNode;
  /** 한 줄을 통째로 쓴다. 내용이 길거나 JSON 인 항목에 쓴다. */
  full?: boolean;
};

/**
 * 상세 화면의 속성 목록. 방 상세, 유저 상세가 쓴다.
 *
 * <p>`dl` 로 쓴다. 라벨과 값의 관계가 마크업에 남아 스크린리더가 "이 값이 무엇인지"를
 * 읽어 줄 수 있다. div 두 개로 만들면 시각적으로만 짝이 맞는다.
 *
 * <p>값이 비면 빈칸이 아니라 `-` 를 찍는다. 빈칸은 "값이 없다"와 "안 불러왔다"를 구분하지 못한다.
 */
export function KeyValue({ items, className }: { items: Item[]; className?: string }) {
  return (
    <dl className={cn('grid grid-cols-1 gap-x-6 gap-y-3 sm:grid-cols-2', className)}>
      {items.map((item) => (
        <div key={item.label} className={cn('min-w-0', item.full && 'sm:col-span-2')}>
          <dt className="text-xs text-ink-muted">{item.label}</dt>
          <dd className="mt-0.5 break-words text-sm text-ink">
            {item.value === null || item.value === undefined || item.value === '' ? (
              <span className="text-ink-muted">-</span>
            ) : (
              item.value
            )}
          </dd>
        </div>
      ))}
    </dl>
  );
}
