import { RouletteSector } from '@/types/roulette';
import * as S from './RouletteSlice.styled';
import { WHEEL_CONFIG } from '../../constants/config';
import { getCenterAngle, getTextPosition, describeArc } from '../../utils';

type Props = {
  player: RouletteSector;
  strokeColor: string;
  isGlowing?: boolean;
};

const RouletteSlice = ({ player, strokeColor, isGlowing = false }: Props) => {
  const centerAngle = getCenterAngle(player.startAngle, player.endAngle);
  const textPosition = getTextPosition(centerAngle);

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
          strokeWidth={WHEEL_CONFIG.STROKE_WIDTH}
          filter={isGlowing ? 'url(#glow)' : undefined}
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
          strokeWidth={WHEEL_CONFIG.STROKE_WIDTH}
          filter={isGlowing ? 'url(#glow)' : undefined}
        />
      )}
      <S.PlayerNameText
        x={textPosition.x}
        y={textPosition.y}
        textAnchor="middle"
        dominantBaseline="middle"
      >
        {player.playerName}
      </S.PlayerNameText>
    </g>
  );
};

export default RouletteSlice;
