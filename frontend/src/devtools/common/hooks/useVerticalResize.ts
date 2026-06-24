import { useState, useRef, useCallback, useEffect, PointerEvent } from 'react';

/**
 * 패널의 수평(세로) 리사이즈를 관리하는 커스텀 훅입니다.
 */
export const useVerticalResize = (
  initialWidthPercent: number = 50,
  minWidthPercent: number = 20,
  maxWidthPercent: number = 80
) => {
  const [detailWidthPercent, setDetailWidthPercent] = useState(initialWidthPercent);
  const [isResizingVertical, setIsResizingVertical] = useState(false);
  const resizeStartXRef = useRef<number | null>(null);
  const resizeStartWidthPercentRef = useRef<number | null>(null);
  const contentRef = useRef<HTMLDivElement | null>(null);

  /**
   * 리사이즈 시작 핸들러 (React용)
   */
  const handleVerticalResizeStart = useCallback(
    (e: PointerEvent) => {
      e.preventDefault();
      e.stopPropagation();
      setIsResizingVertical(true);
      resizeStartXRef.current = e.clientX;
      resizeStartWidthPercentRef.current = detailWidthPercent;
      if (e.target instanceof HTMLElement) {
        e.target.setPointerCapture(e.pointerId);
      }
    },
    [detailWidthPercent]
  );

  /**
   * 리사이즈 중 (브라우저 DOM 이벤트용)
   */
  const handleVerticalResizeMove = useCallback(
    (e: globalThis.PointerEvent) => {
      if (
        !isResizingVertical ||
        resizeStartXRef.current === null ||
        resizeStartWidthPercentRef.current === null ||
        !contentRef.current
      )
        return;

      const contentWidth = contentRef.current.offsetWidth;
      const deltaX = e.clientX - resizeStartXRef.current;
      const deltaPercent = (deltaX / contentWidth) * 100;
      const newWidthPercent = resizeStartWidthPercentRef.current - deltaPercent;
      const minWidth = Math.max((300 / contentWidth) * 100, minWidthPercent);

      if (newWidthPercent >= minWidth && newWidthPercent <= maxWidthPercent) {
        setDetailWidthPercent(newWidthPercent);
      } else if (newWidthPercent < minWidth) {
        setDetailWidthPercent(minWidth);
      } else {
        setDetailWidthPercent(maxWidthPercent);
      }
    },
    [isResizingVertical, minWidthPercent, maxWidthPercent]
  );

  /**
   * 리사이즈 종료 핸들러 (브라우저 DOM 이벤트용)
   */
  const handleVerticalResizeEnd = useCallback((e: globalThis.PointerEvent) => {
    setIsResizingVertical(false);
    resizeStartXRef.current = null;
    resizeStartWidthPercentRef.current = null;
    if (e.target instanceof HTMLElement) {
      e.target.releasePointerCapture(e.pointerId);
    }
  }, []);

  /**
   * pointermove, pointerup 리스너 등록
   */
  useEffect(() => {
    if (!isResizingVertical) return;

    const onMove = (e: globalThis.PointerEvent) => handleVerticalResizeMove(e);
    const onUp = (e: globalThis.PointerEvent) => handleVerticalResizeEnd(e);

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
  }, [isResizingVertical, handleVerticalResizeMove, handleVerticalResizeEnd]);

  return {
    detailWidthPercent,
    handleVerticalResizeStart,
    isResizingVertical,
    contentRef,
  };
};
