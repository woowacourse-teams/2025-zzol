import { storageManager, STORAGE_KEYS } from '@/utils/StorageManager';

export const useMyStats = () => {
  const winCount = storageManager.getItem(STORAGE_KEYS.WIN_COUNT, 'localStorage', '0') ?? '0';
  const streak =
    storageManager.getItem(STORAGE_KEYS.NON_WIN_STREAK, 'localStorage', '0') ?? '0';

  return { winCount, streak };
};
