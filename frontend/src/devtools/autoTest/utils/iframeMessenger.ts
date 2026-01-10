import type { RefObject } from 'react';
import type { TestMessage } from '@/devtools/autoTest/types/testMessage';

export type IframeRefMap = Record<string, HTMLIFrameElement | null>;

type CreateIframeMessengerParams = {
  iframeRefs: RefObject<IframeRefMap>;
};

export const createIframeMessenger = ({ iframeRefs }: CreateIframeMessengerParams) => {
  const postMessage = (iframeName: string, message: TestMessage) => {
    const iframe = iframeRefs.current[iframeName];
    if (iframe?.contentWindow) {
      iframe.contentWindow.postMessage(message, '*');
    }
  };

  const sendToIframe = (iframeName: string, message: TestMessage) => {
    postMessage(iframeName, message);
  };

  const broadcastToIframes = (
    iframeNames: string[],
    buildMessage: (iframeName: string) => TestMessage
  ) => {
    iframeNames.forEach((iframeName) => {
      const message = buildMessage(iframeName);
      sendToIframe(iframeName, message);
    });
  };

  return {
    sendToIframe,
    broadcastToIframes,
  };
};
