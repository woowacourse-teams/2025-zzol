import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import IframePreviewToggle from '@/devtools/autoTest/components/IframePreviewToggle/IframePreviewToggle';
import { setupAutoTestListener } from '@/devtools/autoTest/flow/setupAutoTestListener';
import { initializeAutoTestLogger } from '@/devtools/autoTest/utils/autoTestLogger';
import NetworkDebuggerPanel from '@/devtools/networkDebug/components/NetworkDebuggerPanel/NetworkDebuggerPanel';
import { isTopWindow } from '@/devtools/common/utils/isTopWindow';

export const DevToolsWrapper = () => {
  const location = useLocation();

  // AutoTestLogger 초기화
  useEffect(() => {
    if (process.env.ENABLE_DEVTOOLS) {
      initializeAutoTestLogger();
    }
  }, []);

  // Auto test listener 설정
  useEffect(() => {
    if (!isTopWindow()) {
      const cleanup = setupAutoTestListener();

      const sendReady = () => {
        const iframeName = window.frameElement?.getAttribute('name') || '';
        if (iframeName === 'host' && window.parent && window.parent !== window) {
          window.parent.postMessage({ type: 'IFRAME_READY', iframeName }, '*');
        }
      };

      setTimeout(sendReady, 100);
      return cleanup;
    }
  }, []);

  // Path change 감지 및 메시지 전송
  useEffect(() => {
    if (!isTopWindow() && window.parent && window.parent !== window) {
      const iframeName = window.frameElement?.getAttribute('name') || '';
      if (iframeName) {
        window.parent.postMessage(
          {
            type: 'PATH_CHANGE',
            iframeName,
            path: location.pathname,
          },
          '*'
        );

        const orderPagePattern = /^\/room\/[^/]+\/order$/;
        if (orderPagePattern.test(location.pathname)) {
          window.parent.postMessage({ type: 'TEST_COMPLETED' }, '*');
        }
      }
    }
  }, [location.pathname]);

  return (
    <>
      <IframePreviewToggle />
      <NetworkDebuggerPanel />
    </>
  );
};
