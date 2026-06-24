import { COLOR_MAP, ColorKey } from '@/constants/color';
import { ComponentProps } from 'react';
import * as S from './Headline1.styled';

type Props = { color?: ColorKey } & ComponentProps<'h1'>;

const Headline1 = ({ children, color = 'gray-700' as ColorKey }: Props) => {
  const resolvedColor = COLOR_MAP[color];
  return <S.Container $color={resolvedColor}>{children}</S.Container>;
};

export default Headline1;
