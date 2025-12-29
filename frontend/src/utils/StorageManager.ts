export const STORAGE_KEYS = {
  VISITED: 'zzol-visited',
  FIRST_TIME_USER: 'zzol-first-time-user',
  JOIN_CODE: 'zzol-join-code',
  MY_NAME: 'zzol-my-name',
  QR_CODE_URL: 'zzol-qr-code-url',
  PLAYER_TYPE: 'zzol-player-type',
} as const;

type StorageType = 'localStorage' | 'sessionStorage';

export class StorageManager {
  private static instance: StorageManager;

  public static getInstance(): StorageManager {
    if (!StorageManager.instance) {
      StorageManager.instance = new StorageManager();
    }
    return StorageManager.instance;
  }

  private constructor() {}

  private getStorage(type: StorageType): Storage | null {
    try {
      if (typeof window === 'undefined') {
        console.warn('스토리지에 접근할 수 없음');
        return null;
      }
      return window[type];
    } catch (error) {
      console.error(`${type} 스토리지에 접근할 수 없음 :`, error);
      return null;
    }
  }

  public setItem(key: string, value: string, type: StorageType): boolean {
    try {
      const storage = this.getStorage(type);
      if (!storage) return false;

      storage.setItem(key, value);
      return true;
    } catch (error) {
      console.error(`값 넣기 실패 ${type}:`, error);
      return false;
    }
  }

  public getItem(
    key: string,
    type: StorageType,
    defaultValue: string | null = null
  ): string | null {
    try {
      const storage = this.getStorage(type);
      if (!storage) return defaultValue;

      return storage.getItem(key) ?? defaultValue;
    } catch (error) {
      console.error(`값 가져오기 실패 ${type}:`, error);
      return defaultValue;
    }
  }

  public removeItem(key: string, type: StorageType): boolean {
    try {
      const storage = this.getStorage(type);
      if (!storage) return false;

      storage.removeItem(key);
      return true;
    } catch (error) {
      console.error(`삭제 실패 ${type}:`, error);
      return false;
    }
  }

  public hasItem(key: string, type: StorageType): boolean {
    return this.getItem(key, type) !== null;
  }

  public clear(type: StorageType): boolean {
    try {
      const storage = this.getStorage(type);
      if (!storage) return false;

      storage.clear();
      return true;
    } catch (error) {
      console.error(`초기화 실패 ${type}:`, error);
      return false;
    }
  }

  public setObject<T>(key: string, value: T, type: StorageType): boolean {
    try {
      const jsonString = JSON.stringify(value);
      return this.setItem(key, jsonString, type);
    } catch (error) {
      console.error('json 변환 실패 :', error);
      return false;
    }
  }

  public getObject<T>(key: string, type: StorageType): T | null {
    try {
      const jsonString = this.getItem(key, type);
      if (!jsonString) return null;

      return JSON.parse(jsonString) as T;
    } catch (error) {
      console.error('json 파싱 실패 :', error);
      return null;
    }
  }
}

export const storageManager = StorageManager.getInstance();
