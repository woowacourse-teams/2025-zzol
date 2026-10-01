import { ENV_NAME } from '@/lib/env';
import { cn } from '@/lib/cn';

/**
 * 지금 어느 환경을 보고 있는지. 레일 머리의 로고 옆에 <b>항상</b> 떠 있다.
 *
 * <p>PROD, DEV, LOCAL 모두 로고색 채움에 흰 글자로 같은 모양이다. 환경은 색이 아니라
 * 배지의 글자로 구분한다.
 *
 * <p>prod 에서 IP 차단 해제를 누르는 것과 dev 에서 누르는 것은 결과가 완전히 다른데
 * 화면은 똑같이 생겼다. 이 배지가 유일한 구분 장치다.
 */
export function EnvBadge({ className }: { className?: string }) {
  return (
    <span
      className={cn(
        'rounded-sm px-1.5 py-0.5 text-2xs font-bold tracking-wide',
        'bg-attention-solid text-ink-inverse',
        className,
      )}
    >
      {ENV_NAME}
    </span>
  );
}
