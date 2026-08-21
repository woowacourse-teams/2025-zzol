import { PlayerProbability } from '@/types/roulette';

export const MAX_NEARBY_PLAYERS = 4;

/** 나로부터 몇 번째로 가까운가. 0 은 나 자신이다. */
export type NearbyPlayer = PlayerProbability & { proximityRank: number };

/**
 * 나와 확률이 가까운 참가자를 골라 확률 내림차순으로 돌려준다.
 *
 * 전원을 보여주면 토글 뒤 전체 확률 목록과 같아진다. 차이가 작은 순으로 추려야
 * "내가 지금 누구랑 붙어 있나"를 읽을 수 있다.
 *
 * 화면에는 순위처럼 보여야 하므로 확률 내림차순으로 정렬하되,
 * 자리가 모자랄 때 지워야 하는 것은 나와 가장 먼 사람이므로
 * 근접 순위(proximityRank)를 함께 남긴다. 두 순서는 서로 다르다.
 */
export const pickNearbyPlayers = (
  players: PlayerProbability[],
  myName: string,
  count: number = MAX_NEARBY_PLAYERS
): NearbyPlayer[] => {
  const me = players.find((player) => player.playerName === myName);
  if (!me) return [];

  const nearby = players
    .filter((player) => player.playerName !== myName)
    .sort((a, b) => {
      const distanceGap =
        Math.abs(a.probability - me.probability) - Math.abs(b.probability - me.probability);
      // 거리가 같으면 나보다 앞선 쪽을 먼저 보여준다 — 서버 정렬에 기대지 않기 위함
      return distanceGap !== 0 ? distanceGap : b.probability - a.probability;
    })
    .slice(0, Math.max(count, 0))
    .map((player, index) => ({ ...player, proximityRank: index + 1 }));

  return [{ ...me, proximityRank: 0 }, ...nearby].sort((a, b) => b.probability - a.probability);
};
