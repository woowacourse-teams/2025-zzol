import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { RefObject } from 'react';
import { MiniGameType } from '@/types/miniGame/common';
import { TestMessage } from '@/devtools/autoTest/types/testMessage';
import { createIframeMessenger } from '../utils/iframeMessenger';

type MessageHandler = (message: TestMessage) => void;
type MessageHandlers = Partial<Record<TestMessage['type'], MessageHandler>>;

type UseAutoTestBridgeParams = {
  isOpen: boolean;
  iframeNames: string[];
  gameSequence: MiniGameType[];
  iframeRefs: RefObject<Record<string, HTMLIFrameElement | null>>;
};

type RunState = {
  isRunning: boolean;
  isPaused: boolean;
};

type ReadinessState = {
  guests: Record<string, boolean>;
  iframes: Record<string, boolean>;
};

type BridgeControls = {
  start: () => void;
  stop: () => void;
  pause: () => void;
  resume: () => void;
};

export type UseAutoTestBridgeResult = {
  runState: RunState;
  readiness: ReadinessState;
  iframePaths: Record<string, string>;
  controls: BridgeControls;
};

type UseStartHostOnGuestsReadyParams = {
  isOpen: boolean;
  guestIframeNames: string[];
  guestReadyState: Record<string, boolean>;
  sendToIframe: (iframeName: string, message: TestMessage) => void;
};

const useStartHostOnGuestsReady = ({
  isOpen,
  guestIframeNames,
  guestReadyState,
  sendToIframe,
}: UseStartHostOnGuestsReadyParams) => {
  useEffect(() => {
    if (!isOpen) return;
    if (guestIframeNames.length === 0) return;

    const allGuestsReady = guestIframeNames.every((guestName) => guestReadyState[guestName]);
    if (allGuestsReady) {
      sendToIframe('host', { type: 'CLICK_GAME_START' });
    }
  }, [guestIframeNames, guestReadyState, isOpen, sendToIframe]);
};

type SimpleCommandType = 'RESET_TO_HOME' | 'STOP_TEST' | 'PAUSE_TEST' | 'RESUME_TEST';

export const useAutoTestBridge = ({
  isOpen,
  iframeNames,
  gameSequence,
  iframeRefs,
}: UseAutoTestBridgeParams): UseAutoTestBridgeResult => {
  // 상태
  const [isRunning, setIsRunning] = useState<boolean>(false);
  const [isPaused, setIsPaused] = useState<boolean>(false);
  const [guestReadyState, setGuestReadyState] = useState<Record<string, boolean>>({});
  const [readyState, setReadyState] = useState<Record<string, boolean>>({});
  const [iframePaths, setIframePaths] = useState<Record<string, string>>({});

  // 레퍼런스
  const joinCodeRef = useRef<string | null>(null);
  const pendingStartTest = useRef<boolean>(false);

  // 파생 값
  const guestIframeNames = useMemo(
    () => iframeNames.filter((name) => name.startsWith('guest')),
    [iframeNames]
  );

  const { sendToIframe, broadcastToIframes } = useMemo(() => {
    return createIframeMessenger({
      iframeRefs,
    });
  }, [iframeRefs]);

  // 명령 핸들러
  const initializeRunState = useCallback(() => {
    setIsRunning(true);
    setIsPaused(false);
    joinCodeRef.current = null;
    pendingStartTest.current = true;
    setGuestReadyState({});
    setReadyState({});
  }, []);

  const finalizeRunState = useCallback(() => {
    setIsRunning(false);
    setIsPaused(false);
    pendingStartTest.current = false;
  }, []);

  const broadcastCommandToAll = useCallback(
    (type: SimpleCommandType) => {
      broadcastToIframes(iframeNames, () => ({ type }));
    },
    [broadcastToIframes, iframeNames]
  );

  const handleStartTest = useCallback(() => {
    initializeRunState();

    broadcastCommandToAll('RESET_TO_HOME');
  }, [broadcastCommandToAll, initializeRunState]);

  const handleStopTest = useCallback(() => {
    finalizeRunState();

    broadcastCommandToAll('STOP_TEST');
  }, [broadcastCommandToAll, finalizeRunState]);

  const handlePauseTest = useCallback(() => {
    setIsPaused(true);
    broadcastCommandToAll('PAUSE_TEST');
  }, [broadcastCommandToAll]);

  const handleResumeTest = useCallback(() => {
    setIsPaused(false);
    broadcastCommandToAll('RESUME_TEST');
  }, [broadcastCommandToAll]);

  // 메시지 처리
  useEffect(() => {
    if (!isOpen) return;

    const messageHandlers: MessageHandlers = {
      JOIN_CODE_RECEIVED: (message) => {
        if (message.type !== 'JOIN_CODE_RECEIVED') return;

        const { joinCode } = message;
        joinCodeRef.current = joinCode;

        broadcastToIframes(guestIframeNames, (guestName) => ({
          type: 'START_TEST',
          role: 'guest',
          joinCode,
          iframeName: guestName,
          gameSequence,
        }));
      },
      IFRAME_READY: (message) => {
        if (message.type !== 'IFRAME_READY') return;

        const { iframeName } = message;

        if (iframeName) {
          setReadyState((prev) => ({
            ...prev,
            [iframeName]: true,
          }));
        }

        if (pendingStartTest.current && iframeName === 'host') {
          setTimeout(() => {
            const startMessage: TestMessage = {
              type: 'START_TEST',
              role: 'host',
              gameSequence,
            };
            sendToIframe('host', startMessage);
            pendingStartTest.current = false;
          }, 0);
        }
      },
      GUEST_READY: (message) => {
        if (message.type !== 'GUEST_READY') return;

        const { iframeName } = message;
        if (iframeName) {
          setGuestReadyState((prev) => ({
            ...prev,
            [iframeName]: true,
          }));
        }
      },
      PATH_CHANGE: (message) => {
        if (message.type !== 'PATH_CHANGE') return;

        const { iframeName, path } = message;
        setIframePaths((prev) => ({
          ...prev,
          [iframeName]: path,
        }));
      },
      TEST_COMPLETED: () => {
        setIsRunning(false);
        setIsPaused(false);
      },
    };

    const handleMessage = (event: MessageEvent<TestMessage>) => {
      if (!event.data || typeof event.data !== 'object' || !('type' in event.data)) {
        return;
      }

      const messageData = event.data;
      const handler = messageHandlers[messageData.type];
      if (handler) {
        handler(messageData as never);
      }
    };

    window.addEventListener('message', handleMessage);
    return () => {
      window.removeEventListener('message', handleMessage);
    };
  }, [broadcastToIframes, gameSequence, guestIframeNames, isOpen, sendToIframe]);

  useStartHostOnGuestsReady({
    isOpen,
    guestIframeNames,
    guestReadyState,
    sendToIframe,
  });

  return {
    runState: {
      isRunning,
      isPaused,
    },
    readiness: {
      guests: guestReadyState,
      iframes: readyState,
    },
    iframePaths,
    controls: {
      start: handleStartTest,
      stop: handleStopTest,
      pause: handlePauseTest,
      resume: handleResumeTest,
    },
  };
};
