export const CANVAS_WIDTH = 320;
export const CANVAS_HEIGHT = 540;
export const BLOCK_HEIGHT = 28;
export const BLOCK_GAP = 2;
export const INITIAL_BLOCK_WIDTH = 150;
export const INITIAL_BLOCK_X = (CANVAS_WIDTH - INITIAL_BLOCK_WIDTH) / 2; // 60
export const CURRENT_BLOCK_Y = 155; // default Y, but we will make it dynamic
export const GRAVITY = 0.4;
export const OPACITY_DECAY = 0.025;

export const BLOCK_COLORS = [
  '#FF6B6B',
  '#FF9F43',
  '#FECA57',
  '#48DBFB',
  '#1DD1A1',
  '#54A0FF',
  '#A29BFE',
  '#FD79A8',
  '#00B894',
  '#FDCB6E',
] as const;
