import { SVGProps } from 'react';

type Props = {
  size?: number;
  color?: string;
} & SVGProps<SVGSVGElement>;

const ErrorIcon = ({ size = 20, color = '#dc2626', ...rest }: Props) => {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      {...rest}
    >
      <g id="SVGRepo_bgCarrier" strokeWidth="0" />
      <g id="SVGRepo_tracerCarrier" strokeLinecap="round" strokeLinejoin="round" />
      <g id="SVGRepo_iconCarrier">
        <circle cx="12" cy="12" r="10" stroke={color} strokeWidth="1.5" />
        <path
          d="M14.5 9.50002L9.5 14.5M9.49998 9.5L14.5 14.5"
          stroke={color}
          strokeWidth="1.5"
          strokeLinecap="round"
        />
      </g>
    </svg>
  );
};

export default ErrorIcon;
