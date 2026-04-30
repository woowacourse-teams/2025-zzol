import { useCallback, useEffect, useState } from 'react';

export const useServiceWorkerUpdate = () => {
  const [waitingWorker, setWaitingWorker] = useState<ServiceWorker | null>(null);

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
      installingWorker = reg?.installing ?? undefined;
      installingWorker?.addEventListener('statechange', handleStateChange);
    };

    navigator.serviceWorker.ready.then((registration) => {
      if (cancelled) return;

      if (registration.waiting && navigator.serviceWorker.controller) {
        setWaitingWorker(registration.waiting);
        return;
      }
      reg = registration;
      reg.addEventListener('updatefound', handleUpdateFound);
    });

    return () => {
      cancelled = true;
      reg?.removeEventListener('updatefound', handleUpdateFound);
      installingWorker?.removeEventListener('statechange', handleStateChange);
    };
  }, []);

  const applyUpdate = useCallback(() => {
    if (!waitingWorker) return;
    window.location.reload();
  }, [waitingWorker]);

  return { updateReady: waitingWorker !== null, applyUpdate };
};
