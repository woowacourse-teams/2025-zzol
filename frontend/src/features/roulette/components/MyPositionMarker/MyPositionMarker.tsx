import { theme } from '@/styles/theme';
import { RouletteSector } from '@/types/roulette';
import { WHEEL_CONFIG } from '../../constants/config';
import { getCenterAngle, polarToCartesian } from '../../utils';
import * as S from './MyPositionMarker.styled';

type Props = {
  sector: RouletteSector;
  isHidden: boolean;
};

/**
 * 내 조각이 어디인지 원판 바깥에서 짚어주는 점과 짧은 선.
 *
 * 당첨 핀(회색 삼각형)과 모양이 겹치지 않아, 내 조각이 12시에 와도 둘이 구분된다.
 * SVG 안에 있어 원판과 함께 돌므로 스핀이 끝난 뒤에도 각도 보정 없이 제자리를 가리킨다.
 */
const MyPositionMarker = ({ sector, isHidden }: Props) => {
  const angle = getCenterAngle(sector.startAngle, sector.endAngle);
  const at = (r: number) =>
    polarToCartesian({ cx: WHEEL_CONFIG.CENTER, cy: WHEEL_CONFIG.CENTER, r, angle });

  const lineStart = at(WHEEL_CONFIG.MY_MARKER_LINE_INNER);
  const lineEnd = at(WHEEL_CONFIG.MY_MARKER_LINE_OUTER);
  const dot = at(WHEEL_CONFIG.MY_MARKER_DOT_CENTER);

  return (
    <S.Marker $isHidden={isHidden} aria-hidden="true">
      {/*
        선의 안쪽 끝은 조각 위에 올라간다. 조각 색은 아홉 가지라 빨강만으로는
        주황·빨강 계열 위에서 묻힌다. 흰 선을 먼저 깔아 어떤 색에서도 윤곽이 남게 한다.
      */}
      <line
        x1={lineStart.x}
        y1={lineStart.y}
        x2={lineEnd.x}
        y2={lineEnd.y}
        stroke={theme.color.white}
        strokeWidth={WHEEL_CONFIG.MY_MARKER_LINE_WIDTH + WHEEL_CONFIG.MY_MARKER_OUTLINE * 2}
        strokeLinecap="round"
      />
      <line
        x1={lineStart.x}
        y1={lineStart.y}
        x2={lineEnd.x}
        y2={lineEnd.y}
        stroke={theme.color.point[500]}
        strokeWidth={WHEEL_CONFIG.MY_MARKER_LINE_WIDTH}
        strokeLinecap="round"
      />
      <circle
        cx={dot.x}
        cy={dot.y}
        r={WHEEL_CONFIG.MY_MARKER_DOT_RADIUS}
        fill={theme.color.point[500]}
        stroke={theme.color.white}
        strokeWidth={WHEEL_CONFIG.MY_MARKER_OUTLINE * 2}
      />
    </S.Marker>
  );
};

export default MyPositionMarker;
