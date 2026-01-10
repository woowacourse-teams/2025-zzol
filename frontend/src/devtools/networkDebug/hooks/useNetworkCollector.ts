import { useEffect, useState, useCallback, useRef } from 'react';
import { NetworkRequest, NetworkCollector } from '../types/network';
import { isTopWindow } from '@/devtools/common/utils/isTopWindow';

declare global {
  interface Window {
    __networkCollector__?: NetworkCollector;
  }
}

// 모든 collector 수집 (메인 + iframe들)
const getAllCollectors = (): NetworkCollector[] => {
  if (typeof window === 'undefined' || typeof document === 'undefined') return [];

  const collectors: NetworkCollector[] = [];

  // 메인 윈도우의 collector
  try {
    if (isTopWindow() && window.__networkCollector__) {
      collectors.push(window.__networkCollector__);
    }
  } catch {
    // cross-origin 등의 경우 무시
  }

  // 모든 iframe의 collector 수집 (document에서 직접 찾기)
  try {
    const iframes = document.querySelectorAll('iframe');
    iframes.forEach((iframe) => {
      try {
        // iframe의 contentWindow에 접근
        const iframeWindow = iframe.contentWindow;
        if (iframeWindow && iframeWindow.__networkCollector__) {
          collectors.push(iframeWindow.__networkCollector__);
        }
      } catch {
        // cross-origin 등의 경우 무시 (접근 불가)
      }
    });
  } catch {
    // iframe 접근 불가능한 경우 무시
  }

  // window.frames도 시도 (fallback)
  try {
    const frames = window.frames;
    if (frames && frames.length) {
      for (let i = 0; i < frames.length; i++) {
        try {
          const frame = frames[i] as Window;
          if (frame && frame.__networkCollector__) {
            // 이미 추가된 collector인지 확인 (중복 방지)
            if (!collectors.includes(frame.__networkCollector__)) {
              collectors.push(frame.__networkCollector__);
            }
          }
        } catch {
          // cross-origin 등의 경우 무시
        }
      }
    }
  } catch {
    // frames 접근 불가능한 경우 무시
  }

  return collectors;
};

export const useNetworkCollector = () => {
  const [requests, setRequests] = useState<NetworkRequest[]>([]);
  const collectorsRef = useRef<NetworkCollector[]>([]);
  const unsubscribeFunctionsRef = useRef<(() => void)[]>([]);
  const initialRequestIdsRef = useRef<Set<string>>(new Set());

  // 메인 윈도우에서만 동작
  const topWindow = isTopWindow();

  // collector 구독 설정
  const setupCollectors = useCallback((collectors: NetworkCollector[]) => {
    // 기존 구독 해제
    unsubscribeFunctionsRef.current.forEach((unsubscribe) => {
      try {
        unsubscribe();
      } catch {
        // 무시
      }
    });
    unsubscribeFunctionsRef.current = [];

    if (collectors.length === 0) return;

    // 모든 collector의 초기 요청 수집
    const allInitialRequests: NetworkRequest[] = [];
    collectors.forEach((collector) => {
      try {
        const collectorRequests = collector.getRequests();
        allInitialRequests.push(...collectorRequests);
      } catch {
        // 무시
      }
    });

    // 타임스탬프 순으로 정렬 (최신순)
    allInitialRequests.sort((a, b) => b.timestamp - a.timestamp);
    setRequests(allInitialRequests);

    // 초기 요청의 ID 추적
    initialRequestIdsRef.current = new Set(allInitialRequests.map((r) => r.id));

    // 모든 collector 구독
    collectors.forEach((collector) => {
      try {
        const unsubscribe = collector.subscribe((request) => {
          setRequests((prev) => {
            // 이미 있는 요청이면 무시
            if (prev.some((r) => r.id === request.id)) {
              return prev;
            }
            // 초기 요청 목록에 있던 건 무시 (이미 추가됨)
            if (initialRequestIdsRef.current.has(request.id)) {
              return prev;
            }
            // 새로운 요청을 앞에 추가
            return [request, ...prev];
          });
        });
        unsubscribeFunctionsRef.current.push(unsubscribe);
      } catch {
        // 무시
      }
    });

    collectorsRef.current = collectors;
  }, []);

  useEffect(() => {
    if (!topWindow) return;

    // 초기 collector 수집 및 구독
    const initialCollectors = getAllCollectors();
    setupCollectors(initialCollectors);

    // 주기적으로 iframe 확인 (동적으로 추가되는 iframe 대응)
    const intervalId = setInterval(() => {
      const currentCollectors = getAllCollectors();
      const existingCollectors = collectorsRef.current;

      // 새로운 collector가 있는지 확인 (객체 참조 비교)
      const hasNewCollector =
        currentCollectors.length !== existingCollectors.length ||
        currentCollectors.some((newCollector) => !existingCollectors.includes(newCollector));

      if (hasNewCollector) {
        setupCollectors(currentCollectors);
      }
    }, 1000); // 1초마다 확인

    return () => {
      unsubscribeFunctionsRef.current.forEach((unsubscribe) => {
        try {
          unsubscribe();
        } catch {
          // 무시
        }
      });
      clearInterval(intervalId);
    };
  }, [topWindow, setupCollectors]);

  const clearRequests = useCallback(() => {
    if (!topWindow) return;

    const allCollectors = getAllCollectors();
    allCollectors.forEach((collector) => {
      try {
        collector.clear();
      } catch {
        // 무시
      }
    });
    setRequests([]);
  }, [topWindow]);

  /**
   * 모든 collector에서 최신 요청 목록을 다시 가져와서 상태를 갱신합니다.
   */
  const refreshRequests = useCallback(() => {
    if (!topWindow) return;

    const allCollectors = getAllCollectors();
    const allRequests: NetworkRequest[] = [];

    allCollectors.forEach((collector) => {
      try {
        const collectorRequests = collector.getRequests();
        allRequests.push(...collectorRequests);
      } catch {
        // 무시
      }
    });

    // 타임스탬프 순으로 정렬 (최신순)
    allRequests.sort((a, b) => b.timestamp - a.timestamp);
    setRequests(allRequests);

    // 초기 요청 ID 업데이트
    initialRequestIdsRef.current = new Set(allRequests.map((r) => r.id));

    // collector 재설정 (새로운 collector가 있을 수 있음)
    setupCollectors(allCollectors);
  }, [topWindow, setupCollectors]);

  return {
    requests,
    clearRequests,
    refreshRequests,
    collectors: collectorsRef.current,
  };
};
