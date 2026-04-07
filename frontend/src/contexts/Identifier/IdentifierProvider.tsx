import { PropsWithChildren, useCallback, useEffect, useState } from 'react';
import { storageManager, STORAGE_KEYS } from '@/utils/StorageManager';
import { IdentifierContext } from './IdentifierContext';

export const IdentifierProvider = ({ children }: PropsWithChildren) => {
  const [joinCode, setJoinCode] = useState<string>(() => {
    return storageManager.getItem(STORAGE_KEYS.JOIN_CODE, 'sessionStorage', '') as string;
  });
  const [myName, setMyName] = useState<string>(() => {
    return storageManager.getItem(STORAGE_KEYS.MY_NAME, 'sessionStorage', '') as string;
  });
  const [qrCodeUrl, setQrCodeUrl] = useState<string>(() => {
    return storageManager.getItem(STORAGE_KEYS.QR_CODE_URL, 'sessionStorage', '') as string;
  });

  useEffect(() => {
    if (joinCode) {
      storageManager.setItem(STORAGE_KEYS.JOIN_CODE, joinCode, 'sessionStorage');
    } else {
      storageManager.removeItem(STORAGE_KEYS.JOIN_CODE, 'sessionStorage');
    }
  }, [joinCode]);

  useEffect(() => {
    if (myName) {
      storageManager.setItem(STORAGE_KEYS.MY_NAME, myName, 'sessionStorage');
    } else {
      storageManager.removeItem(STORAGE_KEYS.MY_NAME, 'sessionStorage');
    }
  }, [myName]);

  useEffect(() => {
    if (qrCodeUrl) {
      storageManager.setItem(STORAGE_KEYS.QR_CODE_URL, qrCodeUrl, 'sessionStorage');
    } else {
      storageManager.removeItem(STORAGE_KEYS.QR_CODE_URL, 'sessionStorage');
    }
  }, [qrCodeUrl]);

  const clearJoinCode = useCallback(() => {
    setJoinCode('');
  }, []);

  const clearMyName = useCallback(() => {
    setMyName('');
  }, []);

  const clearQrCodeUrl = useCallback(() => {
    setQrCodeUrl('');
  }, []);

  const clearIdentifier = useCallback(() => {
    if (joinCode) {
      storageManager.setItem(STORAGE_KEYS.LAST_JOIN_CODE, joinCode, 'localStorage');
    }
    clearJoinCode();
    clearMyName();
    clearQrCodeUrl();
  }, [joinCode, clearJoinCode, clearMyName, clearQrCodeUrl]);

  return (
    <IdentifierContext.Provider
      value={{
        joinCode,
        setJoinCode,
        clearJoinCode,
        myName,
        setMyName,
        clearMyName,
        qrCodeUrl,
        setQrCodeUrl,
        clearQrCodeUrl,
        clearIdentifier,
      }}
    >
      {children}
    </IdentifierContext.Provider>
  );
};
