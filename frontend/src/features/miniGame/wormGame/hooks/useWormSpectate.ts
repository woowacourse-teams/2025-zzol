import { useCallback, useEffect, useState } from 'react';
import { WormStore } from '../core/wormStore';

const POLL_MS = 200;

/**
 * 자기 생사·관전 대상. 스토어는 React 밖이라 생사만 저빈도로 폴링해 state 로 올린다(사망은 1회 이벤트).
 * 관전 대상이 죽으면 다음 생존자로 자동 이동, 탭하면 순환.
 */
export const useWormSpectate = (store: WormStore, enabled: boolean) => {
  const [isDead, setIsDead] = useState(false);
  const [followName, setFollowName] = useState(store.followName);

  const aliveOthers = useCallback(
    () => [...store.worms.values()].filter((w) => w.alive && w.playerName !== store.myName),
    [store]
  );

  useEffect(() => {
    if (!enabled) return;
    const timer = setInterval(() => {
      const me = store.worms.get(store.myName);
      const dead = me !== undefined && !me.alive;
      if (dead && !isDead) {
        setIsDead(true);
        navigator.vibrate?.(80);
      }
      const target = store.worms.get(store.followName);
      if (dead && (!target || !target.alive)) {
        const next = aliveOthers()[0];
        if (next) {
          store.follow(next.playerName);
          setFollowName(next.playerName);
        }
      }
    }, POLL_MS);
    return () => clearInterval(timer);
  }, [enabled, isDead, store, aliveOthers]);

  const cycleFollow = useCallback(() => {
    const others = aliveOthers();
    if (others.length === 0) return;
    const idx = others.findIndex((w) => w.playerName === store.followName);
    const next = others[(idx + 1) % others.length];
    store.follow(next.playerName);
    setFollowName(next.playerName);
  }, [aliveOthers, store]);

  return { isDead, followName, cycleFollow };
};
