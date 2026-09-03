import { RacingPlayer } from '@/types/miniGame/racingGame';
import { getVisiblePlayers } from './getVisiblePlayers';

const player = (playerName: string, position: number): RacingPlayer => ({
  playerName,
  position,
  speed: 0,
});

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

    expect(visible.map(({ playerName }) => playerName)).toEqual([
      'a',
      'b',
      'c',
      '나',
      'd',
      'e',
      'f',
    ]);
    expect(hiddenAhead).toBe(0);
    expect(hiddenBehind).toBe(2);
  });

  it('참가 순서가 아니라 거리로 자른다', () => {
    // 배열 순서로 앞뒤 3명씩 자르면 바로 옆의 far1·far2 대신 멀리 있는 near 가 잘린다.
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

    expect(visible.map(({ playerName }) => playerName)).toContain('near');
    expect(visible.map(({ playerName }) => playerName)).not.toContain('far1');
  });

  it('내 이름이 목록에 없으면 트랙을 비우지 않고 관전으로 표시한다', () => {
    const players = [player('a', 300), player('b', 200)];

    const { players: visible, isSpectating } = getVisiblePlayers(players, '없는사람');

    expect(visible).toHaveLength(2);
    expect(isSpectating).toBe(true);
  });

  it('앞선 사람이 위에 오도록 내림차순으로 준다', () => {
    const players = [player('꼴찌', 100), player('나', 500), player('선두', 900)];

    const { players: visible } = getVisiblePlayers(players, '나');

    expect(visible.map(({ playerName }) => playerName)).toEqual(['선두', '나', '꼴찌']);
  });
});
