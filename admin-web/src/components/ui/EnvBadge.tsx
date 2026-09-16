import { ENV_NAME, type EnvName } from '@/lib/env';
import { cn } from '@/lib/cn';

/**
 * 지금 어느 환경을 보고 있는지. 레일 머리의 로고 옆에 <b>항상</b> 떠 있다.
 *
 * <p>PROD 만 색이 붙는다. DEV 와 LOCAL 은 회색이다. 둘을 색으로 갈라 봐야
 * 잘못 눌렀을 때 손해가 없는 환경끼리 구분하는 것이라 얻는 게 없고,
 * 색이 늘면 정작 PROD 가 눈에 안 걸린다.
 *
 * <p>prod 에서 IP 차단 해제를 누르는 것과 dev 에서 누르는 것은 결과가 완전히 다른데
 * 화면은 똑같이 생겼다. 이 배지가 유일한 구분 장치다.
 */
const TONE: Record<EnvName, string> = {
  PROD: 'bg-attention-solid text-attention-on-solid',
  DEV: 'bg-subtle text-ink-secondary border border-border-strong',
  LOCAL: 'bg-subtle text-ink-muted border border-border-default',
};

export function EnvBadge({ className }: { className?: string }) {
  return (
    <span
      className={cn(
        'rounded-sm px-1.5 py-0.5 text-2xs font-bold tracking-wide',
        TONE[ENV_NAME],
        className,
      )}
    >
      {ENV_NAME}
    </span>
  );
}
