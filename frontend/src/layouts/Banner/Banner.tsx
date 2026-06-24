import { PropsWithChildren } from 'react';
import * as S from './Banner.styled';

type Props = {
  height?: string;
} & PropsWithChildren;

const Banner = ({ height = '40%', children }: Props) => {
  return <S.Container $height={height}>{children}</S.Container>;
};

export default Banner;
