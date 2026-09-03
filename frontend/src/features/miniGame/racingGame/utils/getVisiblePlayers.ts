import { RacingPlayer } from '@/types/miniGame/racingGame';

const VISIBLE_PLAYER_COUNT = 7;

export type VisiblePlayers = {
  /** 화면에 세울 플레이어. 앞선 사람이 위에 오도록 position 내림차순이다. */
  players: RacingPlayer[];
  /** 잘려서 안 보이는 인원. 나보다 앞선 쪽과 뒤진 쪽을 나눠 센다. */
  hiddenAhead: number;
  hiddenBehind: number;
  /** 서버 목록에서 내 이름을 못 찾은 상태. 트랙을 비우지 않고 관전으로 그린다. */
  isSpectating: boolean;
};

const EMPTY: VisiblePlayers = {
  players: [],
  hiddenAhead: 0,
  hiddenBehind: 0,
  isSpectating: false,
};

export const getVisiblePlayers = (players: RacingPlayer[], myName: string): VisiblePlayers => {
  if (players.length === 0) return EMPTY;

  const me = players.find((player) => player.playerName === myName);
  const myPosition = me?.position ?? 0;

  // 참가 순서로 자르면 바로 옆에서 다투는 상대가 잘리고 화면 밖 사람이 남는다. 나와의 거리로 고른다.
  const nearest = [...players]
    .sort((a, b) => Math.abs(a.position - myPosition) - Math.abs(b.position - myPosition))
    .slice(0, VISIBLE_PLAYER_COUNT);

  const shownNames = new Set(nearest.map((player) => player.playerName));
  const hidden = players.filter((player) => !shownNames.has(player.playerName));

  return {
    players: nearest.sort((a, b) => b.position - a.position),
    hiddenAhead: hidden.filter((player) => player.position > myPosition).length,
    hiddenBehind: hidden.filter((player) => player.position <= myPosition).length,
    isSpectating: me === undefined,
  };
};
