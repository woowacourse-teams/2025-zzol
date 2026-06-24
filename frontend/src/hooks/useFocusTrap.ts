import { useEffect, useRef, RefObject } from 'react';

const focusableSelectors = [
  'button:not([disabled])',
  'a[href]',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(', ');

const useFocusTrap = (isActive: boolean): { containerRef: RefObject<HTMLDivElement | null> } => {
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!isActive || !containerRef.current) return;

    const container = containerRef.current;
    const { first, last, all } = getFocusBoundaryElements(container);

    if (all.length === 0) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key !== 'Tab') return;

      const active = document.activeElement;

      if (e.shiftKey && active === first) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && active === last) {
        e.preventDefault();
        first.focus();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    first.focus();

    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isActive]);

  return { containerRef };
};

export default useFocusTrap;

const getFocusableElements = (container: HTMLElement): HTMLElement[] =>
  Array.from(container.querySelectorAll<HTMLElement>(focusableSelectors));

const getFocusBoundaryElements = (container: HTMLElement) => {
  const all = getFocusableElements(container);
  return {
    first: all[0],
    last: all[all.length - 1],
    all,
  };
};
