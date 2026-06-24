import { COLOR_MAP, ColorKey } from '@/constants/color';
import * as S from './Divider.styled';

type Props = {
  color?: ColorKey;
  height?: string;
  width?: string;
};

const Divider = ({ color = 'point-200', height = '3px', width = '100%' }: Props) => {
  return <S.Container $color={COLOR_MAP[color]} $height={height} $width={width} />;
};

export default Divider;
