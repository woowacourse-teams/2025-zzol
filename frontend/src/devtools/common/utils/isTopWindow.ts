/**
 * 현재 윈도우가 최상위 윈도우인지 확인하는 유틸리티 함수입니다.
 * iframe 내부에서는 false를 반환합니다.
 *
 * @returns 최상위 윈도우인 경우 true, 그렇지 않으면 false
 */
export const isTopWindow = (): boolean => {
  if (typeof window === 'undefined') return false;
  try {
    return window.self === window.top;
  } catch {
    return false;
  }
};
