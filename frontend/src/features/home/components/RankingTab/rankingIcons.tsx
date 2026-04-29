export const TrophyIcon = () => (
  <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M6 2h8v7a4 4 0 0 1-8 0V2z" fill="#FBBF24" />
    <path d="M6 5H4a2 2 0 1 0 0 4h2" stroke="#FBBF24" strokeWidth="1.5" strokeLinecap="round" />
    <path d="M14 5h2a2 2 0 1 1 0 4h-2" stroke="#FBBF24" strokeWidth="1.5" strokeLinecap="round" />
    <rect x="9" y="9" width="2" height="4" fill="#FBBF24" />
    <rect x="6" y="13" width="8" height="2.5" rx="1.25" fill="#FBBF24" />
    <path
      d="M10 4.5 l.6 1.8H12.4l-1.5 1.1.6 1.8L10 8.1l-1.5 1.1.6-1.8L7.6 6.3h1.8z"
      fill="white"
      opacity="0.7"
    />
  </svg>
);

export const SkullIcon = () => (
  <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
    {/* 두개골 */}
    <path
      d="M10 2C6.13 2 3 5.13 3 9c0 2.3 1.1 4.34 2.8 5.64V16a1 1 0 0 0 1 1h6.4a1 1 0 0 0 1-1v-1.36A6.99 6.99 0 0 0 17 9c0-3.87-3.13-7-7-7z"
      fill="#6B7280"
    />
    {/* 눈 */}
    <circle cx="7.5" cy="9" r="1.8" fill="#1F2937" />
    <circle cx="12.5" cy="9" r="1.8" fill="#1F2937" />
    {/* 코 */}
    <path
      d="M9.5 11.5 L10 12.5 L10.5 11.5"
      stroke="#1F2937"
      strokeWidth="0.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
    {/* 이빨 */}
    <rect x="6.5" y="16" width="2" height="2" rx="0.3" fill="#1F2937" />
    <rect x="9" y="16" width="2" height="2" rx="0.3" fill="#1F2937" />
    <rect x="11.5" y="16" width="2" height="2" rx="0.3" fill="#1F2937" />
  </svg>
);

export const BlocksIcon = () => (
  <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect x="3" y="14" width="14" height="4" rx="1.5" fill="#8B5CF6" />
    <rect x="5" y="9" width="10" height="4" rx="1.5" fill="#A78BFA" />
    <rect x="7" y="4" width="6" height="4" rx="1.5" fill="#C4B5FD" />
    <rect x="9" y="2" width="2" height="2" rx="1" fill="#DDD6FE" />
  </svg>
);

export const GamepadIcon = () => (
  <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect x="1.5" y="6" width="17" height="10" rx="5" fill="#6B7280" />
    <path d="M6 10.5h3M7.5 9v3" stroke="white" strokeWidth="1.5" strokeLinecap="round" />
    <circle cx="13" cy="9.5" r="1" fill="white" />
    <circle cx="15" cy="11.5" r="1" fill="white" />
    <circle cx="4.5" cy="14" r="1" fill="#4B5563" />
    <circle cx="15.5" cy="14" r="1" fill="#4B5563" />
  </svg>
);

export const RacingCarIcon = () => (
  <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
    {/* 차체 */}
    <path
      d="M10 1C7.5 1 5.5 2.5 5 4L4 6H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h1l1 2c.5 1.5 2.5 3 5 3s4.5-1.5 5-3l1-2h1a1 1 0 0 0 1-1V7a1 1 0 0 0-1-1h-1l-1-2c-.5-1.5-2.5-3-5-3z"
      fill="#EF4444"
    />
    {/* 바퀴 */}
    <rect x="2" y="6" width="3" height="5" rx="1.5" fill="#1F2937" />
    <rect x="15" y="6" width="3" height="5" rx="1.5" fill="#1F2937" />
    <rect x="2.5" y="7" width="2" height="3" rx="1" fill="#374151" />
    <rect x="15.5" y="7" width="2" height="3" rx="1" fill="#374151" />
    {/* 앞 유리 */}
    <ellipse cx="10" cy="6" rx="3" ry="1.8" fill="#93C5FD" opacity="0.8" />
    {/* 레이싱 스트라이프 */}
    <path d="M9 9h2M9 11h2" stroke="white" strokeWidth="1" strokeLinecap="round" opacity="0.5" />
  </svg>
);
