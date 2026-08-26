import { PropsWithChildren, useCallback, useEffect, useMemo, useState } from 'react';

declare global {
  interface Window {
    __ZZOL_WORM_STORE__?: WormStore;
  }
}
import { WormGameContext } from './WormGameContext';
import { useWebSocketSubscription } from '@/apis/websocket/hooks/useWebSocketSubscription';
import { WormStore } from '@/features/miniGame/wormGame/core/wormStore';
import {
  WormDeltaMessage,
  WormGameState,
  WormGameStateMessage,
  WormSnapshotMessage,
} from '@/types/miniGame/wormGame';
import { useIdentifier } from '../Identifier/IdentifierContext';

const WormGameProvider = ({ children }: PropsWithChildren) => {
  const { joinCode, myName } = useIdentifier();
  const [wormGameState, setWormGameState] = useState<WormGameState>('DESCRIPTION');
  // 인스턴스 1개를 마운트 동안 유지 — setState 없이 lazy init 만 쓴다
  const [store] = useState(() => new WormStore(myName));

  // dev autoTest 봇이 iframe 안에서 스토어를 읽어 조향한다(devtools/wormBot). 프로덕션 번들엔 없음
  useEffect(() => {
    if (!process.env.ENABLE_DEVTOOLS) return;
    window.__ZZOL_WORM_STORE__ = store;
    return () => {
      delete window.__ZZOL_WORM_STORE__;
    };
  }, [store]);

  const handleState = useCallback(
    (data: WormGameStateMessage) => {
      setWormGameState(data.state);
      store.setPlaying(data.state === 'PLAYING');
    },
    [store]
  );
  const handleSnapshot = useCallback(
    (data: WormSnapshotMessage) => store.applySnapshot(data, performance.now()),
    [store]
  );
  const handleDelta = useCallback(
    (data: WormDeltaMessage) => store.applyDelta(data, performance.now()),
    [store]
  );

  useWebSocketSubscription(`/room/${joinCode}/worm/state`, handleState);
  // 순서 고정: 유니캐스트 큐를 델타 토픽보다 먼저 구독해야 구독 시점 스냅샷을 놓치지 않는다.
  // 백그라운드 복귀 재구독(usePageVisibility)도 같은 순서로 다시 타서 스냅샷으로 전체 복구된다.
  useWebSocketSubscription('/user/queue/worm/snapshot', handleSnapshot);
  useWebSocketSubscription(`/room/${joinCode}/worm`, handleDelta);
  useWebSocketSubscription(`/room/${joinCode}/worm/snapshot`, handleSnapshot);

  const value = useMemo(() => ({ wormGameState, store }), [wormGameState, store]);

  return <WormGameContext.Provider value={value}>{children}</WormGameContext.Provider>;
};

export default WormGameProvider;
