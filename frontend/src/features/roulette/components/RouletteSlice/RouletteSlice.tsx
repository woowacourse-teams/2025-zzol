import { RouletteSector } from '@/types/roulette';
import * as S from './RouletteSlice.styled';
import { WHEEL_CONFIG } from '../../constants/config';
import { getSliceLabel, describeArc } from '../../utils';

type Props = {
  player: RouletteSector;
  strokeColor: string;
  strokeWidth: number;
};

const RouletteSlice = ({ player, strokeColor, strokeWidth }: Props) => {
  const label = getSliceLabel({
    startAngle: player.startAngle,
    endAngle: player.endAngle,
    nameLength: player.playerName.length,
  });

  const isFullCircle = player.startAngle === 0 && player.endAngle === 360;

  return (
    <g key={player.playerName}>
      {isFullCircle ? (
        <circle
          cx={WHEEL_CONFIG.CENTER}
          cy={WHEEL_CONFIG.CENTER}
          r={WHEEL_CONFIG.RADIUS}
          fill={player.playerColor}
          stroke={strokeColor}
          strokeWidth={strokeWidth}
        />
      ) : (
        <path
          d={describeArc({
            cx: WHEEL_CONFIG.CENTER,
            cy: WHEEL_CONFIG.CENTER,
            r: WHEEL_CONFIG.RADIUS,
            startAngle: player.startAngle,
            endAngle: player.endAngle,
          })}
          fill={player.playerColor}
          stroke={strokeColor}
          strokeWidth={strokeWidth}
          strokeLinejoin="round"
        />
      )}
      {/* 조각이 너무 얇으면 label 이 null 이다 — 이름은 아래 확률 리스트가 읽어준다 */}
      {label && (
        <S.PlayerNameText
          x={label.x}
          y={label.y}
          fontSize={label.fontSize}
          transform={`rotate(${label.rotate} ${WHEEL_CONFIG.CENTER} ${WHEEL_CONFIG.CENTER})`}
          textAnchor="middle"
          dominantBaseline="middle"
        >
          {player.playerName}
        </S.PlayerNameText>
      )}
    </g>
  );
};

export default RouletteSlice;
