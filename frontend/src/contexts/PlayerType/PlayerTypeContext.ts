import { createContext, useContext } from 'react';
import { PlayerType } from '@/types/player';

type PlayerTypeContextType = {
  playerType: PlayerType | null;
  setPlayerType: (type: PlayerType | null) => void;
  setGuest: () => void;
  setHost: () => void;
};

export const PlayerTypeContext = createContext<PlayerTypeContextType | null>(null);

export const usePlayerType = () => {
  const context = useContext(PlayerTypeContext);
  if (!context) {
    throw new Error('usePlayerType 는 playerTypeProvider 안에서 사용해야 합니다.');
  }
  return context;
};
