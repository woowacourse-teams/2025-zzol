import { ReactElement } from 'react';
import * as S from './TopBar.styled';

type AlignType = 'start' | 'center' | 'end' | 'stretch';

type TopBarProps = {
  height?: string;
  left?: ReactElement;
  center?: ReactElement;
  right?: ReactElement;
  align?: [AlignType, AlignType, AlignType];
};

const TopBar = ({
  height = '2.6rem',
  left,
  center,
  right,
  align = ['center', 'center', 'center'],
}: TopBarProps) => {
  const [leftAlign, centerAlign, rightAlign] = align;

  return (
    <S.Container $height={height}>
      <S.LeftSection $align={leftAlign}>{left}</S.LeftSection>
      <S.CenterSection $align={centerAlign}>{center}</S.CenterSection>
      <S.RightSection $align={rightAlign}>{right}</S.RightSection>
    </S.Container>
  );
};

export default TopBar;
