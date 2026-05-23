import { createContext, useContext } from 'react';

export type InstallPromptContextValue = {
  isInstallable: boolean;
  isInstalled: boolean;
  install: () => Promise<void>;
};

export const InstallPromptContext = createContext<InstallPromptContextValue | null>(null);

export const useInstallPromptContext = () => {
  const ctx = useContext(InstallPromptContext);
  if (ctx === null) {
    throw new Error('useInstallPromptContext must be used within an InstallPromptProvider');
  }
  return ctx;
};
