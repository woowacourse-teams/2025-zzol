import { useState, useRef, useCallback, useEffect, PointerEvent } from 'react';

/**
 * 모바일에서 패널의 세로 분할(상하) 리사이즈를 관리하는 커스텀 훅입니다.
 *
 * @param initialHeightPercent - 초기 상단 패널 높이 비율 (%)
 * @param minHeightPercent - 최소 상단 패널 높이 비율 (%)
 * @param maxHeightPercent - 최대 상단 패널 높이 비율 (%)
 * @returns 상단 패널 높이 비율, 리사이즈 시작 핸들러, 리사이즈 상태
 */
export const useMobileSplitResize = (
  initialHeightPercent: number = 50,
  minHeightPercent: number = 20,
  maxHeightPercent: number = 80
) => {
  const [topHeightPercent, setTopHeightPercent] = useState(initialHeightPercent);
  const [isResizing, setIsResizing] = useState(false);
  const resizeStartYRef = useRef<number | null>(null);
  const resizeStartHeightPercentRef = useRef<number | null>(null);
  const contentRef = useRef<HTMLDivElement | null>(null);

  /**
   * 리사이즈 시작 핸들러입니다.
   */
  const handleResizeStart = useCallback(
    (e: PointerEvent) => {
      e.preventDefault();
      e.stopPropagation();
      setIsResizing(true);
      resizeStartYRef.current = e.clientY;
      resizeStartHeightPercentRef.current = topHeightPercent;
      if (e.target instanceof HTMLElement) {
        e.target.setPointerCapture(e.pointerId);
      }
    },
    [topHeightPercent]
  );

  /**
   * 리사이즈 중 핸들러입니다.
   */
  const handleResizeMove = useCallback(
    (e: globalThis.PointerEvent) => {
      if (
        !isResizing ||
        resizeStartYRef.current === null ||
        resizeStartHeightPercentRef.current === null ||
        !contentRef.current
      ) {
        return;
      }

      const contentHeight = contentRef.current.offsetHeight;
      const deltaY = e.clientY - resizeStartYRef.current;
      const deltaPercent = (deltaY / contentHeight) * 100;
      const newHeightPercent = resizeStartHeightPercentRef.current + deltaPercent;

      if (newHeightPercent >= minHeightPercent && newHeightPercent <= maxHeightPercent) {
        setTopHeightPercent(newHeightPercent);
      } else if (newHeightPercent < minHeightPercent) {
        setTopHeightPercent(minHeightPercent);
      } else if (newHeightPercent > maxHeightPercent) {
        setTopHeightPercent(maxHeightPercent);
      }
    },
    [isResizing, minHeightPercent, maxHeightPercent]
  );

  /**
   * 리사이즈 종료 핸들러입니다.
   */
  const handleResizeEnd = useCallback((e: globalThis.PointerEvent) => {
    setIsResizing(false);
    resizeStartYRef.current = null;
    resizeStartHeightPercentRef.current = null;
    if (e.target instanceof HTMLElement) {
      e.target.releasePointerCapture(e.pointerId);
    }
  }, []);

  /**
   * pointermove, pointerup 리스너 등록
   */
  useEffect(() => {
    if (!isResizing) return;

    const onMove = (e: globalThis.PointerEvent) => handleResizeMove(e);
    const onUp = (e: globalThis.PointerEvent) => handleResizeEnd(e);

    document.addEventListener('pointermove', onMove);
    document.addEventListener('pointerup', onUp);
    document.body.style.userSelect = 'none';
    document.body.style.touchAction = 'none';

    return () => {
      document.removeEventListener('pointermove', onMove);
      document.removeEventListener('pointerup', onUp);
      document.body.style.userSelect = '';
      document.body.style.touchAction = '';
    };
  }, [isResizing, handleResizeMove, handleResizeEnd]);

  return {
    topHeightPercent,
    handleResizeStart,
    isResizing,
    contentRef,
  };
};
