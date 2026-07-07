import Headline1 from '@/components/@common/Headline1/Headline1';
import * as S from './PrepareOverlay.styled';
import chatBubble from '@/assets/chat_bubble.svg';
import coffee from '@/assets/logo/coffee-white.png';
import { useEffect, useState } from 'react';

const PREPARE_TEXT = {
  READY: 'READY',
  START: 'START!',
};

export type PreparePhase = keyof typeof PREPARE_TEXT;

type Props = {
  /**
   * 외부에서 phase 를 제어할 때 전달한다(예: 눈치게임이 서버 playStartEpochMs 기준으로 READY→START 전환).
   * 생략하면 기존 동작대로 1초 후 자체 타이머로 START 로 넘어간다(카드게임).
   */
  phase?: PreparePhase;
};

const PrepareOverlay = ({ phase }: Props) => {
  const [autoPhase, setAutoPhase] = useState<PreparePhase>('READY');

  useEffect(() => {
    if (phase) return; // controlled — 자체 타이머 불필요.
    const timer = setTimeout(() => {
      setAutoPhase('START');
    }, 1000);

    return () => clearTimeout(timer);
  }, [phase]);

  const displayText = PREPARE_TEXT[phase ?? autoPhase];

  return (
    <S.Backdrop>
      <S.Content>
        <S.BubbleTextWrapper>
          <S.BubbleImage src={chatBubble} />
          <Headline1 color="white">{displayText}</Headline1>
        </S.BubbleTextWrapper>
        <S.CoffeeImage src={coffee} />
      </S.Content>
    </S.Backdrop>
  );
};

export default PrepareOverlay;
