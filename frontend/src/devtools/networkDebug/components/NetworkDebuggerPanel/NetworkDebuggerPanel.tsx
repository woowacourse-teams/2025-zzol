import { useState, useMemo, useRef } from 'react';
import { useNetworkCollector } from '../../hooks/useNetworkCollector';
import { usePanelResize } from '@/devtools/common/hooks/usePanelResize';
import { useVerticalResize } from '@/devtools/common/hooks/useVerticalResize';
import { useMobileSplitResize } from '@/devtools/common/hooks/useMobileSplitResize';
import { checkIsTouchDevice } from '@/utils/checkIsTouchDevice';
import { isTopWindow } from '@/devtools/common/utils/isTopWindow';
import NetworkFilterBar from './NetworkFilterBar/NetworkFilterBar';
import NetworkRequestList from './NetworkRequestList/NetworkRequestList';
import NetworkRequestDetail from './NetworkRequestDetail/NetworkRequestDetail';
import * as S from './NetworkDebuggerPanel.styled';

/**
 * 네트워크 디버거 패널 컴포넌트입니다.
 * Fetch 및 WebSocket 요청을 모니터링하고 디버깅할 수 있는 도구를 제공합니다.
 */
const NetworkDebuggerPanel = () => {
  const topWindow = isTopWindow();

  const isMobile = useMemo(() => checkIsTouchDevice(), []);

  const [open, setOpen] = useState(false);
  const [selectedContext, setSelectedContext] = useState<string | null>(null);
  const [selectedType, setSelectedType] = useState<'fetch' | 'websocket' | null>(null);
  const [selectedRequest, setSelectedRequest] = useState<string | null>(null);

  const initialPanelHeight = useMemo(() => {
    if (isMobile && typeof window !== 'undefined') {
      return window.innerHeight * 0.8;
    }
    return 400;
  }, [isMobile]);

  const { requests, clearRequests, refreshRequests } = useNetworkCollector();
  const { panelHeight, handleResizeStart } = usePanelResize(initialPanelHeight);
  const { detailWidthPercent, handleVerticalResizeStart, contentRef } = useVerticalResize();
  const {
    topHeightPercent,
    handleResizeStart: handleMobileSplitResizeStart,
    contentRef: mobileContentRef,
  } = useMobileSplitResize(20);

  /**
   * 고유한 context 목록을 추출합니다.
   * 순서: MAIN → HOST → GUEST1, GUEST2... (숫자 순서로 정렬)
   */
  const availableContexts = useMemo(() => {
    const contexts = new Set<string>();
    requests.forEach((req) => {
      if (req.context) contexts.add(req.context);
    });

    const contextArray = Array.from(contexts);
    const contextUpper = contextArray.map((ctx) => ctx.toUpperCase());

    // MAIN, HOST, GUEST 계열로 분류
    const mainContexts: string[] = [];
    const hostContexts: string[] = [];
    const guestContexts: { original: string; number: number }[] = [];
    const otherContexts: string[] = [];

    contextArray.forEach((ctx, index) => {
      const upper = contextUpper[index];
      if (upper === 'MAIN') {
        mainContexts.push(ctx);
      } else if (upper === 'HOST') {
        hostContexts.push(ctx);
      } else if (upper.startsWith('GUEST')) {
        const match = upper.match(/^GUEST(\d+)$/);
        if (match) {
          const num = parseInt(match[1], 10);
          guestContexts.push({ original: ctx, number: num });
        } else {
          guestContexts.push({ original: ctx, number: Infinity });
        }
      } else {
        otherContexts.push(ctx);
      }
    });

    // GUEST 계열을 숫자 순서로 정렬
    guestContexts.sort((a, b) => a.number - b.number);

    // 순서: MAIN → HOST → GUEST1, GUEST2... → 기타
    return [
      ...mainContexts,
      ...hostContexts,
      ...guestContexts.map((g) => g.original),
      ...otherContexts.sort(),
    ];
  }, [requests]);

  /**
   * 필터링된 요청 목록을 반환합니다.
   */
  const filteredRequests = useMemo(() => {
    return requests.filter((req) => {
      if (selectedContext && req.context !== selectedContext) return false;
      if (selectedType && req.type !== selectedType) return false;
      return true;
    });
  }, [requests, selectedContext, selectedType]);

  const selectedRequestData = useMemo(() => {
    if (!selectedRequest) return null;
    return requests.find((req) => req.id === selectedRequest) || null;
  }, [requests, selectedRequest]);

  const longPressTimerRef = useRef<number | null>(null);
  const LONG_PRESS_DURATION = 2000;

  /**
   * 길게 누르기 시작 핸들러입니다.
   */
  const handleLongPressStart = () => {
    longPressTimerRef.current = window.setTimeout(() => {
      setOpen(true);
      longPressTimerRef.current = null;
    }, LONG_PRESS_DURATION);
  };

  /**
   * 길게 누르기 종료 핸들러입니다.
   */
  const handleLongPressEnd = () => {
    if (longPressTimerRef.current) {
      window.clearTimeout(longPressTimerRef.current);
      longPressTimerRef.current = null;
    }
  };

  if (!topWindow) return null;

  if (!open) {
    return (
      <>
        {!isMobile && (
          <S.ToggleButton type="button" onClick={() => setOpen(true)}>
            Network
          </S.ToggleButton>
        )}
        {isMobile && (
          <S.HiddenTrigger
            onPointerDown={handleLongPressStart}
            onPointerUp={handleLongPressEnd}
            onPointerCancel={handleLongPressEnd}
          />
        )}
      </>
    );
  }

  return (
    <S.Panel height={panelHeight}>
      <S.ResizeHandle onPointerDown={handleResizeStart} />
      <S.Header>
        <S.Title>Network</S.Title>
        <S.HeaderActions>
          <S.HeaderButtonWrapper>
            <S.ClearButton type="button" onClick={clearRequests}>
              🚫
            </S.ClearButton>
            <S.ClearButton type="button" onClick={refreshRequests}>
              갱신
            </S.ClearButton>
          </S.HeaderButtonWrapper>
          <S.CloseButton type="button" onClick={() => setOpen(false)}>
            ✕
          </S.CloseButton>
        </S.HeaderActions>
      </S.Header>

      <NetworkFilterBar
        contexts={availableContexts}
        selectedContext={selectedContext}
        selectedType={selectedType}
        onContextChange={setSelectedContext}
        onTypeChange={setSelectedType}
      />

      <S.Content ref={isMobile ? mobileContentRef : contentRef} $isMobile={isMobile}>
        <S.RequestListSection
          detailWidthPercent={selectedRequestData && !isMobile ? detailWidthPercent : 0}
          $isMobile={isMobile}
          $hasDetail={!!selectedRequestData}
          $topHeightPercent={isMobile && selectedRequestData ? topHeightPercent : 0}
        >
          <NetworkRequestList
            requests={filteredRequests}
            selectedRequestId={selectedRequest}
            onSelectRequest={setSelectedRequest}
          />
          {selectedRequestData && !isMobile && (
            <S.VerticalResizeHandle onPointerDown={handleVerticalResizeStart} />
          )}
        </S.RequestListSection>
        {selectedRequestData && isMobile && (
          <S.MobileResizeHandle onPointerDown={handleMobileSplitResizeStart} />
        )}
        {selectedRequestData && (
          <S.DetailSection
            widthPercent={isMobile ? 100 : detailWidthPercent}
            $isMobile={isMobile}
            $topHeightPercent={isMobile ? topHeightPercent : 0}
          >
            <NetworkRequestDetail
              request={selectedRequestData}
              onClose={() => setSelectedRequest(null)}
            />
          </S.DetailSection>
        )}
      </S.Content>
    </S.Panel>
  );
};

export default NetworkDebuggerPanel;
