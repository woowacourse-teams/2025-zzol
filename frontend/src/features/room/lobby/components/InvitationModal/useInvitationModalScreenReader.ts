import { useEffect, useRef, useState } from 'react';

export const useInvitationModalScreenReader = (timeout = 3000) => {
  const screenReaderRef = useRef<HTMLDivElement>(null);
  const [message, setMessage] = useState<string>('');

  useEffect(() => {
    setMessage('친구 초대하기 모달입니다. QR 코드 또는 초대코드를 복사하여 친구들을 초대해보아요.');
    if (screenReaderRef.current) {
      screenReaderRef.current.focus();
    }

    const timer = setTimeout(() => {
      setMessage('');
    }, timeout);

    return () => clearTimeout(timer);
  }, [timeout]);

  return { screenReaderRef, message };
};
