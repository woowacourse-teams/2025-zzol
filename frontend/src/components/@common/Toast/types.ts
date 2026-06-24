export type ToastType = 'success' | 'error' | 'warning' | 'info';

export type ToastOptions = {
  message: string;
  type: ToastType;
  duration?: number;
};
