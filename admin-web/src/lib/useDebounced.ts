import { useEffect, useState } from 'react';

/**
 * 값이 조용해진 뒤에만 바뀐다. 검색 입력이 타이핑마다 서버를 때리지 않게 한다.
 * 5글자 코드를 치면 디바운스 없이는 다섯 번 조회한다.
 */
export function useDebounced<T>(value: T, delayMs = 300): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);

  return debounced;
}
