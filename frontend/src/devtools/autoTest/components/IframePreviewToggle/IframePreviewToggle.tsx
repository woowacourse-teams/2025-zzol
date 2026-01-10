import { useEffect, useMemo, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { checkIsTouchDevice } from '../../../../utils/checkIsTouchDevice';
import { isTopWindow } from '@/devtools/common/utils/isTopWindow';
import { useIframeRegistry } from '@/devtools/autoTest/hooks/useIframeRegistry';
import { useGameSequenceSelector } from '@/devtools/autoTest/hooks/useGameSequenceSelector';
import { useAutoTestBridge } from '@/devtools/autoTest/hooks/useAutoTestBridge';
import IframeControls from '@/devtools/autoTest/components/IframeControls/IframeControls';
import IframePreviewList from '@/devtools/autoTest/components/IframePreviewList/IframePreviewList';
import * as S from './IframePreviewToggle.styled';

const IframePreviewToggle = () => {
  const location = useLocation();
  const [open, setOpen] = useState<boolean>(false);

  const topWindow = isTopWindow();
  const isTouchDevice = useMemo(() => checkIsTouchDevice(), []);
  const isRootPath = location.pathname === '/';

  const {
    data: registryData,
    layout: registryLayout,
    actions: { addGuestIframe, removeIframe, setIframeRef },
  } = useIframeRegistry();
  const { iframeHeight, useMinHeight, canAddMore } = registryLayout;

  const { state: gameSelectionState, actions: gameSelectionActions } = useGameSequenceSelector();
  const { gameSequence } = gameSelectionState;
  const { toggleExpanded, setExpanded, toggleGame } = gameSelectionActions;

  const {
    runState,
    iframePaths,
    controls: autoTestControls,
  } = useAutoTestBridge({
    isOpen: open,
    iframeNames: registryData.iframeNames,
    gameSequence,
    iframeRefs: registryData.iframeRefs,
  });
  const { isRunning, isPaused } = runState;
  const {
    start: handleStartTest,
    stop: handleStopTest,
    pause: handlePauseTest,
    resume: handleResumeTest,
  } = autoTestControls;

  useEffect(() => {
    // 경로가 바뀌면 닫아준다 (예상치 못한 잔상 방지)
    setOpen(false);
    setExpanded(false);
  }, [location.pathname, setExpanded]);

  if (!topWindow || !isRootPath || isTouchDevice) return null;

  return (
    <S.Container>
      <IframeControls
        panel={{ open, onToggleOpen: () => setOpen((prev) => !prev) }}
        gameSelection={{
          ...gameSelectionState,
          onToggleExpanded: toggleExpanded,
          onToggleGame: toggleGame,
        }}
        test={{
          onStart: handleStartTest,
          onPause: handlePauseTest,
          onResume: handleResumeTest,
          onStop: handleStopTest,
          isRunning,
          isPaused,
        }}
      />
      {open && (
        <IframePreviewList
          data={{ iframeNames: registryData.iframeNames, iframePaths: iframePaths }}
          layout={{ iframeHeight, useMinHeight, canAddMore }}
          actions={{
            onAddIframe: addGuestIframe,
            onRemoveIframe: removeIframe,
            onRegisterIframeRef: setIframeRef,
          }}
        />
      )}
    </S.Container>
  );
};

export default IframePreviewToggle;
