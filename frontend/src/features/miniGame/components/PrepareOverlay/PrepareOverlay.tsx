import Headline1 from '@/components/@common/Headline1/Headline1';
import * as S from './PrepareOverlay.styled';
import chatBubble from '@/assets/chat_bubble.svg';
import coffee from '@/assets/logo/coffee-white.png';
import { useEffect, useState } from 'react';

const PREPARE_TEXT = {
  READY: 'READY',
  START: 'START!',
};

const PrepareOverlay = () => {
  const [displayText, setDisplayText] = useState<string>(PREPARE_TEXT.READY);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDisplayText(PREPARE_TEXT.START);
    }, 1000);

    return () => clearTimeout(timer);
  }, []);

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
