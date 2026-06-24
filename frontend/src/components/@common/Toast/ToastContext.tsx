import { createContext, PropsWithChildren, useCallback, useEffect, useRef, useState } from 'react';
import Toast from './Toast';
import { ToastOptions, ToastType } from './types';

type ToastContextType = {
  showToast: (options: ToastOptions) => void;
};

export const ToastContext = createContext<ToastContextType | null>(null);

export const ToastProvider = ({ children }: PropsWithChildren) => {
  const [message, setMessage] = useState<string>('');
  const [type, setType] = useState<ToastType>('info');
  const [isExiting, setIsExiting] = useState(false);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const exitTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const clearTimer = useCallback(() => {
    if (timer.current) {
      clearTimeout(timer.current);
      timer.current = null;
    }
    if (exitTimer.current) {
      clearTimeout(exitTimer.current);
      exitTimer.current = null;
    }
  }, []);

  const resetToastState = useCallback(() => {
    setMessage('');
    setIsExiting(false);
    timer.current = null;
    exitTimer.current = null;
  }, []);

  const displayToast = useCallback((message: string, type: ToastType) => {
    setIsExiting(false);
    setMessage(message);
    setType(type);
  }, []);

  const setupExitTimer = useCallback(
    (duration: number) => {
      timer.current = setTimeout(() => {
        setIsExiting(true);

        exitTimer.current = setTimeout(() => {
          resetToastState();
        }, 300);
      }, duration);
    },
    [resetToastState]
  );

  const showToast = useCallback(
    ({ message, type = 'info', duration = 1200 }: ToastOptions) => {
      clearTimer();
      displayToast(message, type);
      setupExitTimer(duration);
    },
    [clearTimer, displayToast, setupExitTimer]
  );

  useEffect(() => {
    return () => clearTimer();
  }, [clearTimer]);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      {!!message && <Toast message={message} type={type} isExiting={isExiting} />}
    </ToastContext.Provider>
  );
};
