/* eslint-env browser */

export const checkAlreadyHooked = (win, key) => {
  const marker = `__DEV_${key.toUpperCase()}_WRAPPED__`;
  return !!win[marker];
};

