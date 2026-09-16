import { cn } from '@/lib/cn';

type Tab<T extends string | number> = { value: T; label: string };

type TabsProps<T extends string | number> = {
  tabs: Tab<T>[];
  value: T;
  onChange: (value: T) => void;
  /** 접근성 라벨. 무엇을 고르는 컨트롤인지 소리로만 듣는 사람에게 알려준다. */
  label?: string;
  className?: string;
};

/**
 * 세그먼트 컨트롤. 같은 성격의 목록을 갈아 끼우거나 기간을 고를 때 쓴다.
 *
 * <p>화면 세 곳에 각각 손으로 짜여 있던 것을 여기로 모았다. 셋이 조금씩 달랐다 -
 * 테두리 모서리가 하나는 6px 다른 하나는 10px 이었고, 눌리지 않은 항목의 hover 색도
 * 갈렸다. 같은 동작에 다른 모양이 붙으면 화면을 옮길 때마다 새 컨트롤로 보인다.
 *
 * <p>밑줄 탭이 아니라 알약 모양으로 둔다. 밑줄은 카드 머리에 놓으면 카드 경계선과
 * 겹쳐 어느 선이 탭인지 흐려진다. 알약은 카드 안 어디에 놓아도 자기 영역이 분명하다.
 *
 * <p>선택은 회색 채움으로만 표시한다. 여기에 코랄을 쓰면 "손이 필요하다"는 신호와
 * "지금 여기를 보고 있다"가 같은 색이 되어, 화면을 훑을 때 급한 것이 안 보인다.
 */
export function Tabs<T extends string | number>({
  tabs,
  value,
  onChange,
  label,
  className,
}: TabsProps<T>) {
  return (
    <div
      role="group"
      aria-label={label}
      className={cn(
        'inline-flex rounded-md border border-border-default bg-surface p-0.5',
        className,
      )}
    >
      {tabs.map((tab) => (
        <button
          key={tab.value}
          type="button"
          // role="tab" 을 쓰지 않는다. 그러려면 tabpanel 과 aria-controls 까지 맞춰야 하는데
          // 여기서 바뀌는 것은 같은 자리의 내용이라 group + pressed 가 실제 동작에 가깝다.
          aria-pressed={value === tab.value}
          onClick={() => onChange(tab.value)}
          className={cn(
            'rounded-sm px-3 py-1 text-xs font-medium transition-colors',
            value === tab.value
              ? 'bg-subtle text-ink'
              : 'text-ink-muted hover:text-ink-secondary',
          )}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}
