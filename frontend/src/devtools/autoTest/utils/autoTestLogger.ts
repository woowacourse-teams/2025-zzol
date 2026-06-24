import { AutoTestLog, AutoTestLogger } from '../types/autoTest';

declare global {
  interface Window {
    __autoTestLogger__?: AutoTestLoggerInstance;
  }
}

export class AutoTestLoggerInstance implements AutoTestLogger {
  private logs: AutoTestLog[] = [];
  private listeners: Set<(log: AutoTestLog) => void> = new Set();
  private maxLogs = 1000;

  addLog(log: Omit<AutoTestLog, 'id' | 'timestamp'>): void {
    const newLog: AutoTestLog = {
      ...log,
      id: `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
      timestamp: Date.now(),
    };

    this.logs.unshift(newLog);

    // 최대 로그 수 제한
    if (this.logs.length > this.maxLogs) {
      this.logs = this.logs.slice(0, this.maxLogs);
    }

    // 구독자에게 알림
    this.listeners.forEach((listener) => {
      try {
        listener(newLog);
      } catch (error) {
        console.error('Error in AutoTestLogger listener:', error);
      }
    });
  }

  getLogs(): AutoTestLog[] {
    return [...this.logs];
  }

  subscribe(listener: (log: AutoTestLog) => void): () => void {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  clear(): void {
    this.logs = [];
  }
}

export const getAutoTestLogger = (): AutoTestLoggerInstance => {
  if (typeof window === 'undefined') {
    throw new Error('AutoTestLogger requires browser environment');
  }

  if (!window.__autoTestLogger__) {
    window.__autoTestLogger__ = new AutoTestLoggerInstance();
  }

  return window.__autoTestLogger__;
};

export const initializeAutoTestLogger = (): void => {
  if (typeof window === 'undefined') return;
  getAutoTestLogger();
};
