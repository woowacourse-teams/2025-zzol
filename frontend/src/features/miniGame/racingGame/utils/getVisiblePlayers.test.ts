import { RacingPlayer } from '@/types/miniGame/racingGame';
import { getVisiblePlayers } from './getVisiblePlayers';

const player = (playerName: string, position: number): RacingPlayer => ({
  playerName,
  position,
  speed: 0,
});

const shown = (players: RacingPlayer[], myName: string) =>
  getVisiblePlayers(players, myName).rows.filter(({ isVisible }) => isVisible);

const slotOf = (players: RacingPlayer[], myName: string, target: string) =>
  getVisiblePlayers(players, myName).rows.find(({ player }) => player.playerName === target)?.slot;

describe('getVisiblePlayers', () => {
  const nine = [
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

  it('나는 항상 가운데 슬롯이다', () => {
    expect(slotOf(nine, '나', '나')).toBe(0);
    expect(slotOf([player('나', 10), player('a', 900)], '나', '나')).toBe(0);
  });

  it('내 위에는 앞선 사람만, 아래에는 뒤진 사람만 온다', () => {
    const rows = shown(nine, '나');
    const above = rows.filter(({ slot }) => slot < 0).map(({ player }) => player.position);
    const below = rows.filter(({ slot }) => slot > 0).map(({ player }) => player.position);

    expect(Math.min(...above)).toBeGreaterThan(600);
    expect(Math.max(...below)).toBeLessThan(600);
  });

  it('바로 앞사람이 내 바로 위, 바로 뒷사람이 내 바로 아래에 온다', () => {
    expect(slotOf(nine, '나', 'c')).toBe(-1);
    expect(slotOf(nine, '나', 'd')).toBe(1);
  });

  it('9인 방에서 7명만 보이고 잘린 인원을 센다', () => {
    const { hiddenAhead, hiddenBehind } = getVisiblePlayers(nine, '나');

    expect(shown(nine, '나')).toHaveLength(7);
    expect(hiddenAhead).toBe(0);
    expect(hiddenBehind).toBe(2);
  });

  it('내가 1등이면 아래가 더 채워지고 총 인원은 그대로다', () => {
    const leading = nine.map((p) => (p.playerName === '나' ? player('나', 1000) : p));
    const rows = shown(leading, '나');

    expect(rows).toHaveLength(7);
    expect(rows.every(({ slot }) => slot >= 0)).toBe(true);
    expect(getVisiblePlayers(leading, '나').hiddenAhead).toBe(0);
  });

  it('추월이 일어나면 두 사람의 슬롯만 바뀐다', () => {
    const after = nine.map((p) => (p.playerName === 'd' ? player('d', 650) : p));

    // d 가 나를 제쳐 내 바로 위로, 원래 바로 위였던 c 는 한 칸 올라간다. 나머지는 그대로다.
    expect(slotOf(after, '나', 'd')).toBe(-1);
    expect(slotOf(after, '나', 'c')).toBe(-2);
    expect(slotOf(after, '나', 'e')).toBe(1);
    expect(slotOf(after, '나', 'f')).toBe(2);
    expect(slotOf(after, '나', '나')).toBe(0);
  });

  it('내 이름이 목록에 없으면 트랙을 비우지 않고 관전으로 표시한다', () => {
    const players = [player('a', 300), player('b', 200)];

    const { rows, isSpectating } = getVisiblePlayers(players, '없는사람');

    expect(rows).toHaveLength(2);
    expect(isSpectating).toBe(true);
  });

  it('관전 중에 화면 밖으로 밀린 인원을 뒤쪽으로 센다', () => {
    const { rows, hiddenAhead, hiddenBehind } = getVisiblePlayers(nine, '없는사람');

    expect(rows.filter(({ isVisible }) => isVisible)).toHaveLength(7);
    expect(hiddenAhead).toBe(0);
    expect(hiddenBehind).toBe(2);
  });
});
