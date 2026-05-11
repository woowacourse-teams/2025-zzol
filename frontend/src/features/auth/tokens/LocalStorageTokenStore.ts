import { storageManager, STORAGE_KEYS } from '@/utils/StorageManager';
import { Tokens } from '../types';
import { TokenStore } from './TokenStore';

export class LocalStorageTokenStore implements TokenStore {
  getAccessToken(): string | null {
    return storageManager.getItem(STORAGE_KEYS.ACCESS_TOKEN, 'localStorage');
  }

  setTokens(tokens: Tokens): void {
    storageManager.setItem(STORAGE_KEYS.ACCESS_TOKEN, tokens.accessToken, 'localStorage');
  }

  clearTokens(): void {
    storageManager.removeItem(STORAGE_KEYS.ACCESS_TOKEN, 'localStorage');
  }
}
