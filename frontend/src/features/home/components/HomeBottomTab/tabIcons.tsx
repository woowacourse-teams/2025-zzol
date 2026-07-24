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

export const TrophyIcon = (props: IconProps) => (
  <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22" {...props}>
    <path d="M19 5h-2V3H7v2H5c-1.1 0-2 .9-2 2v1c0 2.55 1.92 4.63 4.39 4.94.63 1.5 1.98 2.63 3.61 2.96V19H7v2h10v-2h-4v-3.1c1.63-.33 2.98-1.46 3.61-2.96C19.08 12.63 21 10.55 21 8V7c0-1.1-.9-2-2-2zM5 8V7h2v3.82C5.84 10.4 5 9.3 5 8zm14 0c0 1.3-.84 2.4-2 2.82V7h2v1z" />
  </svg>
);

export const FriendsIcon = (props: IconProps) => (
  <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22" {...props}>
    <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z" />
  </svg>
);

export const MenuIcon = (props: IconProps) => (
  <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22" {...props}>
    <rect x="3" y="5" width="18" height="2.5" rx="1.25" />
    <rect x="3" y="10.75" width="18" height="2.5" rx="1.25" />
    <rect x="3" y="16.5" width="18" height="2.5" rx="1.25" />
  </svg>
);
