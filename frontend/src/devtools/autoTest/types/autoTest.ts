export type AutoTestLog = {
  id: string;
  timestamp: number;
  message: string;
  context: string;
  data?: unknown;
};

export type AutoTestLogger = {
  getLogs: () => AutoTestLog[];
  subscribe: (listener: (log: AutoTestLog) => void) => () => void;
  addLog: (log: Omit<AutoTestLog, 'id' | 'timestamp'>) => void;
};
