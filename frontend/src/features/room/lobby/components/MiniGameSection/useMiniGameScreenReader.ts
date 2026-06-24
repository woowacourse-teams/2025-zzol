import { useEffect, useRef, useState } from 'react';

export const useMiniGameScreenReader = (loading: boolean, hasMiniGames: boolean) => {
  const [message, setMessage] = useState('');
  const screenReaderRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!loading && hasMiniGames) {
      setMessage('미니게임 선택 영역입니다. 원하는 미니게임을 여러 개 선택할 수 있습니다.');
    }
  }, [loading, hasMiniGames]);

  useEffect(() => {
    if (message && screenReaderRef.current) {
      screenReaderRef.current.focus();
    }
  }, [message]);

  const announceSelection = (clickedGameName: string, isSelected: boolean) => {
    setMessage(`'${clickedGameName}'이(가) ${isSelected ? '해제' : '선택'}되었습니다.`);
  };

  return { message, screenReaderRef, announceSelection };
};
