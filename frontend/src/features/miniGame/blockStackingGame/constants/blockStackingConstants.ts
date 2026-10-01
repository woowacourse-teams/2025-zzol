export const CANVAS_WIDTH = 320;
export const CANVAS_HEIGHT = 540;
export const BLOCK_HEIGHT = 28;
export const BLOCK_GAP = 2;
export const INITIAL_BLOCK_WIDTH = 150;
export const INITIAL_BLOCK_X = (CANVAS_WIDTH - INITIAL_BLOCK_WIDTH) / 2; // 60
export const CURRENT_BLOCK_Y = 155; // default Y, but we will make it dynamic
export const GRAVITY = 0.4;
export const OPACITY_DECAY = 0.025;

/** 야경 빌딩 캔버스 팔레트. [낮, 밤] 쌍은 하늘이 어두워지는 정도로 섞는다 */
export const NIGHT_TOWER = {
  /** 층수에 따라 낮 → 노을 → 밤 */
  sky: [
    { floor: 0, top: '#6FB3F2', bottom: '#D6ECFF' },
    { floor: 10, top: '#F58E6B', bottom: '#5B4B8A' },
    { floor: 20, top: '#0B1026', bottom: '#1B2A4A' },
  ],
  skylineSky: { top: '#0B1026', bottom: '#23305A' },
  base: '#2B3445',
  floor: '#44536B',
  crane: '#8796AD',
  window: '#FFD66B',
  windowOff: 'rgba(20,28,45,0.55)',
  farCityDay: 'rgba(60,80,110,0.35)',
  farCityNight: 'rgba(20,28,50,0.9)',
  soil: ['#6B4E36', '#1E1813'],
  grass: ['#67B45C', '#1F3A2A'],
  pebble: ['#8A6A4E', '#2A221B'],
  trunk: ['#7A5236', '#2A1E17'],
  leaves: ['#4E9A4A', '#3F8A52'],
  leavesNight: '#16301F',
  tape: '#FFD21F',
  tapeStripe: '#1B1B1B',
  me: '#F53E41',
  meSoft: '#FF8789',
  out: '#4A5565',
  alive: '#5BE3A8',
  dead: '#FF8B8B',
  warn: '#FF6B6B',
  panel: 'rgba(8,12,24,0.78)',
} as const;
