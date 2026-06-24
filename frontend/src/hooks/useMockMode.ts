import { useCallback, useEffect, useState } from 'react';

const STORAGE_KEY = 'zzol-mock-mode';
const EVENT_KEY = 'zzol-mock-mode-change';
const isDevToolsEnabled = Boolean(process.env.ENABLE_DEVTOOLS);

export const useMockMode = () => {
  const [enabled, setEnabled] = useState(() => {
    if (!isDevToolsEnabled) return false;
    return localStorage.getItem(STORAGE_KEY) !== 'false';
  });

  useEffect(() => {
    if (!isDevToolsEnabled) return;
    const handler = (e: Event) => {
      setEnabled((e as CustomEvent<boolean>).detail);
    };
    window.addEventListener(EVENT_KEY, handler);
    return () => window.removeEventListener(EVENT_KEY, handler);
  }, []);

  const toggle = useCallback(() => {
    if (!isDevToolsEnabled) return;
    setEnabled((prev) => {
      const next = !prev;
      localStorage.setItem(STORAGE_KEY, String(next));
      window.dispatchEvent(new CustomEvent<boolean>(EVENT_KEY, { detail: next }));
      return next;
    });
  }, []);

  return { mockEnabled: enabled, toggle };
};
