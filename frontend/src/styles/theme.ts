import { DESIGN_TOKENS } from '@/constants/design';

const color = {
  point: {
    50: '#FEF2F2',
    100: '#FFE1E1',
    200: '#FFC8C9',
    300: '#FF8789',
    400: '#FD6C6E',
    500: '#F53E41',
  },

  gray: {
    50: '#F9FAFB',
    100: '#F3F4F6',
    200: '#E5E7EB',
    300: '#D1D5DC',
    400: '#99A1AF',
    500: '#6A7282',
    600: '#4A5565',
    700: '#364153',
    800: '#1E2939',
    900: '#101828',
    950: '#030712',
  },

  red: '#FF0000',
  blue: '#0066FF',
  white: '#FFFFFF',
  black: '#000000',
  yellow: '#FFDF20',
} as const;

const typography = {
  h1: {
    fontSize: DESIGN_TOKENS.typography.h1,
    fontWeight: 700,
    fontFamily:
      "'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, sans-serif",
    lineHeight: 1.4,
  },
  h2: {
    fontSize: DESIGN_TOKENS.typography.h2,
    fontWeight: 600,
    fontFamily:
      "'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, sans-serif",
    lineHeight: 1.5,
  },
  h3: {
    fontSize: DESIGN_TOKENS.typography.h3,
    fontWeight: 600,
    fontFamily:
      "'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, sans-serif",
    lineHeight: 1.6,
  },
  h4: {
    fontSize: DESIGN_TOKENS.typography.h4,
    fontWeight: 600,
    fontFamily:
      "'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, sans-serif",

    lineHeight: 1.6,
  },

  paragraph: {
    fontSize: DESIGN_TOKENS.typography.paragraph,
    fontWeight: 500,
    fontFamily:
      "'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, sans-serif",

    lineHeight: 1.6,
  },

  small: {
    fontSize: DESIGN_TOKENS.typography.small,
    fontWeight: 400,
    fontFamily:
      "'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, sans-serif",
    lineHeight: 1.6,
  },
  caption: {
    fontSize: DESIGN_TOKENS.typography.caption,
    fontWeight: 400,
    fontFamily:
      "'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, sans-serif",
    lineHeight: 1.6,
  },
} as const;

export const theme = {
  color,
  typography,
} as const;
