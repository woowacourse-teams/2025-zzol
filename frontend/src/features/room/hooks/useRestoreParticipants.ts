import useFetch from '@/apis/rest/useFetch';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useParticipants } from '@/contexts/Participants/ParticipantsContext';
import { Player } from '@/types/player';
import { useCallback, useEffect, useRef } from 'react';

/**
 * 참가자 명단(colorIndex 포함) 복구 — #1688.
 *
 * 명단은 로비 WS 구독으로만 채워져, 게임·룰렛 페이지에서 하드 리프레시·직접 진입하면 비어 있고
 * 색이 전원 0 이 된다. 방 라우트 진입 시 명단이 비어 있으면 HTTP 로 한 번만 받아 채운다.
 * 실시간 변경(입장·퇴장·레디)은 계속 로비 WS 책임이다.
 */
export const useRestoreParticipants = () => {
  const { joinCode } = useIdentifier();
  const { participants, setParticipants } = useParticipants();

  // useFetch 는 취소 가드가 없어 응답이 늦게 올 수 있다 — 그 사이 WS 로 채워진 최신 명단은 덮지 않는다
  const participantsRef = useRef(participants);
  useEffect(() => {
    participantsRef.current = participants;
  }, [participants]);
  const restore = useCallback(
    (players: Player[]) => {
      if (participantsRef.current.length === 0) setParticipants(players);
    },
    [setParticipants]
  );

  useFetch<Player[]>({
    endpoint: `/rooms/${joinCode}/players`,
    enabled: participants.length === 0 && joinCode !== '',
    errorDisplayMode: 'toast',
    onSuccess: restore,
  });
};
