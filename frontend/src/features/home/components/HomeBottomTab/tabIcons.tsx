import type { SVGProps } from 'react';

type IconProps = SVGProps<SVGSVGElement>;

export const HomeIcon = (props: IconProps) => (
  <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22" {...props}>
    <path d="M12 3L2 12h3v9h5v-6h4v6h5v-9h3L12 3z" />
  </svg>
);

export const RankingIcon = (props: IconProps) => (
  <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22" {...props}>
    <rect x="3" y="11" width="5" height="10" rx="1.5" />
    <rect x="9.5" y="7" width="5" height="14" rx="1.5" />
    <rect x="16" y="3" width="5" height="18" rx="1.5" />
  </svg>
);

export const MenuIcon = (props: IconProps) => (
  <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22" {...props}>
    <rect x="3" y="5" width="18" height="2.5" rx="1.25" />
    <rect x="3" y="10.75" width="18" height="2.5" rx="1.25" />
    <rect x="3" y="16.5" width="18" height="2.5" rx="1.25" />
  </svg>
);
