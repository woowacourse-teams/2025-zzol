import { useEffect, useState } from 'react';

const STORAGE_KEY = 'zzol-mock-mode';
const EVENT_KEY = 'zzol-mock-mode-change';
const isDev = process.env.NODE_ENV === 'development';

export const useMockMode = () => {
  const [enabled, setEnabled] = useState(() => {
    if (!isDev) return false;
    return localStorage.getItem(STORAGE_KEY) !== 'false';
  });

  useEffect(() => {
    const handler = (e: Event) => {
      setEnabled((e as CustomEvent<boolean>).detail);
    };
    window.addEventListener(EVENT_KEY, handler);
    return () => window.removeEventListener(EVENT_KEY, handler);
  }, []);

  const toggle = () => {
    if (!isDev) return;
    const next = !enabled;
    localStorage.setItem(STORAGE_KEY, String(next));
    setEnabled(next);
    window.dispatchEvent(new CustomEvent<boolean>(EVENT_KEY, { detail: next }));
  };

  return { mockEnabled: enabled, toggle };
};
