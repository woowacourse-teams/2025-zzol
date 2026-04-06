import { STORAGE_KEYS, storageManager } from '@/utils/StorageManager';
import { useState } from 'react';

const MAX_RECENT = 3;

const loadFromStorage = (): string[] => {
  return storageManager.getObject<string[]>(STORAGE_KEYS.RECENT_NICKNAMES, 'localStorage') ?? [];
};

const saveToStorage = (nicknames: string[]) => {
  storageManager.setObject(STORAGE_KEYS.RECENT_NICKNAMES, nicknames, 'localStorage');
};

const useRecentNicknames = () => {
  const [recentNicknames, setRecentNicknames] = useState<string[]>(loadFromStorage);

  const addNickname = (name: string) => {
    if (!name.trim()) return;
    const deduped = [name, ...recentNicknames.filter((n) => n !== name)].slice(0, MAX_RECENT);
    setRecentNicknames(deduped);
    saveToStorage(deduped);
  };

  const removeNickname = (name: string) => {
    const updated = recentNicknames.filter((n) => n !== name);
    setRecentNicknames(updated);
    saveToStorage(updated);
  };

  return { recentNicknames, addNickname, removeNickname };
};

export default useRecentNicknames;
