import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useEffect } from 'react';
import * as S from './TrackNotice.styled';

/** 시안은 "몇 초"라고만 적혀 있다. 다른 폴백과 같은 8초로 맞춘다. */
const ROSTER_TIMEOUT_MS = 8000;

/**
 * 서버 목록에서 내 이름을 못 찾았을 때 띄운다.
 *
 * 빈 배열을 돌려주면 트랙이 통째로 비는데 순위표와 진행바는 정상 동작해 더 혼란스럽다.
 */
const TrackNotice = () => {
  const { joinCode } = useIdentifier();
  const navigate = useReplaceNavigate();

  useEffect(() => {
    const timer = window.setTimeout(() => {
      navigate(`/room/${joinCode}/lobby`);
    }, ROSTER_TIMEOUT_MS);
    return () => window.clearTimeout(timer);
  }, [joinCode, navigate]);

  return (
    <S.Notice role="status" aria-live="polite">
      <S.Title>참가 정보를 불러오는 중</S.Title>
      <S.Body>
        순위와 진행률은 그대로 움직입니다. 몇 초 안에 채워지지 않으면 로비로 돌아갑니다.
      </S.Body>
    </S.Notice>
  );
};

export default TrackNotice;
