import { useEffect } from 'react';
import { useBlocker } from 'react-router-dom';

type Props = {
  onConfirm?: () => void;
};

export const useBackButtonConfirm = ({ onConfirm }: Props = {}) => {
  const blocker = useBlocker(({ historyAction }) => historyAction === 'POP');

  useEffect(() => {
    if (blocker.state === 'blocked') {
      const confirmed = window.confirm('정말 페이지를 나가시겠습니까?');
      if (confirmed) {
        blocker.proceed();
        if (onConfirm) onConfirm();
      } else {
        blocker.reset();
      }
    }
  }, [blocker, onConfirm]);
};
