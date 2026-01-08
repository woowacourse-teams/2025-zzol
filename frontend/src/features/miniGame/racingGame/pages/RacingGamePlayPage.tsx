import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useRacingGameState } from '@/contexts/RacingGame/RacingGameContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import RacingGameOverlay from '../components/RacingGameOverlay/RacingGameOverlay';
import RacingGameOverlays from '../components/RacingGameOverlays/RacingGameOverlays';
import RacingProgressBar from '../components/RacingProgressBar/RacingProgressBar';
import RacingRanks from '../components/RacingRanks/RacingRanks';
import RacingPlayersArea from '../components/RacingPlayersArea/RacingPlayersArea';
import * as S from './RacingGamePlayPage.styled';

const RacingGamePage = () => {
  const { joinCode, myName } = useIdentifier();
  const { send } = useWebSocket();
  const navigate = useReplaceNavigate();
  const { miniGameType } = useParams();
  const racingGameState = useRacingGameState();

  useEffect(() => {
    setTimeout(() => {
      send(`/room/${joinCode}/racing-game/start`, {
        hostName: myName,
      });
    }, 2000);
  }, [joinCode, send, myName]);

  useEffect(() => {
    if (racingGameState === 'DONE') {
      navigate(`/room/${joinCode}/${miniGameType}/result`);
    }
  }, [racingGameState, joinCode, navigate, miniGameType]);

  return (
    <>
      <RacingGameOverlays />
      <RacingGameOverlay isGoal={false}>
        <S.Container>
          <RacingRanks />
          <RacingProgressBar />
          <RacingPlayersArea />
        </S.Container>
      </RacingGameOverlay>
    </>
  );
};

export default RacingGamePage;
