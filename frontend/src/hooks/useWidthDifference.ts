import { useRef, useEffect, useState, DependencyList } from 'react';

type UseWidthDifferenceOptions = {
  dependencies?: DependencyList;
};

/**
 * 컨테이너와 래퍼의 너비 차이를 계산하는 훅
 * @param dependencies - 너비 재계산을 트리거할 의존성 배열
 * @returns { containerRef, wrapperRef, widthDifference, slideDistance }
 */

export const useWidthDifference = (options: UseWidthDifferenceOptions = {}) => {
  const { dependencies = [] } = options;
  const containerRef = useRef<HTMLDivElement>(null);
  const wrapperRef = useRef<HTMLDivElement>(null);
  const [widthDifference, setWidthDifference] = useState(0);
  const [slideDistance, setSlideDistance] = useState(0);

  useEffect(() => {
    const calculateWidthDifference = () => {
      if (containerRef.current && wrapperRef.current) {
        const containerWidth = containerRef.current.clientWidth;
        const wrapperWidth = wrapperRef.current.scrollWidth;
        const difference = wrapperWidth - containerWidth;
        setWidthDifference(difference);
        setSlideDistance(difference);
      }
    };

    calculateWidthDifference();
    window.addEventListener('resize', calculateWidthDifference);

    return () => {
      window.removeEventListener('resize', calculateWidthDifference);
    };
  }, [dependencies]);

  return { containerRef, wrapperRef, widthDifference, slideDistance };
};
