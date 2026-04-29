import { useCallback, useEffect, useState } from 'react';

export function useServiceWorkerUpdate() {
  const [waitingWorker, setWaitingWorker] = useState<ServiceWorker | null>(null);

  useEffect(() => {
    if (!('serviceWorker' in navigator)) return;

    navigator.serviceWorker.ready.then((registration) => {
      if (registration.waiting && navigator.serviceWorker.controller) {
        setWaitingWorker(registration.waiting);
        return;
      }

      const handleUpdateFound = () => {
        const newWorker = registration.installing;
        if (!newWorker) return;

        newWorker.addEventListener('statechange', () => {
          if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
            setWaitingWorker(newWorker);
          }
        });
      };

      registration.addEventListener('updatefound', handleUpdateFound);
      return () => registration.removeEventListener('updatefound', handleUpdateFound);
    });
  }, []);

  const applyUpdate = useCallback(() => {
    if (!waitingWorker) return;
    waitingWorker.postMessage({ type: 'SKIP_WAITING' });
    window.location.reload();
  }, [waitingWorker]);

  return { updateReady: waitingWorker !== null, applyUpdate };
}
