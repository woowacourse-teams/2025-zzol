import { useEffect, useState } from 'react';

/**
 * 눈치게임 단일 카운트다운 루프(요구사항 G).
 *
 * BlockStacking 의 useBlockStackingGame rAF 타이머 패턴을 따른다(per-component setInterval 금지).
 * PLAYING(idle/hardCap)·COLLISION_COOLDOWN(resumeAt) 타이머를 하나의 requestAnimationFrame 루프로
 * 처리해, 화면에 그릴 "남은 시간(초)"을 반환한다.
 *
 * 스큐 보정(요구사항 D):
 *   remaining = deadline - (Date.now() + serverOffsetMs)
 *   절대 epoch ms 만으로는 클라 시계가 빠를 때 수백 ms 쿨다운이 음수로 시작해 즉시 끝나버린다.
 *   serverOffsetMs(= serverNowEpochMs - Date.now(), Provider 가 매 메시지 갱신)로 보정한다.
 *
 * @param deadlineEpochMs 카운트다운 목표 시각(서버 epoch ms). 상태에 따라 idleDeadline 또는 resumeAt.
 * @param serverOffsetMs  시계 스큐 오프셋(Provider 제공).
 * @param enabled         루프 활성 여부(예: 해당 상태일 때만 true).
 * @returns remainingMs   남은 시간(ms). 0 미만은 0 으로 clamp.
 */
export const useNunchiCountdown = (
  deadlineEpochMs: number | null,
  serverOffsetMs: number,
  enabled: boolean
): number => {
  const [remainingMs, setRemainingMs] = useState(0);

  useEffect(() => {
    if (!enabled || deadlineEpochMs == null) {
      setRemainingMs(0);
      return;
    }

    let rafId = 0;

    // 절대 epoch ms 데드라인을 매 프레임 재계산한다(스큐 보정 포함).
    // BlockStacking 의 "deadline 루프"(delta 누적이 아님)와 같은 방식이라
    // 탭 복귀 시에도 자동 보정된다 — prevTime/document.hidden 처리가 필요 없다.
    const tick = () => {
      const remaining = Math.max(0, deadlineEpochMs - (Date.now() + serverOffsetMs));
      setRemainingMs(remaining);
      // 0 도달 시 루프를 멈춘다. 실제 상태 전환(종료/재개)은 서버 state 가 권위다.
      if (remaining <= 0) return;
      rafId = requestAnimationFrame(tick);
    };

    rafId = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(rafId);
  }, [deadlineEpochMs, serverOffsetMs, enabled]);

  return remainingMs;
};
