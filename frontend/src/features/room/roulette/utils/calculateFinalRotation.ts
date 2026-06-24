import { RouletteSector } from '@/types/roulette';

type Props = {
  finalAngles: RouletteSector[];
  winner: string | null;
  randomAngle: number;
};

const MARGIN_ANGLE = 2;

export const calculateFinalRotation = ({ finalAngles, winner, randomAngle }: Props) => {
  if (!winner) return 0;
  const winnerData = finalAngles.find((player) => player.playerName === winner);
  if (!winnerData) return 0;

  const { startAngle, endAngle } = winnerData;

  if (endAngle - startAngle <= MARGIN_ANGLE * 2) {
    const centerAngle = (startAngle + endAngle) / 2;
    const finalCenterRotation = 360 - centerAngle;
    return normalize(finalCenterRotation);
  }

  const minAngle = winnerData.startAngle + MARGIN_ANGLE;
  const maxAngle = winnerData.endAngle - MARGIN_ANGLE;

  const randomAngleInRange = minAngle + (maxAngle - minAngle) * (randomAngle / 100);
  const finalRandomRotation = 360 - randomAngleInRange;

  return normalize(finalRandomRotation);
};

const normalize = (deg: number) => ((deg % 360) + 360) % 360;
