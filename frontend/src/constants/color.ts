import { theme } from '@/styles/theme';

export const COLOR_MAP = {
  'gray-50': theme.color.gray[50],
  'gray-100': theme.color.gray[100],
  'gray-200': theme.color.gray[200],
  'gray-300': theme.color.gray[300],
  'gray-400': theme.color.gray[400],
  'gray-500': theme.color.gray[500],
  'gray-600': theme.color.gray[600],
  'gray-700': theme.color.gray[700],
  'gray-800': theme.color.gray[800],
  'gray-900': theme.color.gray[900],
  'gray-950': theme.color.gray[950],
  'point-50': theme.color.point[50],
  'point-100': theme.color.point[100],
  'point-200': theme.color.point[200],
  'point-300': theme.color.point[300],
  'point-400': theme.color.point[400],
  'point-500': theme.color.point[500],
  white: theme.color.white,
  black: theme.color.black,
  yellow: theme.color.yellow,
} as const;

export type ColorKey = keyof typeof COLOR_MAP;

export const colorList = [
  '#FF6B6B',
  '#4cafa9',
  '#85b62e',
  '#bf77f6',
  '#ffa102',
  '#5a88c8',
  '#ff8ad8',
  '#8a8b8e',
  '#1d6d4a',
] as const;

export type ColorList = (typeof colorList)[number];

export const rankColorMap: Record<number, string> = {
  1: '#FFDE65',
  2: '#E5E7EB',
  3: '#FFC8A4',
};

export type RankColorKey = keyof typeof rankColorMap;
