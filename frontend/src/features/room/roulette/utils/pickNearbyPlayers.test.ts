import { colorList } from '@/constants/color';
import { PlayerProbability } from '@/types/roulette';
import { pickNearbyPlayers } from './pickNearbyPlayers';

const player = (playerName: string, probability: number): PlayerProbability => ({
  playerName,
  probability,
  playerColor: colorList[0],
});

const NAMES = (players: { playerName: string }[]) => players.map((p) => p.playerName);

describe('pickNearbyPlayers', () => {
  const players = [
    player('1등', 30),
    player('2등', 20),
    player('3등', 11),
    player('나', 10),
    player('5등', 9),
    player('6등', 8),
    player('꼴등', 2),
  ];

  it('나와 확률 차이가 작은 순으로 4명까지 고른다', () => {
    const picked = pickNearbyPlayers(players, '나');

    // 차이: 3등 1, 5등 1, 6등 2, 2등 10, 꼴등 8, 1등 20
    expect(NAMES(picked)).toEqual(['3등', '나', '5등', '6등', '꼴등']);
  });

  it('화면에는 확률 내림차순으로 세운다', () => {
    const picked = pickNearbyPlayers(players, '나');

    const probabilities = picked.map((p) => p.probability);
    expect(probabilities).toEqual([...probabilities].sort((a, b) => b - a));
  });

  it('나에게는 근접 순위 0 을, 나머지에는 가까운 순으로 1부터 매긴다', () => {
    const picked = pickNearbyPlayers(players, '나');

    const rankByName = Object.fromEntries(picked.map((p) => [p.playerName, p.proximityRank]));
    expect(rankByName).toEqual({ '3등': 1, 나: 0, '5등': 2, '6등': 3, 꼴등: 4 });
  });

  it('거리가 같으면 나보다 앞선 쪽을 먼저 고른다', () => {
    // 3등(11)과 5등(9)은 나(10)로부터 거리가 똑같이 1이다
    const picked = pickNearbyPlayers(players, '나', 1);

    expect(NAMES(picked)).toEqual(['3등', '나']);
  });

  it('참가자가 모자라면 있는 만큼만 돌려준다', () => {
    const picked = pickNearbyPlayers([player('나', 50), player('상대', 50)], '나');

    expect(NAMES(picked)).toEqual(['나', '상대']);
  });

  it('개수를 줄이면 가장 가까운 쪽만 남는다', () => {
    const picked = pickNearbyPlayers(players, '나', 1);

    expect(picked).toHaveLength(2);
  });

  it('내가 목록에 없으면 빈 배열을 돌려준다', () => {
    expect(pickNearbyPlayers(players, '없는사람')).toEqual([]);
  });

  it('원본 배열을 건드리지 않는다', () => {
    const original = [...players];
    pickNearbyPlayers(players, '나');

    expect(players).toEqual(original);
  });
});
