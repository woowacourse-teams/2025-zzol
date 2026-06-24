import { polarToCartesian } from './index';

type Props = {
  cx: number;
  cy: number;
  r: number;
  startAngle: number;
  endAngle: number;
};

export const describeArc = ({ cx, cy, r, startAngle, endAngle }: Props) => {
  const start = polarToCartesian({ cx, cy, r, angle: startAngle });
  const end = polarToCartesian({ cx, cy, r, angle: endAngle });

  const largeArcFlag = endAngle - startAngle <= 180 ? '0' : '1';

  return [
    `M ${cx} ${cy}`,
    `L ${start.x} ${start.y}`,
    `A ${r} ${r} 0 ${largeArcFlag} 1 ${end.x} ${end.y}`,
    'Z',
  ].join(' ');
};
