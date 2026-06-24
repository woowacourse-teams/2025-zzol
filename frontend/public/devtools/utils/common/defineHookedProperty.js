/* eslint-env browser */

/**
 * window 객체의 속성을 영구적으로 훅킹합니다.
 * 원본 descriptor를 보존하여 안전하게 속성을 교체합니다.
 * 이 훅은 영구 감시 목적이므로 복원 기능을 제공하지 않습니다.
 */
export const defineHookedProperty = (win, key, newValue) => {
  const originalDescriptor = Object.getOwnPropertyDescriptor(win, key) || {
    configurable: true,
    enumerable: false,
    writable: true,
    value: win[key],
  };

  Object.defineProperty(win, key, {
    configurable: true,
    enumerable: originalDescriptor.enumerable,
    writable: originalDescriptor.writable,
    value: newValue,
  });

  // 마커 설정 (중복 훅킹 방지용)
  const marker = `__DEV_${key.toUpperCase()}_WRAPPED__`;
  win[marker] = true;
};
