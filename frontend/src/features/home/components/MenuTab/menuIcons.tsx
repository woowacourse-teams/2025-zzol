// 커피 캐릭터 스타일: #4a0f0b 메인, white 디테일, 둥근 형태, 작은 타원 눈

export const PersonIcon = () => (
  <svg width="26" height="26" viewBox="0 -2 26 30" fill="none" xmlns="http://www.w3.org/2000/svg">
    {/* 커피 캐릭터 미니 버전 (내 정보) */}
    {/* 귀 */}
    <circle cx="3" cy="13" r="2.2" fill="white" />
    <circle cx="23" cy="13" r="2.2" fill="white" />
    {/* 컵 몸통 */}
    <path d="M3 13 A10 10 0 0 1 23 13 L20 27 H6 Z" fill="white" />
    {/* 커피 (내부 채움) */}
    <path d="M3 17 Q13 10 23 17 L20 27 H6 Z" fill="#4a0f0b" />
    {/* 눈 */}
    <ellipse cx="9.5" cy="12" rx="1.1" ry="1.7" fill="black" />
    <ellipse cx="16.5" cy="12" rx="1.1" ry="1.7" fill="black" />
    {/* 빨대 */}
    <rect
      x="12" y="-1" width="2.5" height="10"
      fill="white" rx="1"
      transform="rotate(25 13 4)"
    />
  </svg>
);

export const BubbleIcon = () => (
  <svg width="26" height="26" viewBox="0 0 26 26" fill="none" xmlns="http://www.w3.org/2000/svg">
    {/* 말풍선 몸통 */}
    <path
      d="M3 5 Q3 3 5 3 H21 Q23 3 23 5 V16 Q23 18 21 18 H14 L10 23 V18 H5 Q3 18 3 16 Z"
      fill="#4a0f0b"
    />
    {/* 눈 (커피 캐릭터 스타일) */}
    <ellipse cx="10" cy="10.5" rx="1.1" ry="1.7" fill="white" />
    <ellipse cx="16" cy="10.5" rx="1.1" ry="1.7" fill="white" />
    {/* 웃는 입 */}
    <path d="M10 14 Q13 16 16 14" stroke="white" strokeWidth="0.9" strokeLinecap="round" fill="none" />
  </svg>
);

export const ClipboardIcon = () => (
  <svg width="26" height="26" viewBox="0 0 26 26" fill="none" xmlns="http://www.w3.org/2000/svg">
    {/* 문서 몸통 */}
    <rect x="4" y="4" width="18" height="20" rx="3" fill="#4a0f0b" />
    {/* 클립 탭 */}
    <rect x="10" y="2" width="6" height="5" rx="2" fill="#4a0f0b" />
    <rect x="11" y="3" width="4" height="3" rx="1.5" fill="white" />
    {/* 텍스트 줄 */}
    <path d="M8 11 H18" stroke="white" strokeWidth="1.8" strokeLinecap="round" />
    <path d="M8 15 H18" stroke="white" strokeWidth="1.8" strokeLinecap="round" />
    <path d="M8 19 H14" stroke="white" strokeWidth="1.8" strokeLinecap="round" />
    {/* 별표 (업데이트 느낌) */}
    <path d="M17 18.5 L17.5 20 L19 19.5 L17.8 20.5 L18.5 22 L17 21 L15.5 22 L16.2 20.5 L15 19.5 L16.5 20 Z" fill="white" />
  </svg>
);

export const InfoIcon = () => (
  <svg width="26" height="26" viewBox="0 0 26 26" fill="none" xmlns="http://www.w3.org/2000/svg">
    {/* i 이모지 스타일 (서비스 정보) */}
    {/* 배경 동그라미 */}
    <circle cx="13" cy="13" r="11" fill="#4a0f0b" />
    {/* 'i' 점 */}
    <circle cx="13" cy="8.5" r="1.5" fill="white" />
    {/* 'i' 기둥 */}
    <rect x="11.5" y="11.5" width="3" height="7.5" rx="1.5" fill="white" />
  </svg>
);
