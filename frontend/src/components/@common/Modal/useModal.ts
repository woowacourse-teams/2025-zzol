import { useContext } from 'react';
import { ModalContext } from './ModalContext';

const useModal = () => {
  const context = useContext(ModalContext);
  if (!context) {
    throw new Error('useModal 는 ModalProvider 안에서 사용해야 합니다.');
  }
  return context;
};

export default useModal;
