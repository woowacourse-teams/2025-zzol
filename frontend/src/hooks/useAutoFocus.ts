import { useEffect, useRef } from 'react';

const useAutoFocus = <T extends HTMLElement = HTMLDivElement>() => {
  const ref = useRef<T>(null);

  useEffect(() => {
    if (ref.current) {
      ref.current.focus();
    }
  }, []);

  return ref;
};

export default useAutoFocus;
