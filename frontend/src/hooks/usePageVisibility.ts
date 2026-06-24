import { useSyncExternalStore } from 'react';

let isVisible = !document.hidden;
let listeners = new Set<() => void>();

const notifyListeners = () => {
  listeners.forEach((listener) => listener());
};

const handleVisibilityChange = () => {
  const visible = !document.hidden;

  if (visible) {
    console.log('📱 앱이 포그라운드로 전환됨');
  } else {
    console.log('📱 앱이 백그라운드로 전환됨');
  }

  isVisible = visible;
  notifyListeners();
};

document.addEventListener('visibilitychange', handleVisibilityChange);

const getSnapshot = () => {
  return isVisible;
};

const subscribe = (callback: () => void) => {
  listeners.add(callback);

  return () => {
    listeners.delete(callback);
  };
};

export const usePageVisibility = () => {
  const visible = useSyncExternalStore(subscribe, getSnapshot);
  return { isVisible: visible };
};
