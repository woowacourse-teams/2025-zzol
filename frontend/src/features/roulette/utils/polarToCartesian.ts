type Props = {
  cx: number;
  cy: number;
  r: number;
  angle: number;
};

export const polarToCartesian = ({ cx, cy, r, angle }: Props) => {
  const rad = (angle - 90) * (Math.PI / 180); // 12시 방향이 0도 기준
  return {
    x: cx + r * Math.cos(rad),
    y: cy + r * Math.sin(rad),
  };
};
