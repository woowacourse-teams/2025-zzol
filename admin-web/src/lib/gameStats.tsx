import type { GamePlayStat } from '@/api/types';
import { formatPercent } from '@/lib/format';

/** 시작했지만 끝나지 못한 판. 결과가 한 줄도 안 남은 판이다. */
function droppedOf(stat: GamePlayStat) {
  return Math.max(stat.started - stat.finished, 0);
}

/** 시작한 판 중 끊긴 판의 비율. 시작이 없으면 0. */
function dropOffRate(stat: GamePlayStat) {
  return stat.started === 0 ? 0 : droppedOf(stat) / stat.started;
}

/**
 * 한 줄의 오른쪽 끝에 붙는 이탈 표기.
 *
 * <p>앞 칸의 <b>시작 판 수</b>에 이어 붙는다. 줄 전체가 "10판 중 4판 이탈 40%" 한
 * 문장으로 읽혀야 해서, 여는 말이 "판 중" 이다. 이 표기를 쓰는 줄은 count 가 반드시
 * 시작 판 수여야 한다.
 *
 * <p>칸마다 폭이 박혀 있다. "10판 중 4판 이탈" 과 "83판 중 10판 이탈" 을 통째로 오른쪽에
 * 붙이면 자릿수가 다른 줄끼리 글자 위치가 어긋나, 세로로 훑을 때 숫자가 눈에 안 들어온다.
 *
 * <p>비율만 진하게 둔다. 이 표기에서 먼저 읽어야 하는 수다. 앞의 두 수는 그 비율이
 * 잡음인지 아닌지를 가려 주는 역할이라 물러나 있어도 된다.
 */
function dropOffNote(stat: GamePlayStat) {
  // -ml-1.5 는 앞 칸의 숫자에 붙여 읽히게 한다. 사이가 벌어지면 "10 판 중" 으로 떨어진다.
  return (
    <span className="-ml-1.5 flex items-baseline justify-end">
      <span className="text-ink-muted">판 중</span>
      <span className="ml-1.5 w-[1.5rem] text-right text-ink-secondary">{droppedOf(stat)}</span>
      <span className="text-ink-muted">판 이탈</span>
      <span className="ml-2 w-[2.5rem] text-right text-xs font-semibold text-ink">
        {formatPercent(dropOffRate(stat), 0)}
      </span>
    </span>
  );
}

/**
 * 게임별 플레이 줄. <b>홈과 방</b>이 같이 쓴다.
 *
 * <p>막대는 <b>완주한 판</b>까지만 차고 시작한 판까지는 옅은 자국으로 남는다. 그 꼬리가
 * 곧 끊긴 판이고, 오른쪽 끝에 "10판 중 4판 이탈 40%" 로 같은 사실이 숫자로 적힌다.
 *
 * <p>한때 방 화면만 이탈률 순으로 다시 세운 카드를 옆에 따로 뒀다. 작은 숫자가 판 수
 * 순서에 묻힌다는 이유였는데, 같은 게임 목록이 서로 다른 순서로 두 번 서니 한 게임을
 * 두 카드에서 찾아 맞춰 보게 됐다. 줄마다 비율이 적혀 있으면 순서는 하나로 족하다.
 *
 * <p>줄 순서는 <b>판 수</b> 그대로다. 먼저 묻는 것은 "뭘 많이 하나"이고, 이탈은 그 옆에
 * 붙어 "그런데 이건 새고 있다"를 알린다. 기간 전체 이탈률은 카드 제목 옆이 맡는다.
 */
export function toGameRowsWithDropOff(stats: GamePlayStat[]) {
  return stats.map((stat) => ({
    label: stat.label,
    count: stat.started,
    completed: stat.finished,
    note: dropOffNote(stat),
  }));
}
