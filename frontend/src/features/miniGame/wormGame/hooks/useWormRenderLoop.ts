import { RefObject, useEffect } from 'react';
import { WormRenderer } from '../core/wormRenderer';

const MAX_DPR = 2;

/**
 * rAF 루프 — 매 프레임 렌더러가 스토어를 읽어 캔버스에 그린다. React 리렌더와 무관.
 * 리사이즈(iOS 주소창 변동 포함)는 부모 크기로 캔버스를 다시 잡는다. DPR 은 2 로 캡.
 */
export const useWormRenderLoop = (
  canvasRef: RefObject<HTMLCanvasElement | null>,
  renderer: WormRenderer | null
) => {
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || !renderer) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let cssW = 0;
    let cssH = 0;
    const dpr = Math.min(window.devicePixelRatio || 1, MAX_DPR);
    const resize = () => {
      const parent = canvas.parentElement;
      if (!parent) return;
      cssW = parent.clientWidth;
      cssH = parent.clientHeight;
      canvas.width = Math.round(cssW * dpr);
      canvas.height = Math.round(cssH * dpr);
      canvas.style.width = `${cssW}px`;
      canvas.style.height = `${cssH}px`;
    };
    resize();
    window.addEventListener('resize', resize);

    let rafId = 0;
    const frame = (now: number) => {
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      renderer.render(ctx, cssW, cssH, now);
      rafId = requestAnimationFrame(frame);
    };
    rafId = requestAnimationFrame(frame);

    return () => {
      cancelAnimationFrame(rafId);
      window.removeEventListener('resize', resize);
    };
  }, [canvasRef, renderer]);
};
