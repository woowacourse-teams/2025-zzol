import { PropsWithChildren, useCallback, useEffect, useRef, useState } from 'react';
import { clearAllLastStreamIds } from '@/apis/rest/recovery';
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
  const [roomSessionToken, setRoomSessionToken] = useState<string>(() => {
    return storageManager.getItem(STORAGE_KEYS.ROOM_SESSION_TOKEN, 'sessionStorage', '') as string;
  });

  const joinCodeRef = useRef(joinCode);
  const qrCodeUrlRef = useRef(qrCodeUrl);

  useEffect(() => {
    joinCodeRef.current = joinCode;
    qrCodeUrlRef.current = qrCodeUrl;
  }, [joinCode, qrCodeUrl]);

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

  useEffect(() => {
    if (roomSessionToken) {
      storageManager.setItem(STORAGE_KEYS.ROOM_SESSION_TOKEN, roomSessionToken, 'sessionStorage');
    } else {
      storageManager.removeItem(STORAGE_KEYS.ROOM_SESSION_TOKEN, 'sessionStorage');
    }
  }, [roomSessionToken]);

  const clearJoinCode = useCallback(() => {
    setJoinCode('');
  }, []);

  const clearMyName = useCallback(() => {
    setMyName('');
  }, []);

  const clearQrCodeUrl = useCallback(() => {
    setQrCodeUrl('');
  }, []);

  const clearRoomSessionToken = useCallback(() => {
    setRoomSessionToken('');
  }, []);

  const clearIdentifier = useCallback(() => {
    const currentJoinCode = joinCodeRef.current;
    const currentMyName = myNameRef.current;
    if (currentJoinCode) {
      storageManager.setItem(STORAGE_KEYS.LAST_JOIN_CODE, currentJoinCode, 'localStorage');
      if (currentMyName) {
        clearLastStreamId(currentJoinCode, currentMyName);
      }
    }
    clearAllLastStreamIds();
    clearJoinCode();
    clearMyName();
    clearQrCodeUrl();
    clearRoomSessionToken();
  }, [clearJoinCode, clearMyName, clearQrCodeUrl, clearRoomSessionToken]);

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
        roomSessionToken,
        setRoomSessionToken,
        clearRoomSessionToken,
        clearIdentifier,
      }}
    >
      {children}
    </IdentifierContext.Provider>
  );
};
