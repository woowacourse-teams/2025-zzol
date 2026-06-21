import { useNunchiGameContext } from '@/contexts/NunchiGame/NunchiGameContext';
import * as S from './NunchiKeypad.styled';

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

  return (
    <S.Keypad>
      {!isConnected && <S.Warning>연결이 끊겼습니다. 다시 연결 중...</S.Warning>}
      <S.PressButton
        type="button"
        onClick={press}
        disabled={!canPress}
        $state={myInputState}
        $connected={isConnected}
      >
        {/* 버튼 텍스트(getLabel)가 접근성 이름이 되도록 정적 aria-label 을 두지 않는다.
            정적 라벨을 두면 스크린리더가 상태("일어섰다!"/"충돌 — 탈락"/"연결 끊김")를 못 읽는다. */}
        {getLabel()}
      </S.PressButton>
    </S.Keypad>
  );
};

export default NunchiKeypad;
