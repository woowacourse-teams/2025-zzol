import { WormStore } from '@/features/miniGame/wormGame/core/wormStore';
import { WormGameState } from '@/types/miniGame/wormGame';
import { createContext, useContext } from 'react';

type WormGameContextType = {
  /** 상태 전이만 React state — 라우팅·오버레이용 */
  wormGameState: WormGameState;
  /** 델타·궤적·예측 — React 밖 ref 스토어. rAF 루프와 조작 계층이 직접 읽고 쓴다 */
  store: WormStore;
};

export const WormGameContext = createContext<WormGameContextType | null>(null);

export const useWormGame = () => {
  const context = useContext(WormGameContext);
  if (!context) {
    throw new Error('useWormGame 은 WormGameProvider 안에서 사용해야 합니다.');
  }
  return context;
};
