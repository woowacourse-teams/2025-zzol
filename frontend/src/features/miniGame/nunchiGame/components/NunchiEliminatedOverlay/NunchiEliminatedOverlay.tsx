import { useRef } from 'react';
import { useNunchiGameContext } from '@/contexts/NunchiGame/NunchiGameContext';
import * as S from './NunchiEliminatedOverlay.styled';

/**
 * 눈치게임 탈락 오버레이(요구사항 3 — BlockStacking EliminatedOverlay 패턴).
 *
 * 내가 충돌(COLLIDED)하면 Play 화면 전체를 덮어 "탈락" 을 분명히 보여준다.
 * 충돌은 같은 숫자에 둘 이상이 동시에 일어선 것이므로, 그 숫자(currentNumber)를 함께 알린다.
 * 충돌하지 않았으면 아무것도 렌더하지 않는다(생존자는 무대가 계속 보여야 함).
 */
const NunchiEliminatedOverlay = () => {
  const { myInputState, currentNumber } = useNunchiGameContext();

  // 충돌 시점의 숫자를 고정한다 — 생존자가 이어가면 currentNumber 가 뒤에서 바뀌므로.
  const collisionNumberRef = useRef<number | null>(null);
  if (myInputState === 'COLLIDED' && collisionNumberRef.current === null) {
    collisionNumberRef.current = currentNumber;
  }

  if (myInputState !== 'COLLIDED') return null;

  const collisionNumber = collisionNumberRef.current ?? currentNumber;

  return (
    <S.Backdrop role="alert">
      <S.Stamp>탈락</S.Stamp>
      <S.Message>
        <S.Headline>{collisionNumber}에서 같이 일어섰어요</S.Headline>
        <S.Sub>결과 화면에서 다시 만나요</S.Sub>
      </S.Message>
    </S.Backdrop>
  );
};

export default NunchiEliminatedOverlay;
