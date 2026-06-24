import { PropsWithChildren } from 'react';
import * as S from './FadeInItem.styled';

type Props = {
  index?: number;
  delay?: number;
  duration?: number;
} & PropsWithChildren;

const FadeInItem = ({ index = 0, delay = 200, duration = 600, children }: Props) => {
  return (
    <S.Wrapper $index={index} $delay={delay} $duration={duration}>
      {children}
    </S.Wrapper>
  );
};

export default FadeInItem;
