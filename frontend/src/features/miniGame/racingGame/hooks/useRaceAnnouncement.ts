import { useEffect, useRef, useState } from 'react';

const ANNOUNCE_INTERVAL_MS = 2000;

type Props = {
  rank: number;
  totalPlayers: number;
  remainingDistance: number;
  enabled: boolean;
};

/**
 * 스크린리더에 읽힐 경기 상황 문장을 만든다.
 *
 * 서버가 100ms 마다 위치를 내려주므로 그대로 aria-live 에 넣으면 낭독이 끊기며 겹친다.
 * 2초에 한 번만 갱신해 문장 하나가 끝까지 읽히게 한다.
 */
export const useRaceAnnouncement = ({
  rank,
  totalPlayers,
  remainingDistance,
  enabled,
}: Props): string => {
  const [announcement, setAnnouncement] = useState('');
  const latestRef = useRef({ rank, totalPlayers, remainingDistance });

  useEffect(() => {
    latestRef.current = { rank, totalPlayers, remainingDistance };
  }, [rank, totalPlayers, remainingDistance]);

  useEffect(() => {
    if (!enabled) return;

    const timer = window.setInterval(() => {
      const latest = latestRef.current;
      setAnnouncement(
        `${latest.totalPlayers}명 중 ${latest.rank}등, 결승까지 ${latest.remainingDistance} 남음`
      );
    }, ANNOUNCE_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, [enabled]);

  return announcement;
};
