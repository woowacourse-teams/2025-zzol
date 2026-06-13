import { useCallback, useRef, useState } from 'react';

type BlockStackingSounds = {
  ensureAudioContext: () => void;
  playLand: () => void;
  playPerfect: () => void;
  playGameOver: () => void;
  playSpeedUp: () => void;
  muted: boolean;
  toggleMute: () => void;
};

type OscType = 'sine' | 'square' | 'sawtooth' | 'triangle';

const playOscillator = (
  ctx: AudioContext,
  freqStart: number,
  freqEnd: number,
  durationMs: number,
  type: OscType = 'square',
  gainValue = 0.12
) => {
  const osc = ctx.createOscillator();
  const gain = ctx.createGain();
  osc.connect(gain);
  gain.connect(ctx.destination);

  osc.type = type;
  osc.frequency.setValueAtTime(freqStart, ctx.currentTime);
  if (freqEnd !== freqStart) {
    osc.frequency.linearRampToValueAtTime(freqEnd, ctx.currentTime + durationMs / 1000);
  }

  gain.gain.setValueAtTime(gainValue, ctx.currentTime);
  gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + durationMs / 1000);

  osc.start(ctx.currentTime);
  osc.stop(ctx.currentTime + durationMs / 1000);
};

export const useBlockStackingSounds = (): BlockStackingSounds => {
  const [muted, setMuted] = useState(false);
  const toggleMute = useCallback(() => setMuted((prev) => !prev), []);

  const audioCtxRef = useRef<AudioContext | null>(null);
  const mutedRef = useRef(muted);
  mutedRef.current = muted;

  const ensureAudioContext = useCallback(() => {
    if (mutedRef.current) return;
    if (!audioCtxRef.current) {
      audioCtxRef.current = new (
        window.AudioContext ||
        (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext
      )();
    }
    if (audioCtxRef.current.state === 'suspended') {
      audioCtxRef.current.resume();
    }
  }, []);

  const getCtx = useCallback((): AudioContext | null => {
    if (mutedRef.current) return null;
    return audioCtxRef.current;
  }, []);

  const playLand = useCallback(() => {
    const ctx = getCtx();
    if (!ctx) return;
    playOscillator(ctx, 440, 880, 80, 'square', 0.1);
  }, [getCtx]);

  const playPerfect = useCallback(() => {
    const ctx = getCtx();
    if (!ctx) return;
    // C5 → E5 → G5 아르페지오
    const notes = [523, 659, 784];
    notes.forEach((freq, i) => {
      setTimeout(() => {
        if (!audioCtxRef.current) return;
        playOscillator(audioCtxRef.current, freq, freq, 120, 'sine', 0.15);
      }, i * 70);
    });
  }, [getCtx]);

  const playGameOver = useCallback(() => {
    const ctx = getCtx();
    if (!ctx) return;
    playOscillator(ctx, 300, 80, 450, 'sawtooth', 0.15);
  }, [getCtx]);

  const playSpeedUp = useCallback(() => {
    const ctx = getCtx();
    if (!ctx) return;
    playOscillator(ctx, 600, 900, 100, 'sine', 0.1);
  }, [getCtx]);

  return {
    ensureAudioContext,
    playLand,
    playPerfect,
    playGameOver,
    playSpeedUp,
    muted,
    toggleMute,
  };
};
