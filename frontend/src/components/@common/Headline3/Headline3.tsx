import { COLOR_MAP, ColorKey } from '@/constants/color';
import { ComponentProps } from 'react';
import * as S from './Headline3.styled';

type Props = { color?: ColorKey } & ComponentProps<'h3'>;

const Headline3 = ({ children, color = 'gray-700' as ColorKey, ...rest }: Props) => {
  const resolvedColor = COLOR_MAP[color];
  return (
    <S.Container $color={resolvedColor} {...rest}>
      {children}
    </S.Container>
  );
};

export default Headline3;
