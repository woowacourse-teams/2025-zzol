import { useCallback, useMemo, useRef, useState, type RefObject } from 'react';

type IframeRefMap = Record<string, HTMLIFrameElement | null>;

export const HOST_IFRAME_NAME = 'host';
export const PRIMARY_GUEST_IFRAME_NAME = 'guest1';

const DEFAULT_IFRAME_NAMES = [HOST_IFRAME_NAME, PRIMARY_GUEST_IFRAME_NAME] as const;
const MIN_GUEST_IFRAME_NUMBER = 1;
const MAX_GUEST_IFRAME_NUMBER = 8;
const MAX_REGISTERED_IFRAME_COUNT = 9;
const FULL_HEIGHT_IFRAME_COUNT_THRESHOLD = 4;

const createGuestName = (guestNumber: number) => `guest${guestNumber}`;

const extractGuestNumber = (name: string): number | null => {
  const match = name.match(/^guest(\d+)$/);
  if (!match) return null;

  const guestNumber = Number.parseInt(match[1], 10);
  if (Number.isNaN(guestNumber)) return null;

  return guestNumber;
};

const findNextAvailableGuestNumber = (names: string[]): number | null => {
  const usedNumbers = names.reduce<Set<number>>((acc, currentName) => {
    const guestNumber = extractGuestNumber(currentName);
    if (guestNumber !== null) acc.add(guestNumber);
    return acc;
  }, new Set<number>());

  for (let number = MIN_GUEST_IFRAME_NUMBER; number <= MAX_GUEST_IFRAME_NUMBER; number += 1) {
    if (!usedNumbers.has(number)) return number;
  }

  return null;
};

const isProtectedIframeName = (name: string) => {
  return name === HOST_IFRAME_NAME || name === PRIMARY_GUEST_IFRAME_NAME;
};

const computeLayoutState = (iframeCount: number) => {
  const hasFullHeight = iframeCount <= FULL_HEIGHT_IFRAME_COUNT_THRESHOLD;

  return {
    iframeHeight: hasFullHeight ? '100%' : 'auto',
    useMinHeight: !hasFullHeight,
    canAddMore: iframeCount < MAX_REGISTERED_IFRAME_COUNT,
  };
};

type RegistryData = {
  iframeNames: string[];
  iframeRefs: RefObject<IframeRefMap>;
};

type RegistryLayout = {
  iframeHeight: string;
  useMinHeight: boolean;
  canAddMore: boolean;
};

type RegistryActions = {
  addGuestIframe: () => void;
  removeIframe: (name: string) => void;
  setIframeRef: (name: string, iframe: HTMLIFrameElement | null) => void;
};

export type UseIframeRegistryResult = {
  data: RegistryData;
  layout: RegistryLayout;
  actions: RegistryActions;
};

export const useIframeRegistry = (
  initialNames: string[] = [...DEFAULT_IFRAME_NAMES]
): UseIframeRegistryResult => {
  const [iframeNames, setIframeNames] = useState<string[]>(initialNames);
  const iframeRefs = useRef<IframeRefMap>({});

  const setIframeRef = useCallback((name: string, iframe: HTMLIFrameElement | null) => {
    if (!iframe) {
      delete iframeRefs.current[name];
      return;
    }

    iframeRefs.current[name] = iframe;
  }, []);

  const addGuestIframe = useCallback(() => {
    if (iframeNames.length >= MAX_REGISTERED_IFRAME_COUNT) return;

    const nextGuestNumber = findNextAvailableGuestNumber(iframeNames);
    if (nextGuestNumber === null) return;

    const newGuestName = createGuestName(nextGuestNumber);
    setIframeNames((prev) => [...prev, newGuestName]);
  }, [iframeNames]);

  const removeIframe = useCallback(
    (name: string) => {
      if (isProtectedIframeName(name)) return;

      const lastIframeName = iframeNames[iframeNames.length - 1];
      if (name !== lastIframeName) return;

      setIframeNames((prev) => prev.filter((iframeName) => iframeName !== name));
      delete iframeRefs.current[name];
    },
    [iframeNames]
  );

  const { iframeHeight, useMinHeight, canAddMore } = useMemo(() => {
    return computeLayoutState(iframeNames.length);
  }, [iframeNames.length]);

  return {
    data: {
      iframeNames,
      iframeRefs,
    },
    layout: {
      iframeHeight,
      useMinHeight,
      canAddMore,
    },
    actions: {
      addGuestIframe,
      removeIframe,
      setIframeRef,
    },
  };
};
