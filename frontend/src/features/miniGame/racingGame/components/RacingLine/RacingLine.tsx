import RacingLineImg from '@/assets/racing-line.png';
import { PIXELS_PER_UNIT } from '../../constants/track';
import * as S from './RacingLine.styled';

type Props = {
  position: number;
  myPosition: number;
};

const RacingLine = ({ position, myPosition }: Props) => {
  const relativeX = (position - myPosition) * PIXELS_PER_UNIT;

  return (
    <S.Container $position={relativeX}>
      <S.Image src={RacingLineImg} />
    </S.Container>
  );
};

export default RacingLine;
