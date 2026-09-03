import { RacingPlayer } from '@/types/miniGame/racingGame';

const VISIBLE_PLAYER_COUNT = 7;

export type VisiblePlayers = {
  /** 화면에 세울 플레이어. 내 행이 가운데 오도록 돌려 놓은 참가 순서다. */
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

/** 배열을 shift 칸 돌린다. 이웃 관계가 그대로라 행끼리 자리를 바꾸지 않는다. */
const rotate = <T>(items: T[], shift: number): T[] => {
  const size = items.length;
  const offset = ((shift % size) + size) % size;
  return [...items.slice(offset), ...items.slice(0, offset)];
};

export const getVisiblePlayers = (players: RacingPlayer[], myName: string): VisiblePlayers => {
  if (players.length === 0) return EMPTY;

  const me = players.find((player) => player.playerName === myName);
  const myPosition = me?.position ?? 0;

  // 참가 순서로 자르면 바로 옆에서 다투는 상대가 잘리고 화면 밖 사람이 남는다. 나와의 거리로 고른다.
  const nearest = [...players]
    .sort((a, b) => Math.abs(a.position - myPosition) - Math.abs(b.position - myPosition))
    .slice(0, VISIBLE_PLAYER_COUNT);

  // 세로 순서는 위치가 아니라 참가 순서로 잡는다. 위치로 정렬하면 추월할 때마다 행이 통째로 뒤바뀐다.
  const joinOrder = new Map(players.map((player, index) => [player.playerName, index]));
  const ordered = nearest.sort(
    (a, b) => joinOrder.get(a.playerName)! - joinOrder.get(b.playerName)!
  );

  // 내 행을 세로 가운데에 고정한다. 등수가 바뀌어도 내 자리는 안 움직인다.
  const myIndex = ordered.findIndex((player) => player.playerName === myName);
  const centerIndex = Math.floor(ordered.length / 2);
  const visible = myIndex === -1 ? ordered : rotate(ordered, myIndex - centerIndex);

  const shownNames = new Set(visible.map((player) => player.playerName));
  const hidden = players.filter((player) => !shownNames.has(player.playerName));

  return {
    players: visible,
    hiddenAhead: hidden.filter((player) => player.position > myPosition).length,
    hiddenBehind: hidden.filter((player) => player.position <= myPosition).length,
    isSpectating: me === undefined,
  };
};
