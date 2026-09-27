import { useWebSocketSubscription } from '@/apis/websocket/hooks/useWebSocketSubscription';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import {
  BlockStackingGameState,
  BlockStackingRanking,
  StackedBlock,
} from '@/types/miniGame/blockStackingGame';
import { PropsWithChildren, useCallback, useMemo, useState } from 'react';
import { BlockStackingGameContext } from './BlockStackingGameContext';
import { GAME_DURATION } from '@/features/miniGame/blockStackingGame/constants/blockStackingBalance';
import type { BlockStackingStateResponse } from '@/apis/websocket/generated/wsContract';
import {
  INITIAL_BLOCK_WIDTH,
  INITIAL_BLOCK_X,
} from '@/features/miniGame/blockStackingGame/constants/blockStackingConstants';

type Towers = Record<string, StackedBlock[]>;

const BASE_TOWER: StackedBlock[] = [{ x: INITIAL_BLOCK_X, width: INITIAL_BLOCK_WIDTH }];

/**
 * 브로드캐스트마다 각자의 빌딩에 새 층을 올린다.
 * 메시지를 놓쳐 층이 건너뛰면 사이 층은 직전 블록으로 채워 높이를 층수와 맞춘다.
 */
const growTowers = (prev: Towers, players: BlockStackingRanking[]): Towers => {
  let changed = false;
  const next = { ...prev };
  players.forEach(({ name, floor, topX, topWidth }) => {
    const tower = prev[name] ?? BASE_TOWER;
    if (tower.length - 1 >= floor) return;
    const grown = [...tower];
    while (grown.length - 1 < floor) {
      const isTop = grown.length === floor;
      grown.push(
        isTop && topX != null && topWidth != null
          ? { x: topX, width: topWidth }
          : grown[grown.length - 1]
      );
    }
    next[name] = grown;
    changed = true;
  });
  return changed ? next : prev;
};

const BlockStackingGameProvider = ({ children }: PropsWithChildren) => {
  const { joinCode } = useIdentifier();
  const [gameState, setGameState] = useState<BlockStackingGameState>('READY');
  const [rankings, setRankings] = useState<BlockStackingRanking[]>([]);
  const [towers, setTowers] = useState<Towers>({});
  const [isLocalGameOver, setIsLocalGameOver] = useState(false);
  const [endTimeEpochMs, setEndTimeEpochMs] = useState<number | null>(null);
  const [totalTimeSeconds, setTotalTimeSeconds] = useState(GAME_DURATION);

  const setLocalGameOver = useCallback(() => setIsLocalGameOver(true), []);

  useWebSocketSubscription(
    `/room/${joinCode}/block-stacking/state`,
    useCallback(({ state, endTimeEpochMs: ms }: BlockStackingStateResponse) => {
      setGameState(state);
      if (state === 'PLAYING') {
        setIsLocalGameOver(false);
        setTowers({});
        setEndTimeEpochMs(ms ?? null);
        if (ms) {
          setTotalTimeSeconds(Math.ceil((ms - Date.now()) / 1000));
        }
      }
    }, [])
  );

  useWebSocketSubscription(
    `/room/${joinCode}/block-stacking/progress`,
    useCallback(({ players }: { players: BlockStackingRanking[] }) => {
      setRankings(players);
      setTowers((prev) => growTowers(prev, players));
    }, [])
  );

  const value = useMemo(
    () => ({
      gameState,
      rankings,
      towers,
      isLocalGameOver,
      setLocalGameOver,
      endTimeEpochMs,
      totalTimeSeconds,
    }),
    [
      gameState,
      rankings,
      towers,
      isLocalGameOver,
      setLocalGameOver,
      endTimeEpochMs,
      totalTimeSeconds,
    ]
  );

  return (
    <BlockStackingGameContext.Provider value={value}>{children}</BlockStackingGameContext.Provider>
  );
};

export default BlockStackingGameProvider;
