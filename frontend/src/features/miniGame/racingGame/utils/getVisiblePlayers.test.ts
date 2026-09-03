import { RacingPlayer } from '@/types/miniGame/racingGame';
import { getVisiblePlayers } from './getVisiblePlayers';

const player = (playerName: string, position: number): RacingPlayer => ({
  playerName,
  position,
  speed: 0,
});

const names = (players: RacingPlayer[]) => players.map(({ playerName }) => playerName);

describe('getVisiblePlayers', () => {
  it('9인 방에서 나와 가까운 7명을 남긴다', () => {
    const players = [
      player('a', 900),
      player('b', 800),
      player('c', 700),
      player('나', 600),
      player('d', 500),
      player('e', 400),
      player('f', 300),
      player('g', 200),
      player('h', 100),
    ];

    const { players: visible, hiddenAhead, hiddenBehind } = getVisiblePlayers(players, '나');

    expect(names(visible).sort()).toEqual(['a', 'b', 'c', 'd', 'e', 'f', '나']);
    expect(hiddenAhead).toBe(0);
    expect(hiddenBehind).toBe(2);
  });

  it('참가 순서가 아니라 거리로 자른다', () => {
    // 배열 순서로 앞뒤 3명씩 자르면 바로 옆의 near 가 잘리고 멀리 있는 far 가 남는다.
    const players = [
      player('나', 1000),
      player('far1', 0),
      player('far2', 10),
      player('far3', 20),
      player('far4', 30),
      player('far5', 40),
      player('far6', 50),
      player('near', 990),
    ];

    const { players: visible } = getVisiblePlayers(players, '나');

    expect(names(visible)).toContain('near');
    expect(names(visible)).not.toContain('far1');
  });

  it('추월이 일어나도 행 순서가 바뀌지 않는다', () => {
    const before = [player('나', 100), player('x', 200), player('y', 50)];
    // 내가 x 를 제치고 선두가 됐다. 보이는 사람은 그대로다.
    const after = [player('나', 300), player('x', 200), player('y', 50)];

    expect(names(getVisiblePlayers(after, '나').players)).toEqual(
      names(getVisiblePlayers(before, '나').players)
    );
  });

  it('내 행을 세로 가운데에 둔다', () => {
    const players = [
      player('a', 900),
      player('b', 800),
      player('나', 700),
      player('c', 600),
      player('d', 500),
    ];

    const { players: visible } = getVisiblePlayers(players, '나');

    expect(visible[Math.floor(visible.length / 2)].playerName).toBe('나');
  });

  it('내 등수가 바뀌어도 내 자리는 가운데 그대로다', () => {
    const players = [player('a', 900), player('b', 800), player('나', 700)];
    const overtaken = [player('a', 900), player('b', 800), player('나', 1000)];

    const center = (list: RacingPlayer[]) => list[Math.floor(list.length / 2)].playerName;

    expect(center(getVisiblePlayers(players, '나').players)).toBe('나');
    expect(center(getVisiblePlayers(overtaken, '나').players)).toBe('나');
  });

  it('내 이름이 목록에 없으면 트랙을 비우지 않고 관전으로 표시한다', () => {
    const players = [player('a', 300), player('b', 200)];

    const { players: visible, isSpectating } = getVisiblePlayers(players, '없는사람');

    expect(visible).toHaveLength(2);
    expect(isSpectating).toBe(true);
  });
});
