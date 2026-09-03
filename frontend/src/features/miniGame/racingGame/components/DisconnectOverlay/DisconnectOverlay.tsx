import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { usePageVisibility } from '@/hooks/usePageVisibility';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useEffect } from 'react';
import * as S from './DisconnectOverlay.styled';

const RECONNECT_TIMEOUT_MS = 8000;

/**
 * 소켓이 끊긴 동안 덮는 화면.
 *
 * 사용자는 화면이 멈춘 이유를 알고, 인터벌은 같은 isConnected 로 전송을 건너뛴다.
 * 8초 안에 못 붙으면 로비로 보낸다. 눈치게임의 stuck 폴백과 같은 기준이다.
 */
const DisconnectOverlay = () => {
  const { joinCode } = useIdentifier();
  const navigate = useReplaceNavigate();
  const { isVisible } = usePageVisibility();

  // 백그라운드로 보내면 useWebSocketReconnection 이 소켓을 일부러 닫는다. 그건 끊김이 아니므로
  // 화면을 보고 있을 때만 센다. 안 그러면 잠깐 폰을 내려놓은 사람이 로비로 튕긴다.
  useEffect(() => {
    if (!isVisible) return;

    const timer = window.setTimeout(() => {
      navigate(`/room/${joinCode}/lobby`);
    }, RECONNECT_TIMEOUT_MS);
    return () => window.clearTimeout(timer);
  }, [isVisible, joinCode, navigate]);

  return (
    <S.Scrim role="status" aria-live="polite">
      <S.Banner>
        <S.BannerText>연결이 끊겼습니다. 다시 연결 중...</S.BannerText>
      </S.Banner>

      <S.Notice>
        <S.NoticeTitle>탭을 잠시 멈춥니다</S.NoticeTitle>
        <S.NoticeBody>
          연결이 돌아오면 그 자리에서 이어집니다. 그동안 누른 탭은 서버로 가지 않습니다.
        </S.NoticeBody>
      </S.Notice>

      {isVisible && (
        <S.Countdown>
          <S.CountdownLabel>8초 안에 안 되면 로비로</S.CountdownLabel>
          <S.CountdownTrack>
            <S.CountdownFill $durationMs={RECONNECT_TIMEOUT_MS} />
          </S.CountdownTrack>
        </S.Countdown>
      )}
    </S.Scrim>
  );
};

export default DisconnectOverlay;
