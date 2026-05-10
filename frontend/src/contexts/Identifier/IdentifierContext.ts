import { createContext, useContext } from 'react';

type IdentifierContextType = {
  joinCode: string;
  setJoinCode: (joinCode: string) => void;
  clearJoinCode: () => void;

  myName: string;
  setMyName: (myName: string) => void;
  clearMyName: () => void;

  qrCodeUrl: string;
  setQrCodeUrl: (qrCodeUrl: string) => void;
  clearQrCodeUrl: () => void;

  roomSessionToken: string;
  setRoomSessionToken: (token: string) => void;
  clearRoomSessionToken: () => void;

  clearIdentifier: () => void;
};

export const IdentifierContext = createContext<IdentifierContextType | null>(null);

export const useIdentifier = () => {
  const context = useContext(IdentifierContext);
  if (!context) {
    throw new Error('useIdentifier 는 IdentifierProvider 안에서 사용해야 합니다.');
  }
  return context;
};
