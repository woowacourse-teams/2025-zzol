import { PIXELS_PER_UNIT } from '../../constants/track';
import * as S from './TrackGround.styled';

const TICK_INTERVAL = 100;
/** 화면 밖에서 눈금을 미리 만들어 둔다. 화면 반폭은 최대 215px 이라 400이면 넉넉하다. */
const TICK_RANGE = 400;

type Props = {
  myPosition: number;
  endDistance: number;
};

/**
 * 트랙 바닥과 100 단위 눈금.
 *
 * 화면에 하늘과 캐릭터뿐이라 자기가 나아가고 있는지 알 수 없었다. 결승선은 경기 후반에야
 * 화면에 들어온다. 눈금이 그 전까지의 랜드마크가 된다.
 */
const TrackGround = ({ myPosition, endDistance }: Props) => {
  const first = Math.floor((myPosition - TICK_RANGE) / TICK_INTERVAL) * TICK_INTERVAL;
  const last = myPosition + TICK_RANGE;

  const ticks: number[] = [];
  for (let distance = Math.max(0, first); distance <= last; distance += TICK_INTERVAL) {
    if (endDistance > 0 && distance > endDistance) break;
    ticks.push(distance);
  }

  return (
    <S.Ground aria-hidden="true">
      {ticks.map((distance) => (
        <S.Tick
          key={distance}
          style={{ transform: `translateX(${(distance - myPosition) * PIXELS_PER_UNIT}px)` }}
        >
          <S.TickMark />
          <S.TickLabel>{distance}</S.TickLabel>
        </S.Tick>
      ))}
    </S.Ground>
  );
};

export default TrackGround;
