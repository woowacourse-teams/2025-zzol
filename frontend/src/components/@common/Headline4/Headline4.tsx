import { COLOR_MAP, ColorKey } from '@/constants/color';
import { ComponentProps } from 'react';
import * as S from './Headline4.styled';

type Props = { color?: ColorKey } & ComponentProps<'h4'>;

const Headline4 = ({ children, color = 'gray-700' as ColorKey }: Props) => {
  const resolvedColor = COLOR_MAP[color];
  return <S.Container $color={resolvedColor}>{children}</S.Container>;
};

export default Headline4;
