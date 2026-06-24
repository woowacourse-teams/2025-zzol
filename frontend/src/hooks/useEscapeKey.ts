import { useEffect, useRef } from 'react';

type Props = {
  onEscape: () => void;
  enabled?: boolean;
};

const useEscapeKey = ({ onEscape, enabled = true }: Props) => {
  const onEscapeRef = useRef(onEscape);

  useEffect(() => {
    onEscapeRef.current = onEscape;
  }, [onEscape]);

  useEffect(() => {
    if (!enabled) return;

    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onEscapeRef.current();
      }
    };

    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [enabled]);
};

export default useEscapeKey;
