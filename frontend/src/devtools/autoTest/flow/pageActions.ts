import {
  clickElement,
  clickElementWithClickEvent,
  findElement,
  findFirstElementByTestIdPrefix,
  typeInInput,
  wait,
  waitForElement,
  extractJoinCode,
  DELAY_BETWEEN_ACTIONS,
  DELAY_AFTER_API,
} from './domUtils';
import { MiniGameType } from '@/types/miniGame/common';

// 기본 게임 순서 상수
export const DEFAULT_SINGLE_GAME: readonly MiniGameType[] = ['CARD_GAME'] as const;
export const DEFAULT_DOUBLE_GAMES: readonly MiniGameType[] = ['CARD_GAME', 'RACING_GAME'] as const;

export type PageActionContext = {
  role: 'host' | 'guest';
  joinCode?: string;
  playerName: string;
  iframeName?: string;
  gameSequence?: MiniGameType[];
};

export type PageAction = {
  pathPattern: string | RegExp;
  role?: 'host' | 'guest';
  execute: (context: PageActionContext) => Promise<void>;
};

// 홈 페이지 액션
const homePageHostAction = async () => {
  const createButton = await waitForElement('create-room-button', 5000);
  if (!createButton) {
    console.warn('[AutoTest] Create room button not found');
    return;
  }
  await clickElement(createButton);
};

const homePageGuestAction = async (context: PageActionContext) => {
  const joinButton = await waitForElement('join-room-button', 5000);
  if (!joinButton) {
    console.warn('[AutoTest] Join room button not found');
    return;
  }
  await clickElement(joinButton);

  // joinCode 입력 처리 (모달이 열림)
  await wait(DELAY_BETWEEN_ACTIONS);

  if (context.joinCode) {
    const joinCodeInput = await waitForElement('join-code-input', 10000);
    if (!joinCodeInput) {
      console.warn('[AutoTest] Join code input not found');
      return;
    }
    await typeInInput(joinCodeInput, context.joinCode);

    const enterButton = await waitForElement('enter-room-button', 10000);
    if (!enterButton) {
      console.warn('[AutoTest] Enter room button not found');
      return;
    }
    await clickElement(enterButton);
  }
};

// 이름 입력 페이지 액션 (공통)
const entryNamePageAction = async (context: PageActionContext) => {
  await wait(300);

  const nameInput = await waitForElement('player-name-input', 10000);
  if (!nameInput) {
    console.warn('[AutoTest] Name input not found');
    return;
  }
  await typeInInput(nameInput, context.playerName);

  // 역할별 제출 버튼
  const submitButtonTestId =
    context.role === 'host' ? 'create-room-submit-button' : 'join-room-submit-button';
  const submitButton = await waitForElement(submitButtonTestId, 10000);
  if (!submitButton) {
    console.warn(`[AutoTest] ${submitButtonTestId} not found`);
    return;
  }
  await clickElement(submitButton);

  await wait(DELAY_AFTER_API);
};

// 로비 페이지 - 호스트 액션
const lobbyPageHostAction = async (context: PageActionContext) => {
  const joinCode = extractJoinCode();
  if (!joinCode) {
    console.warn('[AutoTest] Could not extract joinCode from path');
    return;
  }

  if (window.parent && window.parent !== window) {
    window.parent.postMessage({ type: 'JOIN_CODE_RECEIVED', joinCode }, '*');
  }

  await wait(DELAY_BETWEEN_ACTIONS);

  const miniGameToggle = await waitForElement('toggle-option-미니게임', 10000);
  if (!miniGameToggle) {
    console.warn('[AutoTest] MiniGame toggle button not found');
    return;
  }
  await clickElementWithClickEvent(miniGameToggle);

  await wait(DELAY_BETWEEN_ACTIONS * 2);

  // 게임 순서 결정
  const gameSequence = context.gameSequence || DEFAULT_SINGLE_GAME;

  // 각 게임을 순서대로 선택
  for (const gameType of gameSequence) {
    const gameButton = findElement(`game-action-${gameType}`);
    let attempts = 0;
    let button = gameButton;

    // 버튼이 나타날 때까지 대기
    while (!button && attempts < 50) {
      await wait(200);
      button = findElement(`game-action-${gameType}`);
      attempts++;
    }

    if (!button) {
      console.warn(`[AutoTest] ${gameType} button not found after waiting`);
      continue;
    }

    // 이미 선택된 게임인지 확인 (토글 동작 고려)
    const isSelected =
      button.getAttribute('aria-selected') === 'true' ||
      button.classList.contains('selected') ||
      button.getAttribute('data-selected') === 'true';

    if (!isSelected) {
      await clickElementWithClickEvent(button);
      await wait(DELAY_BETWEEN_ACTIONS);
    }
  }

  await wait(DELAY_AFTER_API);
};

// 로비 페이지 - 게스트 액션
const lobbyPageGuestAction = async () => {
  await wait(DELAY_BETWEEN_ACTIONS);

  const readyButton = await waitForElement('game-ready-button', 10000);
  if (!readyButton) {
    console.warn('[AutoTest] Game ready button not found');
    return;
  }
  await clickElement(readyButton);

  await wait(DELAY_AFTER_API);

  if (window.parent && window.parent !== window) {
    const iframeName = window.frameElement?.getAttribute('name') || '';
    window.parent.postMessage({ type: 'GUEST_READY', iframeName }, '*');
  }
};

// 게임 시작 버튼 클릭 (메시지 수신 시)
export const handleHostGameStart = async () => {
  const gameStartButton = await waitForElement('game-start-button', 10000);
  if (!gameStartButton) {
    console.warn('[AutoTest] Game start button not found');
    return;
  }

  await wait(DELAY_BETWEEN_ACTIONS);
  await clickElement(gameStartButton);
  // 게임 결과 페이지는 페이지 기반 플로우가 처리하도록 함
};

