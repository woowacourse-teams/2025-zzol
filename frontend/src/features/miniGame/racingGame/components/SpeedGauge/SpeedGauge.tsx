import { useEffect, useRef, useState } from 'react';
import { MAX_SPEED, SPEED_SEGMENT_COUNT } from '../../constants/track';
import * as S from './SpeedGauge.styled';

type Props = {
  speed: number;
};

/**
 * 내 캐릭터 아래에 붙는 속도 게이지.
 *
 * 연타를 멈추면 감속하는데 그게 화면에 안 나왔다. 아이콘 회전 속도만으로는 눈으로 재기 어렵다.
 * 서버가 준 speed 를 Lerp 없이 그대로 그려 애니메이션보다 먼저 반응한다.
 */
const SpeedGauge = ({ speed }: Props) => {
  const [isSlowing, setIsSlowing] = useState(false);
  const lastSpeedRef = useRef(speed);

  useEffect(() => {
    if (speed === lastSpeedRef.current) return;
    setIsSlowing(speed < lastSpeedRef.current);
    lastSpeedRef.current = speed;
  }, [speed]);

  const filledCount = Math.round((Math.min(speed, MAX_SPEED) / MAX_SPEED) * SPEED_SEGMENT_COUNT);

  return (
    <S.Container aria-hidden="true">
      <S.Bars>
        {Array.from({ length: SPEED_SEGMENT_COUNT }, (_, index) => (
          <S.Segment key={index} $isFilled={index < filledCount} $isSlowing={isSlowing} />
        ))}
        <S.Arrow $isSlowing={isSlowing} />
      </S.Bars>
      <S.Readout $isSlowing={isSlowing}>
        {Math.round(speed)} / {MAX_SPEED}
      </S.Readout>
    </S.Container>
  );
};

export default SpeedGauge;
