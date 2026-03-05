import useLazyFetch from '@/apis/rest/useLazyFetch';
import { getIsRecovering } from '@/apis/websocket/contexts/WebSocketProvider';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useParticipants } from '@/contexts/Participants/ParticipantsContext';
import { usePlayerType } from '@/contexts/PlayerType/PlayerTypeContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useCallback, useEffect } from 'react';

export const useRoomAccessGuard = () => {
  const { myName, joinCode } = useIdentifier();
  const { participants } = useParticipants();
  const { playerType } = usePlayerType();
  const navigate = useReplaceNavigate();

  const { execute: checkRoomExists } = useLazyFetch<{ exist: boolean }>({
    endpoint: `/rooms/check-joinCode?joinCode=${joinCode}`,
    onError: (error) => {
      console.error('방 존재 여부 체크 실패:', error);
      navigateToHome('방 존재 여부 체크 실패');
    },
  });

  const navigateToHome = useCallback(
    (reason: string) => {
      console.log(`${reason} - 홈으로 리디렉션`);
      navigate('/');
    },
    [navigate]
  );

  const validateUserExistsAndRedirect = useCallback(async () => {
    // 동기적으로 체크
    if (getIsRecovering()) {
      console.log('복구 중 - 검증 건너뜀');
      return;
    }

    if (!joinCode) {
      navigateToHome('joinCode가 없음');
      return;
    }

    if (!playerType) {
      navigateToHome('playerType이 없음');
      return;
    }

    if (!myName) {
      navigateToHome('해당 사용자 닉네임이 없음');
      return;
    }

    const response = await checkRoomExists();
    if (!response) return;
    if (!response.exist) {
      navigateToHome('방이 존재하지 않음');
      return;
    }

    if (!participants.length) {
      console.log('participants가 로드되지 않음 - 검증 건너뜀');
      return;
    }

    const currentUser = participants.find((participant) => participant.playerName === myName);

    if (!currentUser) {
      navigateToHome('사용자 정보에서 자기 자신을 찾을 수 없음');
    }
  }, [joinCode, playerType, myName, participants, navigateToHome, checkRoomExists]);

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      validateUserExistsAndRedirect();
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [validateUserExistsAndRedirect]);
};
