import { RacingPlayer } from '@/types/miniGame/racingGame';
import { useMemo, useState } from 'react';

export type RankedPlayer = Pick<RacingPlayer, 'playerName' | 'position'>;

type Props = {
  players: RankedPlayer[];
  endDistance: number;
};

/**
 * 순위표와 완주 연출이 같은 순서를 쓰게 한다.
 *
 * 둘이 따로 계산하면 화면 순위와 연출 순위가 갈린다. 완주자는 통과한 차례대로 고정하고,
 * 아직 달리는 사람만 위치로 정렬한다.
 */
export const useRaceRanking = ({ players, endDistance }: Props) => {
  const [finishOrder, setFinishOrder] = useState<RankedPlayer[]>([]);

  // 결승선 통과 순서를 렌더 중 누적한다(React 공식 "렌더 중 state 조정" 패턴).
  // 새 통과자가 있을 때만 setState → 즉시 재렌더 후 통과자 목록이 비어 무한 루프가 없다.
  // 같은 틱에 둘이 함께 넘으면 서버 배열 순서는 참가 순서라 등수가 아니다.
  // 서버는 결승선을 얼마나 지나쳤는지로 통과 시각을 되짚으므로 더 멀리 간 쪽이 먼저다.
  const newFinishers = players
    .filter(
      ({ playerName, position }) =>
        endDistance > 0 &&
        position >= endDistance &&
        !finishOrder.some((player) => player.playerName === playerName)
    )
    .sort((a, b) => b.position - a.position);
  if (newFinishers.length > 0) {
    setFinishOrder((prev) => [
      ...prev,
      ...newFinishers.map(({ playerName, position }) => ({ playerName, position })),
    ]);
  }

  const rankedPlayers = useMemo(() => {
    const unFinishedSortedPlayers = players
      .filter((player) => !finishOrder.some((p) => p.playerName === player.playerName))
      .sort((a, b) => b.position - a.position);

    return [...finishOrder, ...unFinishedSortedPlayers];
  }, [players, finishOrder]);

  return rankedPlayers;
};
