import Headline4 from '@/components/@common/Headline4/Headline4';
import { useCardGame } from '@/contexts/CardGame/CardGameContext';
import PrepareOverlay from '@/features/miniGame/components/PrepareOverlay/PrepareOverlay';
import Layout from '@/layouts/Layout';
import RoundTransition from '../components/RoundTransition/RoundTransition';
import GameCardGrid from '../components/GameCardGrid/GameCardGrid';
import PlayerCardDisplay from '../components/PlayerCardDisplay/PlayerCardDisplay';
import RoundHeader from '../components/RoundHeader/RoundHeader';
import { useCardGameActions } from '../hooks/useCardGameActions';
import { useCardGameTimer } from '../hooks/useCardGameTimer';

const CardGamePlayPage = () => {
  const { isTransition, currentRound, currentCardGameState, selectedCardInfo, cardInfos } =
    useCardGame();
  const { selectCard } = useCardGameActions();
  const { currentTime, isTimerActive, roundTotalTime } = useCardGameTimer();

  const showPrepareOverlay = currentCardGameState === 'PREPARE';
  const isCardClickDisabled =
    currentCardGameState === 'PREPARE' || currentCardGameState === 'SCORE_BOARD';

  const onCardClick = (cardIndex: number) => {
    if (isCardClickDisabled) return;
    selectCard(cardIndex);
  };

  if (isTransition) {
    return <RoundTransition currentRound={currentRound} />;
  }

  return (
    <Layout>
      <Layout.TopBar center={<Headline4>랜덤카드 게임</Headline4>} />
      <Layout.Content>
        <RoundHeader
          round={currentRound}
          currentTime={currentTime}
          roundTotalTime={roundTotalTime}
          isTimerActive={isTimerActive}
        />
        <PlayerCardDisplay selectedCardInfo={selectedCardInfo} />
        <GameCardGrid cardInfos={cardInfos} onCardClick={onCardClick} />
      </Layout.Content>
      {showPrepareOverlay && <PrepareOverlay />}
    </Layout>
  );
};

export default CardGamePlayPage;
