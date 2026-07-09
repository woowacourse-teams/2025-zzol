import { useNunchiGameContext } from '@/contexts/NunchiGame/NunchiGameContext';
import * as S from './NunchiKeypad.styled';
import type { NunchiButtonTone } from './NunchiKeypad.styled';

/**
 * 눈치게임 하단 키패드(요구사항 5/9).
 *
 * 락아웃(요구사항 5):
 *  - canPress 가 true 일 때만 누를 수 있다(IDLE & PLAYING & 연결됨).
 *  - 첫 press 즉시 PRESSED → 비활성(더블탭 방지). STOOD 확정 시 "일어섰다" 표시.
 *  - 충돌자(COLLIDED)는 영구 OUT 비활성.
 *  - COLLISION_COOLDOWN 동안 비활성(재개 카운트다운은 Stage 가 표시).
 *  - WS 끊김 시 비활성 + 경고(요구사항 J).
 *
 * 큰 타겟 + touch-action:manipulation + user-select:none 으로 더블탭 줌·텍스트 선택을 막는다.
 */
const NunchiKeypad = () => {
  const { canPress, press, myInputState, gameState, isConnected } = useNunchiGameContext();

  const getLabel = () => {
    if (!isConnected) return '연결 끊김';
    if (myInputState === 'COLLIDED') return '충돌 — 탈락';
    if (myInputState === 'STOOD') return '일어섰다!';
    if (myInputState === 'PRESSED') return '대기 중...';
    if (gameState === 'COLLISION_COOLDOWN') return '잠시 후 재개';
    return '눌러!';
  };

  // 키패드 상태를 버튼 표면 톤으로 정규화한다(요구사항 1/5).
  const getTone = (): NunchiButtonTone => {
    if (!isConnected) return 'muted';
    if (myInputState === 'COLLIDED') return 'out';
    if (myInputState === 'STOOD') return 'stood';
    if (myInputState === 'PRESSED') return 'pressed';
    if (gameState === 'COLLISION_COOLDOWN') return 'muted';
    return 'active';
  };

  return (
    <S.Keypad>
      {!isConnected && <S.Warning>연결이 끊겼습니다. 다시 연결 중...</S.Warning>}
      {/* 버튼 안 텍스트는 빼고 '버튼 느낌'만 준다. 상태는 색/오버레이/게이지가 전달하고,
          스크린리더용 접근성 이름은 aria-label(getLabel)로 유지한다. */}
      <S.PressButton
        type="button"
        onClick={press}
        disabled={!canPress}
        aria-label={getLabel()}
        $tone={getTone()}
        $invite={canPress}
      />
    </S.Keypad>
  );
};

export default NunchiKeypad;
