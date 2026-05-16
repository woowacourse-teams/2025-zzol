import { useCallback, useEffect, useState } from 'react';

const isDevToolsEnabled = Boolean(process.env.ENABLE_DEVTOOLS);

export const useServiceWorkerUpdate = () => {
  const [waitingWorker, setWaitingWorker] = useState<ServiceWorker | null>(null);
  const [devForced, setDevForced] = useState(false);

  useEffect(() => {
    if (!isDevToolsEnabled) return;
    const handler = (e: Event) => {
      if (e instanceof CustomEvent && typeof e.detail === 'boolean') {
        setDevForced(e.detail);
      }
    };
    window.addEventListener('dev:updateReady', handler);
    return () => window.removeEventListener('dev:updateReady', handler);
  }, []);

  useEffect(() => {
    if (!('serviceWorker' in navigator)) return;

    let cancelled = false;
    let reg: ServiceWorkerRegistration | undefined;
    let installingWorker: ServiceWorker | undefined;

    const handleStateChange = () => {
      if (installingWorker?.state === 'installed' && navigator.serviceWorker.controller) {
        setWaitingWorker(installingWorker);
      }
    };

    const handleUpdateFound = () => {
      installingWorker?.removeEventListener('statechange', handleStateChange);
      installingWorker = reg?.installing ?? undefined;
      installingWorker?.addEventListener('statechange', handleStateChange);
    };

    navigator.serviceWorker.ready
      .then((registration) => {
        if (cancelled) return;

        if (registration.waiting && navigator.serviceWorker.controller) {
          setWaitingWorker(registration.waiting);
          return;
        }
        reg = registration;
        reg.addEventListener('updatefound', handleUpdateFound);
      })
      .catch(() => {});

    return () => {
      cancelled = true;
      reg?.removeEventListener('updatefound', handleUpdateFound);
      installingWorker?.removeEventListener('statechange', handleStateChange);
    };
  }, []);

  const applyUpdate = useCallback(() => {
    if (isDevToolsEnabled && devForced) {
      window.dispatchEvent(new CustomEvent('dev:updateReady', { detail: false }));
      return;
    }
    if (!waitingWorker) return;
    window.location.reload();
  }, [waitingWorker, devForced]);

  return { updateReady: waitingWorker !== null || devForced, applyUpdate };
};
