type Props = {
  size?: number;
};

const ErrorIcon = ({ size = 100 }: Props) => {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 48 48"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <circle cx="24" cy="24" r="22" fill="#FFE5E5" />
      <path
        d="M24 12L34 30H14L24 12Z"
        fill="#FD6C6E"
        stroke="#FD6C6E"
        strokeWidth="2"
        strokeLinejoin="round"
        rx="2"
      />
      <line
        x1="24"
        y1="17"
        x2="24"
        y2="24"
        stroke="white"
        strokeWidth="2.5"
        strokeLinecap="round"
      />
      <circle cx="24" cy="27" r="1.5" fill="white" />
    </svg>
  );
};

export default ErrorIcon;
