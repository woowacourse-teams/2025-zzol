import { RacingGameState } from '@/types/miniGame/racingGame';

type Props = {
  isConnected: boolean;
  racingGameState: RacingGameState;
  isGoal: boolean;
};

/**
 * 지금 누른 탭이 서버로 갈 수 있는지.
 *
 * 전송과 피드백이 같은 판단을 써야 한다. 갈라지면 못 움직이는 구간에서 리플과 진동만 나가
 * 먹힌 것처럼 보인다.
 *
 * isGoal 은 완주 뒤라 더 달릴 수 없다는 뜻이다. 관전 모드(N02)가 들어오면 그 구간의 탭은
 * 시점 전환이 되므로 여기서 함께 막으면 안 된다.
 */
export const canAcceptTap = ({ isConnected, racingGameState, isGoal }: Props): boolean =>
  isConnected && racingGameState === 'PLAYING' && !isGoal;
