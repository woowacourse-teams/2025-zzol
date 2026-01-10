import { MiniGameType } from '@/types/miniGame/common';

export type TestMessage =
  | {
      type: 'START_TEST';
      role: 'host' | 'guest';
      joinCode?: string;
      iframeName?: string;
      gameSequence: MiniGameType[];
    }
  | { type: 'JOIN_CODE_RECEIVED'; joinCode: string }
  | { type: 'GUEST_READY'; iframeName?: string }
  | { type: 'CLICK_GAME_START' }
  | { type: 'PATH_CHANGE'; iframeName: string; path: string }
  | { type: 'TEST_COMPLETED' }
  | { type: 'STOP_TEST' }
  | { type: 'PAUSE_TEST' }
  | { type: 'RESUME_TEST' }
  | { type: 'RESET_TO_HOME' }
  | { type: 'IFRAME_READY'; iframeName: string };
