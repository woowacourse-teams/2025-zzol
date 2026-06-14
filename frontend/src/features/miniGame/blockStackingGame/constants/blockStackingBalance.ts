export const INITIAL_SPEED = 3.2;
export const SPEED_INCREMENT = 1.05; // 5% per floor
export const MAX_SPEED = 8.0;
export const GAME_DURATION = 20;
export const PERFECT_THRESHOLD = 3;

export const getBlockSpeed = (floor: number): number =>
  Math.min(INITIAL_SPEED * Math.pow(SPEED_INCREMENT, floor), MAX_SPEED);
