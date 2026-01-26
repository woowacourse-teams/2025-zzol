import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { fetchRecoveryMessages, getLastStreamId, RecoveryMessage } from '@/apis/rest/recovery';

export const useWebSocketRecovery = () => {
  const navigate = useNavigate();
  const { joinCode, myName } = useIdentifier();

  const routeRecoveryMessage = useCallback(
    (msg: RecoveryMessage) => {
      const { destination } = msg;

      // 룰렛 화면 전환
      if (destination.includes('/roulette') && !destination.includes('/winner')) {
        console.log('🔄 복구: 룰렛 화면으로 이동');
        navigate(`/room/${joinCode}/roulette/play`, { replace: true });
        return;
      }

      // 당첨자 발표 화면
      if (destination.includes('/winner')) {
        console.log('🔄 복구: 당첨자 화면으로 이동');
        navigate(`/room/${joinCode}/roulette/result`, { replace: true });
        return;
      }

      // 필요시 다른 destination 추가
      // if (destination.includes('/game/start')) { ... }
    },
    [joinCode, navigate]
  );

  const recoverMessages = useCallback(async () => {
    if (!joinCode || !myName) {
      console.log('⚠️ 복구 스킵: joinCode 또는 myName 없음');
      return;
    }

    const lastStreamId = getLastStreamId(joinCode);
    if (!lastStreamId) {
      console.log('⚠️ 복구 스킵: lastStreamId 없음');
      return;
    }

    console.log('🔄 메시지 복구 시작:', { joinCode, myName, lastStreamId });

    const messages = await fetchRecoveryMessages(joinCode, myName, lastStreamId);

    if (messages.length === 0) {
      console.log('✅ 복구할 메시지 없음');
      return;
    }

    console.log(`🔄 복구 메시지 ${messages.length}개 처리`);

    // 메시지 순서대로 처리
    for (const msg of messages) {
      routeRecoveryMessage(msg);

      // lastStreamId 업데이트
      try {
        localStorage.setItem(`lastStreamId:${joinCode}`, msg.streamId);
      } catch {
        // ignore
      }
    }

    console.log('✅ 메시지 복구 완료');
  }, [joinCode, myName, routeRecoveryMessage]);

  return { recoverMessages };
};
