import { ReactNode } from 'react';
import * as S from './Flip.styled';

type Props = {
  flipped: boolean;
  initialView: ReactNode;
  flippedView: ReactNode;
  width?: string;
  height?: string;
  duration?: number;
};

const Flip = ({ flipped, initialView, flippedView, width, height, duration }: Props) => {
  return (
    <S.FlipWrapper $width={width} $height={height}>
      <S.Flipper flipped={flipped} $duration={duration}>
        <S.InitialView>{initialView}</S.InitialView>
        <S.FlippedView>{flippedView}</S.FlippedView>
      </S.Flipper>
    </S.FlipWrapper>
  );
};

export default Flip;
