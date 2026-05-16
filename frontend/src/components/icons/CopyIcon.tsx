type Props = {
  size?: number;
};

const CopyIcon = ({ size = 12 }: Props) => (
  <svg viewBox="0 0 13 13" fill="none" width={size} height={size} aria-hidden="true">
    <rect
      x="3.5"
      y="0.65"
      width="8.85"
      height="8.85"
      rx="2"
      stroke="currentColor"
      strokeWidth="1.3"
    />
    <rect
      x="0.65"
      y="3.5"
      width="8.85"
      height="8.85"
      rx="2"
      stroke="currentColor"
      strokeWidth="1.3"
    />
  </svg>
);

export default CopyIcon;
