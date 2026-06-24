import { wait, DELAY_BETWEEN_ACTIONS } from './domUtils';
import {
  findPageAction,
  handleHostGameStart,
  clearRacingGameClickInterval,
  type PageActionContext,
} from './pageActions';
import { MiniGameType } from '@/types/miniGame/common';
import { TestMessage } from '@/devtools/autoTest/types/testMessage';

// 플로우 상태 추적 (각 역할별로)
type FlowState = 'idle' | 'running' | 'paused';
const flowState: Record<'host' | 'guest', FlowState> = {
  host: 'idle',
  guest: 'idle',
};

// 역할별 상태 접근 헬퍼 함수 (타입 추론 개선)
const getFlowState = (role: 'host' | 'guest'): FlowState => flowState[role];
const setFlowState = (role: 'host' | 'guest', state: FlowState): void => {
  flowState[role] = state;
};

// 페이지 기반 플로우 실행
const runFlow = async (role: 'host' | 'guest', context: PageActionContext) => {
  const currentState = getFlowState(role);
  if (currentState === 'running' || currentState === 'paused') {
    return;
  }

  setFlowState(role, 'running');

  try {
    await wait(DELAY_BETWEEN_ACTIONS);

    let currentPath = window.location.pathname;
    let joinCode: string | null = null;

    while (true) {
      const currentFlowState = getFlowState(role);
      if (currentFlowState !== 'running' && currentFlowState !== 'paused') {
        break;
      }

      const pathJoinCodeMatch = currentPath.match(/^\/room\/([^/]+)/);
      if (pathJoinCodeMatch) {
        joinCode = pathJoinCodeMatch[1];
        context.joinCode = joinCode;
      }

      const pageAction = findPageAction(currentPath, role);
      if (pageAction) {
        await pageAction.execute(context);
      }

      const checkPausedState = getFlowState(role);
      if (checkPausedState === 'paused') {
        await waitForResume(role);
      }

      await waitForPathChange(currentPath, role);

      const newPath = window.location.pathname;

      if (currentPath.match(/^\/room\/[^/]+\/RACING_GAME\/play$/)) {
        clearRacingGameClickInterval();
      }

      if (/^\/room\/[^/]+\/order$/.test(newPath)) {
        setFlowState(role, 'idle');
        break;
      }

      if (newPath === '/') {
        clearRacingGameClickInterval();
        setFlowState(role, 'idle');
        break;
      }

      currentPath = newPath;
    }
  } catch (error) {
    console.error(`[AutoTest Debug] Error in ${role} flow:`, error);
    throw error;
  } finally {
    setFlowState(role, 'idle');
  }
};

// 경로 변경 대기 (타임아웃 없음)
const waitForPathChange = async (currentPath: string, role: 'host' | 'guest'): Promise<void> => {
  while (window.location.pathname === currentPath) {
    if (getFlowState(role) === 'paused') {
      await waitForResume(role);
    }
    await wait(100);
  }
  await wait(500); // 경로 변경 후 안정화 대기
};

// 재개 신호 대기
const waitForResume = async (role: 'host' | 'guest'): Promise<void> => {
  while (getFlowState(role) === 'paused') {
    await wait(100);
  }
};

// 호스트 플로우 실행
const runHostFlow = async (gameSequence?: MiniGameType[]) => {
  const context: PageActionContext = {
    role: 'host',
    playerName: 'host',
    gameSequence,
  };

  await runFlow('host', context);
};

// 게스트 플로우 실행
const runGuestFlow = async (joinCode: string, iframeName?: string) => {
  const guestName = iframeName || 'guest1';
  const context: PageActionContext = {
    role: 'guest',
    joinCode,
    playerName: guestName,
    iframeName,
  };

  await runFlow('guest', context);
};

type MessageHandlers = {
  [K in TestMessage['type']]?: (payload: Extract<TestMessage, { type: K }>) => Promise<void> | void;
};

type MessageHandlerContext = {
  getGuestJoinCode: () => string | null;
  setGuestJoinCode: (value: string | null) => void;
};

const createMessageHandlers = ({
  getGuestJoinCode,
  setGuestJoinCode,
}: MessageHandlerContext): MessageHandlers => ({
  START_TEST: async ({ role, joinCode, iframeName, gameSequence }) => {
    if (role === 'host') {
      runHostFlow(gameSequence);
      return;
    }

    if (role === 'guest' && joinCode) {
      setGuestJoinCode(joinCode);
      await runGuestFlow(joinCode, iframeName);
    }
  },
  JOIN_CODE_RECEIVED: async ({ joinCode }) => {
    if (getGuestJoinCode() !== null) {
      return;
    }

    setGuestJoinCode(joinCode);
    const iframeName = window.frameElement?.getAttribute('name') || '';
    await runGuestFlow(joinCode, iframeName);
  },
  CLICK_GAME_START: async () => {
    await handleHostGameStart();
  },
  TEST_COMPLETED: () => {
    clearRacingGameClickInterval();
    setFlowState('host', 'idle');
    setFlowState('guest', 'idle');
  },
  STOP_TEST: () => {
    clearRacingGameClickInterval();
    setFlowState('host', 'idle');
    setFlowState('guest', 'idle');
  },
  PAUSE_TEST: () => {
    if (getFlowState('host') === 'running') {
      setFlowState('host', 'paused');
    }
    if (getFlowState('guest') === 'running') {
      setFlowState('guest', 'paused');
    }
  },
  RESUME_TEST: () => {
    if (getFlowState('host') === 'paused') {
      setFlowState('host', 'running');
    }
    if (getFlowState('guest') === 'paused') {
      setFlowState('guest', 'running');
    }
  },
  RESET_TO_HOME: () => {
    window.location.href = '/';
  },
});

export const setupAutoTestListener = () => {
  if (typeof window === 'undefined') return;

  let guestJoinCode: string | null = null;

  const getGuestJoinCode = () => guestJoinCode;
  const setGuestJoinCode = (value: string | null) => {
    guestJoinCode = value;
  };

  const handlers = createMessageHandlers({ getGuestJoinCode, setGuestJoinCode });

  const messageHandler = async (event: MessageEvent<TestMessage>) => {
    // TestMessage 타입 체크
    if (!event.data || typeof event.data !== 'object' || !('type' in event.data)) {
      return;
    }

    const { type } = event.data;
    const handler = handlers[type];
    if (!handler) {
      return;
    }

    await handler(event.data as never);
  };

  window.addEventListener('message', messageHandler);

  return () => {
    window.removeEventListener('message', messageHandler);
  };
};
