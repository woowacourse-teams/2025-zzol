import { useRef, useEffect, useState, DependencyList } from 'react';

type UseHeightDifferenceOptions = {
  fadeInOffset?: number;
  dependencies?: DependencyList;
};

/**
 * 컨테이너와 래퍼의 높이 차이를 계산하는 훅
 * @param fadeInOffset - FadeIn 애니메이션의 translateY 값 (기본값: 20)
 * @param dependencies - 높이 재계산을 트리거할 의존성 배열
 * @returns { containerRef, wrapperRef, heightDifference }
 */

export const useHeightDifference = (options: UseHeightDifferenceOptions = {}) => {
  const { fadeInOffset = 20, dependencies = [] } = options;
  const containerRef = useRef<HTMLDivElement>(null);
  const wrapperRef = useRef<HTMLDivElement>(null);
  const [heightDifference, setHeightDifference] = useState(0);

  useEffect(() => {
    const calculateHeightDifference = () => {
      if (containerRef.current && wrapperRef.current) {
        const containerHeight = containerRef.current.clientHeight;
        const wrapperHeight = wrapperRef.current.scrollHeight;
        const difference = wrapperHeight - containerHeight - fadeInOffset;

        setHeightDifference(difference);
      }
    };

    calculateHeightDifference();

    window.addEventListener('resize', calculateHeightDifference);

    return () => {
      window.removeEventListener('resize', calculateHeightDifference);
    };
  }, [fadeInOffset, dependencies]);

  return { containerRef, wrapperRef, heightDifference };
};
