export const checkIsTouchDevice = (): boolean => {
  if (typeof window === 'undefined') return false;

  const hasTouchEvent = 'ontouchstart' in window;
  const hasTouchPoints = window.navigator.maxTouchPoints > 0;

  return hasTouchEvent || hasTouchPoints;
};
