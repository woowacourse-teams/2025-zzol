import { useBackButtonConfirm } from '@/hooks/useBackButtonConfirm';
import { MiniGameType } from '@/types/miniGame/common';
import { PropsWithChildren } from 'react';
import { useParams } from 'react-router-dom';
import { GAME_CONFIGS } from '../config/gameConfigs';

const MiniGameProviders = ({ children }: PropsWithChildren) => {
  useBackButtonConfirm();

  const { miniGameType } = useParams();
  if (!miniGameType || !(miniGameType in GAME_CONFIGS)) {
    return <>{children}</>;
  }

  const ProviderComponent = GAME_CONFIGS[miniGameType as MiniGameType].Provider;
  if (!ProviderComponent) {
    return <>{children}</>;
  }

  return <ProviderComponent>{children}</ProviderComponent>;
};

export default MiniGameProviders;
