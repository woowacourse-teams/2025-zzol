import { RacingPlayer } from '@/types/miniGame/racingGame';

const VISIBLE_PLAYER_COUNT = 7;
const HALF_COUNT = 3;

export type VisibleRow = {
  player: RacingPlayer;
  /** 나를 0으로 둔 세로 슬롯. 음수가 위, 양수가 아래다. */
  slot: number;
  /** 잘린 사람은 가장자리 밖 슬롯에 숨긴다. 언마운트하지 않아 들어오고 나가는 게 애니메이션이 된다. */
  isVisible: boolean;
};

export type VisiblePlayers = {
  /** 서버가 준 전원. 슬롯 오름차순이라 위에서 아래 순이다. */
  rows: VisibleRow[];
  /** 잘려서 안 보이는 인원. 나보다 앞선 쪽과 뒤진 쪽을 나눠 센다. */
  hiddenAhead: number;
  hiddenBehind: number;
  /** 서버 목록에서 내 이름을 못 찾은 상태. 트랙을 비우지 않고 관전으로 그린다. */
  isSpectating: boolean;
};

const EMPTY: VisiblePlayers = {
  rows: [],
  hiddenAhead: 0,
  hiddenBehind: 0,
  isSpectating: false,
};

/** 내 이름이 없을 때. 나를 가운데 둘 수 없으니 앞선 사람부터 슬롯을 나눠 목록을 화면에 세운다. */
const spectateRows = (players: RacingPlayer[]): VisibleRow[] => {
  const ordered = [...players].sort((a, b) => b.position - a.position);
  const center = Math.floor(Math.min(ordered.length, VISIBLE_PLAYER_COUNT) / 2);

  return ordered.map((player, index) => ({
    player,
    slot: index - center,
    isVisible: index < VISIBLE_PLAYER_COUNT,
  }));
};

export const getVisiblePlayers = (players: RacingPlayer[], myName: string): VisiblePlayers => {
  if (players.length === 0) return EMPTY;

  const me = players.find((player) => player.playerName === myName);
  if (me === undefined) {
    return { rows: spectateRows(players), hiddenAhead: 0, hiddenBehind: 0, isSpectating: true };
  }

  const others = players.filter((player) => player.playerName !== myName);
  // 가까운 순으로 줄 세운다. 바로 앞사람이 내 바로 위, 바로 뒷사람이 내 바로 아래에 온다.
  const ahead = others
    .filter((player) => player.position > me.position)
    .sort((a, b) => a.position - b.position);
  const behind = others
    .filter((player) => player.position <= me.position)
    .sort((a, b) => b.position - a.position);

  // 한쪽이 모자라면 반대쪽을 더 채워 7명을 유지한다.
  // ponytail: 한쪽으로 몰리면 ±3 밖 슬롯이 짧은 화면에서 트랙 밖으로 나간다. 줄 간격을 줄여야 하면 그때 손본다.
  const aheadShown = Math.min(ahead.length, HALF_COUNT + Math.max(0, HALF_COUNT - behind.length));
  const behindShown = Math.min(behind.length, HALF_COUNT + Math.max(0, HALF_COUNT - ahead.length));

  const rows: VisibleRow[] = [
    ...ahead.map((player, index) => ({
      player,
      slot: -(index + 1),
      isVisible: index < aheadShown,
    })),
    { player: me, slot: 0, isVisible: true },
    ...behind.map((player, index) => ({
      player,
      slot: index + 1,
      isVisible: index < behindShown,
    })),
  ].sort((a, b) => a.slot - b.slot);

  return {
    rows,
    hiddenAhead: ahead.length - aheadShown,
    hiddenBehind: behind.length - behindShown,
    isSpectating: false,
  };
};
