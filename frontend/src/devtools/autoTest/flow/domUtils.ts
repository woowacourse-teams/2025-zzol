export const DELAY_BETWEEN_ACTIONS = 250;
export const DELAY_AFTER_API = 1000;

export const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export const findElement = (testId: string): HTMLElement | null => {
  return document.querySelector(`[data-testid="${testId}"]`);
};

export const clickElement = async (element: HTMLElement | null) => {
  if (!element) {
    console.warn('[AutoTest] Element not found');
    return;
  }

  // React의 pointer 이벤트만 사용하여 시뮬레이션
  const pointerDownEvent = new PointerEvent('pointerdown', {
    bubbles: true,
    cancelable: true,
    pointerId: 1,
    pointerType: 'mouse',
    clientX: 0,
    clientY: 0,
  });

  const pointerUpEvent = new PointerEvent('pointerup', {
    bubbles: true,
    cancelable: true,
    pointerId: 1,
    pointerType: 'mouse',
    clientX: 0,
    clientY: 0,
  });

  element.dispatchEvent(pointerDownEvent);
  await wait(10);
  element.dispatchEvent(pointerUpEvent);
  await wait(DELAY_BETWEEN_ACTIONS);
};

export const clickElementWithClickEvent = async (element: HTMLElement | null) => {
  if (!element) {
    console.warn('[AutoTest] Element not found');
    return;
  }

  // onClick 이벤트 핸들러를 가진 요소 (예: ToggleButton)
  const clickEvent = new MouseEvent('click', {
    bubbles: true,
    cancelable: true,
    view: window,
  });

  element.dispatchEvent(clickEvent);
  await wait(DELAY_BETWEEN_ACTIONS);
};

export const typeInInput = async (element: HTMLElement | null, value: string) => {
  if (!element || !(element instanceof HTMLInputElement)) {
    console.warn('[AutoTest] Input element not found');
    return;
  }

  element.focus();

  // React의 onChange를 트리거하기 위한 이벤트 생성
  const createInputEvent = () => {
    return new Event('input', { bubbles: true, cancelable: true });
  };

  const createChangeEvent = () => {
    return new Event('change', { bubbles: true, cancelable: true });
  };

  // value를 직접 설정하고 이벤트 발생
  element.value = '';
  const clearEvent = createInputEvent();
  element.dispatchEvent(clearEvent);
  await wait(50);

  // React가 value 변경을 감지하도록 Object.defineProperty로 설정
  const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
    window.HTMLInputElement.prototype,
    'value'
  )?.set;
  if (nativeInputValueSetter) {
    nativeInputValueSetter.call(element, value);
    const inputEvent = createInputEvent();
    const changeEvent = createChangeEvent();
    element.dispatchEvent(inputEvent);
    await wait(50);
    element.dispatchEvent(changeEvent);
  } else {
    // Fallback: 직접 설정
    element.value = value;
    const inputEvent = createInputEvent();
    const changeEvent = createChangeEvent();
    element.dispatchEvent(inputEvent);
    await wait(50);
    element.dispatchEvent(changeEvent);
  }

  await wait(DELAY_BETWEEN_ACTIONS);
};

export const waitForElement = async (
  testId: string,
  maxWait = 10000
): Promise<HTMLElement | null> => {
  const startTime = Date.now();
  while (Date.now() - startTime < maxWait) {
    const element = findElement(testId);
    if (element) {
      await wait(100);
      return element;
    }
    await wait(100);
  }
  console.warn(`[AutoTest] Element with testId "${testId}" not found after ${maxWait}ms`);
  return null;
};

export const findFirstElementByTestIdPrefix = (prefix: string): HTMLElement | null => {
  const elements = Array.from(document.querySelectorAll(`[data-testid^="${prefix}"]`));
  return (elements[0] as HTMLElement) || null;
};

export const extractJoinCode = (): string | null => {
  const match = window.location.pathname.match(/^\/room\/([^/]+)/);
  return match ? match[1] : null;
};
