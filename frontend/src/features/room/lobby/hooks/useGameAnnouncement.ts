import { Player, PlayerType } from '@/types/player';
import { useCallback, useEffect, useRef, useState } from 'react';

type AnnouncementOptions = {
  isAllReady: boolean;
  participants: Player[];
  playerType: PlayerType | null;
  myName: string;
};

export const useGameAnnouncement = ({
  isAllReady,
  participants,
  playerType,
  myName,
}: AnnouncementOptions) => {
  const [announcement, setAnnouncement] = useState('');
  const previousParticipantsRef = useRef<Player[]>([]);
  const previousReadyStateRef = useRef(false);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isFirstRenderRef = useRef(true);

  const announce = useCallback((message: string) => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }

    setAnnouncement(message);
    timeoutRef.current = setTimeout(() => {
      setAnnouncement('');
      timeoutRef.current = null;
    }, 100);
  }, []);

  useEffect(() => {
    if (isFirstRenderRef.current) {
      isFirstRenderRef.current = false;
      previousReadyStateRef.current = isAllReady;
      return;
    }

    if (!previousReadyStateRef.current && isAllReady) {
      if (playerType === 'HOST') {
        announce('모든 참가자가 준비되었습니다. 게임 시작 버튼을 눌러주세요.');
      } else {
        announce('모든 참가자가 준비되었습니다. 호스트가 게임을 시작할 때까지 기다려주세요.');
      }
    } else if (previousReadyStateRef.current && !isAllReady) {
      announce('참가자 중 누군가 준비를 취소했습니다.');
    }

    previousReadyStateRef.current = isAllReady;
  }, [isAllReady, playerType, announce]);

  useEffect(() => {
    if (previousParticipantsRef.current.length > 0) {
      const previousNames = previousParticipantsRef.current.map((p) => p.playerName);
      const currentNames = participants.map((p) => p.playerName);

      const joined = participants.filter(
        (player) => !previousNames.includes(player.playerName) && player.playerName !== myName
      );

      const left = previousParticipantsRef.current.filter(
        (player) => !currentNames.includes(player.playerName) && player.playerName !== myName
      );

      if (joined.length > 0) {
        const names = joined.map((p) => p.playerName).join(', ');
        announce(`${names}님이 입장하셨습니다`);
      } else if (left.length > 0) {
        const names = left.map((p) => p.playerName).join(', ');
        announce(`${names}님이 퇴장하셨습니다`);
      }
    }

    previousParticipantsRef.current = participants;
  }, [participants, myName, announce]);

  useEffect(() => {
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
      previousParticipantsRef.current = [];
      previousReadyStateRef.current = false;
      isFirstRenderRef.current = true;
    };
  }, []);

  return announcement;
};

export default useGameAnnouncement;
