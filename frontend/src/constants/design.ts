import { responsiveWidth, responsiveHeight } from '@/utils/responsive';

export const DESIGN_TOKENS = {
  // 타이포그래피
  typography: {
    h1: responsiveWidth(24, 30),
    h2: responsiveWidth(20, 24),
    h3: responsiveWidth(18, 20),
    h4: responsiveWidth(14, 16),
    paragraph: responsiveWidth(14, 16),
    small: responsiveWidth(12, 14),
    caption: responsiveWidth(11, 12),
  },

  // 카드
  card: {
    small: {
      width: responsiveWidth(40, 48),
      height: responsiveHeight(45, 61),
    },
    medium: {
      width: responsiveWidth(54, 64),
      height: responsiveHeight(60, 82),
    },
    large: {
      width: responsiveWidth(80, 96),
      height: responsiveHeight(95, 123),
    },
  },

  // 원형
  circle: {
    small: responsiveWidth(29, 34),
    medium: responsiveWidth(38, 46),
    large: responsiveWidth(57, 69),
  },
} as const;
