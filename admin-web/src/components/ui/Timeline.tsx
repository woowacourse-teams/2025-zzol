import { cn } from '@/lib/cn';

type TimelineDotProps = {
  /** 마지막 항목이면 아래로 잇는 선을 그리지 않는다. 이어질 것이 없다. */
  last?: boolean;
  /** 점을 로고색으로 채운다. 퍼널의 마지막 단계, 실패한 조치처럼 눈이 먼저 가야 할 곳. */
  accent?: boolean;
  className?: string;
};

/**
 * 세로로 이어지는 목록의 점과 선.
 *
 * <p>퍼널의 단계와 조치 이력이 같은 걸 쓴다. 둘 다 <b>서로 무관한 항목 여럿</b>이 아니라
 * 하나로 이어진 흐름인데, 가로 구분선으로 그리면 그 성격이 지워진다. 세로선은 반대로 잇는다.
 *
 * <p>선은 이 칸의 바닥까지 내려간다. 그래서 <b>줄 사이 여백은 이 칸이 아니라 옆 칸이</b>
 * 만든다. 여백을 여기 넣으면 선이 그만큼 짧아져 항목 사이가 끊겨 보인다.
 *
 * <p>가로로 늘어난 부모 안에서 높이를 스스로 못 잡으므로 {@code self-stretch} 로
 * 줄 높이를 따라간다. 부모가 {@code items-start} 여도 이 칸만 늘어난다.
 */
export function TimelineDot({ last, accent, className }: TimelineDotProps) {
  return (
    <span className={cn('relative flex w-3 shrink-0 justify-center self-stretch', className)}>
      <span
        className={cn(
          // 표면 색 링을 둘러 점이 선 위에 얹힌 것처럼 보이게 한다.
          'z-10 mt-1.5 size-2 shrink-0 rounded-full ring-2 ring-surface',
          accent ? 'bg-accent' : 'bg-border-strong',
        )}
        aria-hidden
      />
      {!last && (
        // 아래로 6px 더 뺀다. 이 칸은 줄 바닥에서 끝나는데 다음 줄의 점은 자기 위쪽에
        // 6px 여백을 두고 시작하므로, 그냥 두면 줄마다 6px 씩 선이 끊긴다. 빼는 값이
        // 다음 점의 mt 와 같으므로 옆 칸의 여백이 얼마든 상관없다.
        <span className="absolute bottom-0 top-3 -mb-1.5 w-px bg-border-default" aria-hidden />
      )}
    </span>
  );
}
