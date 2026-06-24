import { useTheme } from '@emotion/react';
import { SVGProps } from 'react';

type Props = {
  stroke?: string;
  strokeWidth?: number;
} & SVGProps<SVGSVGElement>;

const CloseIcon = ({ stroke, strokeWidth = 2, ...rest }: Props) => {
  const theme = useTheme();
  const iconStroke = stroke ?? theme.color.gray[400];

  return (
    <svg
      width="19"
      height="19"
      viewBox="0 0 19 19"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      {...rest}
    >
      <path
        d="M14.8438 4.15625L4.15625 14.8438"
        stroke={iconStroke}
        strokeWidth={strokeWidth}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M14.8438 14.8438L4.15625 4.15625"
        stroke={iconStroke}
        strokeWidth={strokeWidth}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
};

export default CloseIcon;
