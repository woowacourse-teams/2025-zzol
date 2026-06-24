import * as S from './Skeleton.styled';

type Props = {
  width?: string | number;
  height?: string | number;
  borderRadius?: string | number;
};

const Skeleton = ({ width = '100%', height = '20px', borderRadius = '4px' }: Props) => {
  return <S.Container $width={width} $height={height} $borderRadius={borderRadius} />;
};

export default Skeleton;
