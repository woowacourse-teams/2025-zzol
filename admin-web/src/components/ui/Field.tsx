import { ChevronDown, Search } from 'lucide-react';
import type { ComponentProps, ReactNode } from 'react';
import { cn } from '@/lib/cn';

const FIELD_BASE =
  'h-8 w-full rounded-md border border-border-strong bg-surface px-2.5 text-sm text-ink ' +
  'placeholder:text-ink-muted transition-colors ' +
  'hover:border-ink-muted focus:border-accent focus:outline-none ' +
  'disabled:cursor-not-allowed disabled:bg-subtle disabled:text-ink-muted';

export function Input({ className, ...props }: ComponentProps<'input'>) {
  return <input className={cn(FIELD_BASE, className)} {...props} />;
}

/**
 * 검색 입력. 돋보기를 안에 둔다.
 * 별도 "검색" 버튼을 두지 않는 이유는 목록 화면의 검색이 디바운스로 즉시 반영되기 때문이다.
 * 누를 필요가 없는 버튼이 놓여 있으면 눌러야 하는 줄 안다.
 */
export function SearchInput({ className, ...props }: ComponentProps<'input'>) {
  return (
    <div className={cn('relative', className)}>
      <Search
        className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-ink-muted"
        aria-hidden
      />
      <input type="search" className={cn(FIELD_BASE, 'pl-8')} {...props} />
    </div>
  );
}

/**
 * 네이티브 select 를 스타일링만 한다. Radix 를 얹지 않는 이유는 백오피스의 선택지가
 * 대부분 열 개 미만이고, 네이티브가 키보드와 모바일에서 이미 잘 동작하기 때문이다.
 */
/**
 * 드롭다운.
 *
 * <p>화살표를 lucide 아이콘으로 그린다. 한때 배경 이미지에 SVG data URI 를 넣었는데
 * <b>한 번도 그려지지 않았다.</b> Tailwind 의 임의값은 공백을 담을 수 없어서
 * {@code viewBox='0 0 24 24'} 의 공백에서 클래스 생성이 끊겼고, 그 결과 화살표 없는
 * 셀렉트가 입력칸과 구분되지 않은 채로 남아 있었다.
 *
 * <p>같은 이유로 이 자리에 data URI 를 다시 쓰지 않는다. 아이콘은 화면의 다른 모든
 * 아이콘과 같은 곳에서 오고 색도 역할 별칭을 그대로 받는다.
 */
export function Select({ className, children, ...props }: ComponentProps<'select'>) {
  return (
    <span className={cn('relative inline-flex w-full', className)}>
      <select className={cn(FIELD_BASE, 'w-full cursor-pointer appearance-none pr-8')} {...props}>
        {children}
      </select>
      {/* 클릭이 셀렉트로 가야 한다. 아이콘 위를 눌렀을 때 아무 일도 안 일어나면
        * 고장 난 것으로 읽힌다. */}
      <ChevronDown
        className="pointer-events-none absolute right-2.5 top-1/2 size-4 -translate-y-1/2 text-ink-muted"
        aria-hidden
      />
    </span>
  );
}

type LabelProps = Omit<ComponentProps<'label'>, 'title'> & {
  /** 라벨 문구. 컨트롤은 children 으로 받는다. */
  text: ReactNode;
  hint?: ReactNode;
  /**
   * 라벨 오른쪽 끝에 붙는 글자 수 같은 수치.
   *
   * <p>{@code hint} 와 자리가 다르다. hint 는 컨트롤 아래에서 "무엇을 넣는 칸인가"를
   * 말하고, 이쪽은 위에서 <b>지금 얼마나 찼는가</b>를 말한다. 한때 제목은 아래 왼쪽,
   * 본문은 위 오른쪽에 세던 것을 여기로 모았다. 같은 것을 두 자리에서 세면 폼을 훑을 때
   * 눈이 두 번 움직인다.
   */
  counter?: ReactNode;
};

/**
 * 입력 한 줄. 라벨 문구, 컨트롤, 보조 문구 순으로 쌓는다.
 *
 * <p>문구와 컨트롤을 따로 받는다. 둘을 children 하나로 받으면 라벨 텍스트용 span 안에
 * input 이 들어가 문구 스타일이 컨트롤까지 덮고, 보조 문구가 컨트롤 아래가 아니라
 * 엉뚱한 자리에 붙는다.
 *
 * <p>{@code <label>} 로 감싸므로 {@code htmlFor} 없이도 클릭이 컨트롤로 간다.
 */
export function Label({ className, text, children, hint, counter, ...props }: LabelProps) {
  return (
    <label className={cn('flex flex-col gap-1.5', className)} {...props}>
      <span className="flex items-baseline justify-between gap-2 text-xs font-medium text-ink-secondary">
        {text}
        {counter && <span className="text-2xs font-normal text-ink-muted">{counter}</span>}
      </span>
      {children}
      {hint && <span className="text-2xs text-ink-muted">{hint}</span>}
    </label>
  );
}
