import useFetch from '@/apis/rest/useFetch';
import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { useWebSocketSubscription } from '@/apis/websocket/hooks/useWebSocketSubscription';
import Button from '@/components/@common/Button/Button';
import LocalErrorBoundary from '@/components/@common/ErrorBoundary/LocalErrorBoundary';
import ProbabilityList from '@/components/@composition/ProbabilityList/ProbabilityList';
import RouletteViewToggle from '@/components/@composition/RouletteViewToggle/RouletteViewToggle';
import SectionTitle from '@/components/@composition/SectionTitle/SectionTitle';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { usePlayerType } from '@/contexts/PlayerType/PlayerTypeContext';
import { useProbabilityHistory } from '@/contexts/ProbabilityHistory/ProbabilityHistoryContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import Layout from '@/layouts/Layout';
import { MiniGameType } from '@/types/miniGame/common';
import { RouletteView, RouletteWinnerResponse } from '@/types/roulette';
import { useCallback, useRef, useState } from 'react';
import RoulettePlaySection from '../../components/RoulettePlaySection/RoulettePlaySection';
import * as S from './RoulettePlayPage.styled';
import useRoulettePlay from './hooks/useRoulettePlay';
import useRouletteProbabilities from './hooks/useRouletteProbabilities';

const RoulettePlayPage = () => {
  const { joinCode, myName } = useIdentifier();
  const { playerType } = usePlayerType();
  const { send } = useWebSocket();
  const navigate = useReplaceNavigate();
  const [currentView, setCurrentView] = useState<RouletteView>('roulette');
  const { winner, randomAngle, isSpinStarted, handleSpinClick, startSpinWithResult } =
    useRoulettePlay();
  const { probabilityHistory } = useProbabilityHistory();
  const isFirstLoadRef = useRef(true);
  const { data: remainingMiniGames } = useFetch<{ remaining: MiniGameType[] }>({
    endpoint: `/rooms/${joinCode}/miniGames/remaining`,
    enabled: !!joinCode,
  });
  useRouletteProbabilities(isFirstLoadRef);

  const handleWinnerData = useCallback(
    (data: RouletteWinnerResponse) => {
      setCurrentView('roulette');
      startSpinWithResult(data);
    },
    [startSpinWithResult]
  );

  const handleGameStart = useCallback(
    (data: { miniGameType: MiniGameType }) => {
      const { miniGameType: nextMiniGame } = data;
      navigate(`/room/${joinCode}/${nextMiniGame}/ready`);
    },
    [joinCode, navigate]
  );

  useWebSocketSubscription<RouletteWinnerResponse>(`/room/${joinCode}/winner`, handleWinnerData);
  useWebSocketSubscription(`/room/${joinCode}/round`, handleGameStart);

  const handleViewChange = () => {
    setCurrentView((prev) => (prev === 'statistics' ? 'roulette' : 'statistics'));
  };

  const hasNextMiniGame = remainingMiniGames && remainingMiniGames.remaining.length > 0;

  const handleUnifiedButtonClick = () => {
    if (isSpinStarted) return;

    if (hasNextMiniGame) {
      send(`/room/${joinCode}/minigame/command`, {
        commandType: 'START_MINI_GAME',
        commandRequest: {
          hostName: myName,
        },
      });
    } else {
      handleSpinClick();
    }
  };

  const getButtonText = () => {
    if (isSpinStarted) return '룰렛 돌리는 중';
    if (hasNextMiniGame) return '다음 미니게임 하러가기';
    return '룰렛 돌리기';
  };

  const VIEW_COMPONENTS = {
    roulette: (
      <RoulettePlaySection
        isSpinStarted={isSpinStarted}
        winner={winner}
        randomAngle={randomAngle}
        isFirstLoadRef={isFirstLoadRef}
      />
    ),
    statistics: <ProbabilityList playerProbabilities={probabilityHistory.current} />,
  };

  //TODO: 다른 에러 처리방식을 찾아보기
  if (!playerType) return null;

  return (
    <Layout>
      <Layout.TopBar />
      <Layout.Content>
        <S.Container>
          <SectionTitle title="룰렛 현황" description="미니게임 결과에 따라 확률이 조정됩니다" />
          <LocalErrorBoundary>{VIEW_COMPONENTS[currentView]}</LocalErrorBoundary>
          <S.IconButtonWrapper>
            <RouletteViewToggle currentView={currentView} onViewChange={handleViewChange} />
          </S.IconButtonWrapper>
        </S.Container>
      </Layout.Content>
      <Layout.ButtonBar>
        {playerType === 'HOST' ? (
          <Button
            variant={isSpinStarted ? 'disabled' : 'primary'}
            onClick={handleUnifiedButtonClick}
            data-testid="roulette-spin-button"
          >
            {getButtonText()}
          </Button>
        ) : (
          <Button variant={isSpinStarted ? 'disabled' : 'loading'} loadingText="대기 중">
            룰렛 돌리는 중
          </Button>
        )}
      </Layout.ButtonBar>
    </Layout>
  );
};

export default RoulettePlayPage;