// 게임 결과 페이지 - 호스트 액션
const gameResultPageHostAction = async () => {
  await wait(500);
  await wait(DELAY_BETWEEN_ACTIONS);

  const rouletteResultButton = await waitForElement('roulette-result-button', 10000);
  if (!rouletteResultButton) {
    console.warn('[AutoTest] Roulette result button not found');
    return;
  }

  await clickElement(rouletteResultButton);

  await wait(DELAY_AFTER_API);
};

// 룰렛 플레이 페이지 - 호스트 액션
const roulettePlayPageHostAction = async () => {
  await wait(DELAY_BETWEEN_ACTIONS * 2);

  let rouletteSpinButton = findElement('roulette-spin-button');
  let spinAttempts = 0;
  while (!rouletteSpinButton && spinAttempts < 100) {
    await wait(200);
    rouletteSpinButton = findElement('roulette-spin-button');
    spinAttempts++;
  }

  if (!rouletteSpinButton) {
    console.warn('[AutoTest] Roulette spin button not found');
    return;
  }

  // 버튼이 활성화될 때까지 대기 (disabled 상태가 아닐 때까지)
  let isDisabled =
    rouletteSpinButton.hasAttribute('disabled') ||
    rouletteSpinButton.classList.contains('disabled') ||
    rouletteSpinButton.getAttribute('aria-disabled') === 'true';
  let disabledAttempts = 0;
  while (isDisabled && disabledAttempts < 100) {
    await wait(200);
    rouletteSpinButton = findElement('roulette-spin-button');
    if (rouletteSpinButton) {
      isDisabled =
        rouletteSpinButton.hasAttribute('disabled') ||
        rouletteSpinButton.classList.contains('disabled') ||
        rouletteSpinButton.getAttribute('aria-disabled') === 'true';
    }
    disabledAttempts++;
  }

  if (!rouletteSpinButton) {
    console.warn('[AutoTest] Roulette spin button not found after waiting for enabled state');
    return;
  }

  await clickElement(rouletteSpinButton);
};

// 레이싱 게임 플레이 페이지 - 연속 클릭 관리
let racingGameClickIntervalId: number | null = null;

export const clearRacingGameClickInterval = () => {
  if (racingGameClickIntervalId !== null) {
    clearInterval(racingGameClickIntervalId);
    racingGameClickIntervalId = null;
  }
};

// 레이싱 게임 플레이 페이지 - 공통 액션 (호스트/게스트 모두)
const racingGamePlayPageAction = async () => {
  // 기존 인터벌이 있으면 먼저 정리
  clearRacingGameClickInterval();

  await wait(DELAY_BETWEEN_ACTIONS);

  // 1초에 5번 클릭 = 200ms마다 클릭
  const CLICK_INTERVAL_MS = 100;

  racingGameClickIntervalId = window.setInterval(() => {
    // RacingGameOverlay 요소 찾기 (data-testid 사용)
    const overlayElement = findElement('racing-game-overlay');

    // Overlay를 찾지 못한 경우 body에 이벤트 발생 (fallback)
    const targetElement = overlayElement || document.body;

    // pointerdown 이벤트 발생
    const pointerDownEvent = new PointerEvent('pointerdown', {
      bubbles: true,
      cancelable: true,
      pointerId: 1,
      pointerType: 'mouse',
      clientX: window.innerWidth / 2,
      clientY: window.innerHeight / 2,
    });

    targetElement.dispatchEvent(pointerDownEvent);
  }, CLICK_INTERVAL_MS);
};

// 페이지 액션 목록
export const pageActions: PageAction[] = [
  {
    pathPattern: '/',
    role: 'host',
    execute: homePageHostAction,
  },
  {
    pathPattern: '/',
    role: 'guest',
    execute: homePageGuestAction,
  },
  {
    pathPattern: '/entry/name',
    execute: entryNamePageAction,
  },
  {
    pathPattern: /^\/room\/[^/]+\/lobby$/,
    role: 'host',
    execute: lobbyPageHostAction,
  },
  {
    pathPattern: /^\/room\/[^/]+\/lobby$/,
    role: 'guest',
    execute: lobbyPageGuestAction,
  },
  {
    pathPattern: /^\/room\/[^/]+\/[^/]+\/result$/, // /room/:joinCode/:miniGameType/result
    role: 'host',
    execute: gameResultPageHostAction,
  },
  {
    pathPattern: /^\/room\/[^/]+\/roulette\/play$/,
    role: 'host',
    execute: roulettePlayPageHostAction,
  },
  {
    pathPattern: /^\/room\/[^/]+\/RACING_GAME\/play$/,
    execute: racingGamePlayPageAction,
  },
];

// 경로 패턴 매칭 유틸리티
export const matchPath = (pathname: string, pattern: string | RegExp): boolean => {
  if (typeof pattern === 'string') {
    return pathname === pattern;
  }
  return pattern.test(pathname);
};

// 현재 경로에 맞는 페이지 액션 찾기
export const findPageAction = (pathname: string, role: 'host' | 'guest'): PageAction | null => {
  // 역할별 필터링 후 정확한 경로 매칭 우선, 그 다음 패턴 매칭
  const exactMatch = pageActions.find(
    (action) =>
      (!action.role || action.role === role) &&
      typeof action.pathPattern === 'string' &&
      action.pathPattern === pathname
  );

  if (exactMatch) {
    return exactMatch;
  }

  const patternMatch = pageActions.find(
    (action) =>
      (!action.role || action.role === role) &&
      typeof action.pathPattern !== 'string' &&
      matchPath(pathname, action.pathPattern)
  );

  return patternMatch || null;
};
