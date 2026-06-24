import { useContext } from 'react';
import { ToastContext } from './ToastContext';

const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast 는 ToastProvider 안에서 사용해야 합니다.');
  }
  return context;
};

export default useToast;
