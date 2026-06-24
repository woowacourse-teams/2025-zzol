import { PlayerType } from '@/types/player';
import { storageManager, STORAGE_KEYS } from '@/utils/StorageManager';
import { PlayerTypeContext } from './PlayerTypeContext';
import { PropsWithChildren, useEffect, useState } from 'react';

export const PlayerTypeProvider = ({ children }: PropsWithChildren) => {
  const [playerType, setPlayerType] = useState<PlayerType | null>(() => {
    return storageManager.getItem(STORAGE_KEYS.PLAYER_TYPE, 'sessionStorage') as PlayerType | null;
  });

  useEffect(() => {
    if (playerType) {
      storageManager.setItem(STORAGE_KEYS.PLAYER_TYPE, playerType, 'sessionStorage');
    } else {
      storageManager.removeItem(STORAGE_KEYS.PLAYER_TYPE, 'sessionStorage');
    }
  }, [playerType]);

  const setGuest = () => {
    setPlayerType('GUEST');
  };

  const setHost = () => {
    setPlayerType('HOST');
  };

  return (
    <PlayerTypeContext.Provider value={{ playerType, setPlayerType, setGuest, setHost }}>
      {children}
    </PlayerTypeContext.Provider>
  );
};
