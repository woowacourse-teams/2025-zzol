import * as S from './RouletteWheelBack.styled';
import { WHEEL_CONFIG } from '@/features/roulette/constants/config';
import BreadCharacterPink from '@/assets/logo/bread-pink.png';
import { memo } from 'react';
import { theme } from '@/styles/theme';

const RouletteWheelBack = () => {
  return (
    <S.Container>
      <Pin />
      <S.Wrapper>
        <svg
          width={WHEEL_CONFIG.SIZE}
          height={WHEEL_CONFIG.SIZE}
          viewBox={`0 0 ${WHEEL_CONFIG.SIZE} ${WHEEL_CONFIG.SIZE}`}
          stroke={theme.color.point[400]}
          strokeWidth="8"
        >
          <circle
            cx={WHEEL_CONFIG.CENTER}
            cy={WHEEL_CONFIG.CENTER}
            r={WHEEL_CONFIG.RADIUS}
            fill="transparent"
          />
        </svg>
        <S.BreadCharacter src={BreadCharacterPink} alt="bread-character" />
      </S.Wrapper>
    </S.Container>
  );
};

export default RouletteWheelBack;

const Pin = memo(() => <S.Pin />);
Pin.displayName = 'Pin';
