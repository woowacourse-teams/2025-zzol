import { useRacingGame } from '@/contexts/RacingGame/RacingGameContext';
import PrepareOverlay from '../../../components/PrepareOverlay/PrepareOverlay';
import Finish from '../Finish/Finish';

const RacingGameOverlays = () => {
  const { racingGameState } = useRacingGame();

  return (
    <>
      {racingGameState === 'PREPARE' && <PrepareOverlay />}
      {racingGameState === 'DONE' && <Finish />}
    </>
  );
};

export default RacingGameOverlays;
