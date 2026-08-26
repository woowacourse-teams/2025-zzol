import useFetch from '@/apis/rest/useFetch';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useParticipants } from '@/contexts/Participants/ParticipantsContext';
import { useBackButtonConfirm } from '@/hooks/useBackButtonConfirm';
import { MiniGameType } from '@/types/miniGame/common';
import { Player } from '@/types/player';
import { PropsWithChildren } from 'react';
import { useParams } from 'react-router-dom';
import { GAME_CONFIGS } from '../config/gameConfigs';

const MiniGameProviders = ({ children }: PropsWithChildren) => {
  useBackButtonConfirm();

  // 명단(colorIndex 포함)은 로비 WS 구독으로만 채워진다. 게임 페이지 하드 리프레시·직접 진입 시엔
  // 비어 있어 색이 전원 0 이 되므로 한 번만 HTTP 로 복구한다(#1688). 실시간 변경은 로비 책임.
  const { joinCode } = useIdentifier();
  const { participants, setParticipants } = useParticipants();
  useFetch<Player[]>({
    endpoint: `/rooms/${joinCode}/players`,
    enabled: participants.length === 0 && joinCode !== '',
    errorDisplayMode: 'toast',
    onSuccess: setParticipants,
  });

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
