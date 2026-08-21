import { PlayerProbability } from '@/types/roulette';

/**
 * 참가자별 확률을 스크린리더용 한 문장으로 만든다.
 *
 * 휠은 시각 요소라 접근성 트리에서 감춰 두고(얇은 조각은 이름을 그리지도 않는다),
 * 낭독은 이 문장이 맡는다. 대기방과 룰렛 플레이 화면이 같은 문장을 써야
 * 화면을 옮겨도 듣는 내용이 달라지지 않는다.
 */
export const describeProbabilities = (playerProbabilities: PlayerProbability[]): string => {
  if (playerProbabilities.length === 0) {
    return '현재 참여한 인원이 없습니다.';
  }

  return playerProbabilities
    .map(({ playerName, probability }) => `${playerName}님의 확률 ${probability}%`)
    .join(', ');
};
